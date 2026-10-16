package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.data.model.UserSettingsModel
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.FireBackgroundLight
import com.example.ui.theme.FireDanger
import com.example.ui.theme.FirePurpleLight
import com.example.ui.theme.FirePurplePrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentProfile: UserProfile?,
    settingsRepository: SettingsRepository,
    userRepository: UserRepository,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onOpenSecurityCenter: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.observeUserSettings().collectAsState(
        initial = UserSettingsModel(userId = currentProfile?.userId ?: "")
    )

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = FireBackgroundLight,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF20202B)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20202B)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card Header
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditProfile() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(FirePurpleLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = FirePurplePrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentProfile?.displayName?.ifBlank { "User" } ?: "User",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF20202B)
                            )
                            Text(
                                text = "@${currentProfile?.username ?: "username"}",
                                fontSize = 13.sp,
                                color = Color(0xFF888896)
                            )
                        }

                        Button(
                            onClick = onEditProfile,
                            colors = ButtonDefaults.buttonColors(containerColor = FirePurpleLight, contentColor = FirePurplePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Edit", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Security & App Lock Section
            item {
                SectionHeader("SECURITY & APP LOCK")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = FirePurplePrimary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("App Lock PIN", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        text = if (settings.appLockEnabled) "App locked with PIN" else "Disabled",
                                        fontSize = 11.sp,
                                        color = Color(0xFF888896)
                                    )
                                }
                            }
                            Switch(
                                checked = settings.appLockEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        showPinSetupDialog = true
                                    } else {
                                        scope.launch {
                                            settingsRepository.saveSettings(
                                                settings.copy(appLockEnabled = false, appLockPinHash = null)
                                            )
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = FirePurplePrimary, checkedTrackColor = FirePurpleLight)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F6))

                        SettingActionRow(
                            icon = Icons.Default.Security,
                            title = "Open Security Center",
                            subtitle = "Run malware scans, APK audits and anti-spy checks",
                            onClick = onOpenSecurityCenter
                        )
                    }
                }
            }

            // Privacy Section
            item {
                SectionHeader("PRIVACY")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleRow(
                            title = "Online Status",
                            subtitle = "Show when you are active on Fireonline Chati",
                            isChecked = settings.onlineVisible,
                            onCheckedChange = {
                                scope.launch {
                                    settingsRepository.saveSettings(settings.copy(onlineVisible = it))
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF0F0F6))

                        SettingToggleRow(
                            title = "Read Receipts",
                            subtitle = "Send and receive seen status for messages",
                            isChecked = settings.readReceipts,
                            onCheckedChange = {
                                scope.launch {
                                    settingsRepository.saveSettings(settings.copy(readReceipts = it))
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF0F0F6))

                        SettingToggleRow(
                            title = "Last Seen Visibility",
                            subtitle = "Allow contacts to see when you were last online",
                            isChecked = settings.lastSeenVisible,
                            onCheckedChange = {
                                scope.launch {
                                    settingsRepository.saveSettings(settings.copy(lastSeenVisible = it))
                                }
                            }
                        )
                    }
                }
            }

            // Notifications Section
            item {
                SectionHeader("NOTIFICATIONS")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingToggleRow(
                            title = "Message Notifications",
                            subtitle = "Alert on incoming messages and files",
                            isChecked = settings.notificationsEnabled,
                            onCheckedChange = {
                                scope.launch {
                                    settingsRepository.saveSettings(settings.copy(notificationsEnabled = it))
                                }
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF0F0F6))

                        SettingToggleRow(
                            title = "Message Sound & Vibration",
                            subtitle = "Play sound when sending and receiving",
                            isChecked = settings.messageSound,
                            onCheckedChange = {
                                scope.launch {
                                    settingsRepository.saveSettings(settings.copy(messageSound = it))
                                }
                            }
                        )
                    }
                }
            }

            // Storage & Data Section
            item {
                SectionHeader("STORAGE & DATA")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingActionRow(
                            icon = Icons.Default.Storage,
                            title = "Clear Cached Media",
                            subtitle = "Frees space from temporary previews and file caches",
                            onClick = {
                                context.cacheDir.deleteRecursively()
                                Toast.makeText(context, "Local media cache cleared", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // CEO Community & Links Section (MANDATORY)
            item {
                SectionHeader("OFFICIAL COMMUNITIES")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingActionRow(
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            title = "CEO Community — Telegram",
                            subtitle = "Join our official announcements & developer channel",
                            onClick = {
                                val url = "https://t.me/+_aoEtf998dM1MjZl"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F6))

                        SettingActionRow(
                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                            title = "TikTok Community",
                            subtitle = "Follow @firechat_app for updates and features",
                            onClick = {
                                val url = "https://www.tiktok.com/@firechat_app"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            // Account Actions (Sign Out & Delete)
            item {
                SectionHeader("ACCOUNT")
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingActionRow(
                            icon = Icons.AutoMirrored.Filled.ExitToApp,
                            title = "Sign Out",
                            subtitle = "Log out from this session securely",
                            onClick = onSignOut
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F6))

                        SettingActionRow(
                            icon = Icons.Default.DeleteForever,
                            title = "Request Account Deletion",
                            subtitle = "Permanently removes your account profile and data",
                            titleColor = FireDanger,
                            onClick = { showDeleteAccountDialog = true }
                        )
                    }
                }
            }

            // App About
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Fireonline Chati v1.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF888896)
                    )
                    Text(
                        text = "Real-time • Cloud Firestore Backend • Zero-Trust Security",
                        fontSize = 11.sp,
                        color = Color(0xFFA0A0B0)
                    )
                }
            }
        }
    }

    // App Lock PIN Setup Dialog
    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinSetupDialog = false
                newPinInput = ""
                confirmPinInput = ""
                pinError = null
            },
            title = { Text("Set App Lock PIN", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter a 4-to-6 digit numeric PIN to lock Fireonline Chati.", fontSize = 13.sp, color = Color(0xFF888896))
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                        label = { Text("New PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPinInput = it },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = pinError!!, color = Color.Red, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length < 4) {
                            pinError = "PIN must be at least 4 digits"
                            return@Button
                        }
                        if (newPinInput != confirmPinInput) {
                            pinError = "PINs do not match"
                            return@Button
                        }
                        val hash = settingsRepository.hashPin(newPinInput)
                        scope.launch {
                            settingsRepository.saveSettings(
                                settings.copy(appLockEnabled = true, appLockPinHash = hash)
                            )
                        }
                        showPinSetupDialog = false
                        newPinInput = ""
                        confirmPinInput = ""
                        Toast.makeText(context, "App Lock PIN enabled", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FirePurplePrimary)
                ) {
                    Text("Enable Lock")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showPinSetupDialog = false
                    newPinInput = ""
                    confirmPinInput = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Account Deletion Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account?", fontWeight = FontWeight.Bold, color = FireDanger) },
            text = {
                Text(
                    text = "Are you sure you want to delete your Fireonline Chati account? This will sign you out and request permanent removal of your user profile.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FireDanger)
                ) {
                    Text("Yes, Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF888896),
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF20202B))
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF888896))
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = FirePurplePrimary, checkedTrackColor = FirePurpleLight)
        )
    }
}

@Composable
private fun SettingActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = Color(0xFF20202B),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(FirePurpleLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor == FireDanger) FireDanger else FirePurplePrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = titleColor)
            Text(subtitle, fontSize = 11.sp, color = Color(0xFF888896))
        }
    }
}
