package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.example.ui.theme.FirePurpleLight
import com.example.ui.theme.FirePurplePrimary
import kotlinx.coroutines.launch

val PRESET_AVATARS = listOf(
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
    "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200"
)

@Composable
fun EditProfileDialog(
    currentProfile: UserProfile?,
    userRepository: UserRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var displayName by remember { mutableStateOf(currentProfile?.displayName ?: "") }
    var username by remember { mutableStateOf(currentProfile?.username ?: "") }
    var bio by remember { mutableStateOf(currentProfile?.bio ?: "") }
    var statusMessage by remember { mutableStateOf(currentProfile?.statusMessage ?: "Hey there! I am using Fireonline Chati") }
    var selectedPhotoUrl by remember { mutableStateOf(currentProfile?.photoUrl ?: PRESET_AVATARS.first()) }

    var isSaving by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentProfile == null || currentProfile.username.isEmpty()) "Complete Your Profile" else "Edit Profile",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color(0xFF20202B)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar Preview
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(FirePurpleLight)
                        .border(2.dp, FirePurplePrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedPhotoUrl.isNotBlank()) {
                        AsyncImage(
                            model = selectedPhotoUrl,
                            contentDescription = "Avatar Preview",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = FirePurplePrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Choose Avatar", fontSize = 12.sp, color = Color(0xFF888896))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    items(PRESET_AVATARS) { url ->
                        val isSelected = url == selectedPhotoUrl
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) FirePurplePrimary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { selectedPhotoUrl = url }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("display_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' } },
                    label = { Text("Unique Username (@)") },
                    singleLine = true,
                    prefix = { Text("@") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = statusMessage,
                    onValueChange = { statusMessage = it },
                    label = { Text("Status Message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = validationError!!,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (displayName.trim().isEmpty()) {
                        validationError = "Display name cannot be empty"
                        return@Button
                    }
                    if (username.trim().length < 3) {
                        validationError = "Username must be at least 3 characters"
                        return@Button
                    }
                    isSaving = true
                    validationError = null
                    scope.launch {
                        val result = userRepository.saveProfile(
                            displayName = displayName,
                            username = username,
                            bio = bio.ifBlank { null },
                            statusMessage = statusMessage.ifBlank { null },
                            photoUrl = selectedPhotoUrl.ifBlank { null }
                        )
                        isSaving = false
                        if (result.isSuccess) {
                            onSaved()
                        } else {
                            validationError = result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile"
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = FirePurplePrimary),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Save Profile")
                }
            }
        },
        dismissButton = {
            if (currentProfile != null && currentProfile.username.isNotEmpty()) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
