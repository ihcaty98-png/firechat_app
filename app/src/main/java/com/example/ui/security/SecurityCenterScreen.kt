package com.example.ui.security

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SecurityEventItem
import com.example.data.repository.ApkAnalysisResult
import com.example.data.repository.SecurityFinding
import com.example.data.repository.SecurityRepository
import com.example.data.repository.SecurityScanResult
import com.example.ui.theme.FireBackgroundLight
import com.example.ui.theme.FireDanger
import com.example.ui.theme.FirePurpleLight
import com.example.ui.theme.FirePurplePrimary
import com.example.ui.theme.FirePurpleSecondary
import com.example.ui.theme.FireSuccess
import com.example.ui.theme.FireWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityCenterScreen(
    securityRepository: SecurityRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<SecurityScanResult?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: APK & Tools, 2: History

    // URL Inspector state
    var testUrlInput by remember { mutableStateOf("") }
    var urlInspectionResult by remember { mutableStateOf<Pair<String, String>?>(null) }

    // APK Inspector state
    var customApkResult by remember { mutableStateOf<ApkAnalysisResult?>(null) }
    var isAnalyzingApk by remember { mutableStateOf(false) }

    val securityEvents by securityRepository.observeSecurityEvents().collectAsState(initial = emptyList())

    // Run initial scan on open
    LaunchedEffect(Unit) {
        isScanning = true
        scanResult = securityRepository.runComprehensiveScan()
        isScanning = false
    }

    val apkFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isAnalyzingApk = true
            scope.launch {
                val name = uri.lastPathSegment ?: "InspectionPackage.apk"
                val result = securityRepository.analyzeApkFile(uri, name, 0L)
                customApkResult = result
                isAnalyzingApk = false
            }
        }
    }

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
                    text = "Security Center",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20202B)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = FirePurplePrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Audit & Score", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Tools & APKs", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Scan History", fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Overview & Findings
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // Dashboard Score Card
                            ScoreDashboardCard(
                                scanResult = scanResult,
                                isScanning = isScanning,
                                onRescan = {
                                    scope.launch {
                                        isScanning = true
                                        scanResult = securityRepository.runComprehensiveScan()
                                        isScanning = false
                                    }
                                }
                            )
                        }

                        item {
                            Text(
                                text = "Findings & Recommendations (${scanResult?.findings?.size ?: 0})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF20202B)
                            )
                        }

                        if (scanResult != null && scanResult!!.findings.isNotEmpty()) {
                            items(scanResult!!.findings) { finding ->
                                FindingItemCard(
                                    finding = finding,
                                    onActionClick = {
                                        when (finding.actionType) {
                                            "DEVELOPER_SETTINGS" -> {
                                                try {
                                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                                                } catch (_: Exception) {
                                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                                }
                                            }
                                            "ACCESSIBILITY_SETTINGS" -> {
                                                try {
                                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                                } catch (_: Exception) {
                                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                                }
                                            }
                                            "OVERLAY_SETTINGS" -> {
                                                try {
                                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")))
                                                } catch (_: Exception) {
                                                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                                }
                                            }
                                            "APP_PERMISSIONS" -> {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", context.packageName, null)
                                                }
                                                context.startActivity(intent)
                                            }
                                            else -> {
                                                Toast.makeText(context, finding.description, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                )
                            }
                        } else if (!isScanning) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = FireSuccess,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "All System Checks Passed",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "No critical threats, suspicious overlays or dangerous settings found.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF888896),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Security Tools & APK inspection
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            // On-Demand APK Malware Scanner
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(FirePurpleLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Android,
                                                contentDescription = null,
                                                tint = FirePurplePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "APK & File Safety Inspector",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "Inspect any APK package for malicious signatures & excessive permissions",
                                                fontSize = 12.sp,
                                                color = Color(0xFF888896)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { apkFilePicker.launch("application/vnd.android.package-archive") },
                                        enabled = !isAnalyzingApk,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FirePurplePrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isAnalyzingApk) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                        } else {
                                            Text("Select APK File to Analyze")
                                        }
                                    }

                                    if (customApkResult != null) {
                                        val apk = customApkResult!!
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(FirePurpleLight)
                                                .padding(12.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Result: ${apk.riskScore.uppercase()}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (apk.riskScore == "danger") FireDanger else FireSuccess
                                                )
                                                Text(
                                                    text = "SHA-256: ${apk.sha256Hash}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF20202B)
                                                )
                                                apk.reasons.forEach {
                                                    Text(text = "• $it", fontSize = 12.sp, color = Color(0xFF20202B))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            // Phishing Link Inspector
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(FirePurpleLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Link,
                                                contentDescription = null,
                                                tint = FirePurplePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Phishing & URL Guard",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = "Test web links for deceitful domains, raw IP redirection, or scams",
                                                fontSize = 12.sp,
                                                color = Color(0xFF888896)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = testUrlInput,
                                        onValueChange = { testUrlInput = it },
                                        placeholder = { Text("Paste link e.g. https://...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            urlInspectionResult = securityRepository.checkUrlSafety(testUrlInput)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = FirePurplePrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Check URL Safety")
                                    }

                                    if (urlInspectionResult != null) {
                                        val (status, explanation) = urlInspectionResult!!
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Status: ${status.uppercase()} — $explanation",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (status) {
                                                "danger" -> FireDanger
                                                "warning" -> FireWarning
                                                else -> FireSuccess
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Anti-Theft Guidance Card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = FirePurplePrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Android Anti-Theft Protection", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ensure Google Find My Device is enabled in your Android System Settings to remotely lock or erase your device in the event of theft.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF888896)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                            } catch (_: Exception) {
                                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Open Device Security Settings")
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // History
                    if (securityEvents.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No security events recorded yet.",
                                color = Color(0xFF888896),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(securityEvents) { event ->
                                SecurityEventCard(event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreDashboardCard(
    scanResult: SecurityScanResult?,
    isScanning: Boolean,
    onRescan: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FirePurplePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Device Protection Status",
                        fontSize = 14.sp,
                        color = Color(0xFFDCD8FC)
                    )
                    Text(
                        text = if (isScanning) "Scanning..." else "${scanResult?.score ?: 95}/100",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onRescan() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Counters Row (Critical / High / Med / Low)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusPill("Critical", scanResult?.criticalCount ?: 0, FireDanger)
                StatusPill("High", scanResult?.highCount ?: 0, FireWarning)
                StatusPill("Medium", scanResult?.mediumCount ?: 0, Color(0xFFFBBF24))
                StatusPill("Low", scanResult?.lowCount ?: 0, Color.White)
            }
        }
    }
}

@Composable
private fun StatusPill(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFFDCD8FC)
        )
    }
}

@Composable
fun FindingItemCard(
    finding: SecurityFinding,
    onActionClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        when (finding.severity) {
                            "critical" -> FireDanger.copy(alpha = 0.15f)
                            "high" -> FireWarning.copy(alpha = 0.15f)
                            else -> FirePurpleLight
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (finding.severity) {
                        "critical", "high" -> Icons.Default.Warning
                        else -> Icons.Default.Security
                    },
                    contentDescription = null,
                    tint = when (finding.severity) {
                        "critical" -> FireDanger
                        "high" -> FireWarning
                        else -> FirePurplePrimary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = finding.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF20202B)
                )
                Text(
                    text = finding.description,
                    fontSize = 11.sp,
                    color = Color(0xFF888896),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = FirePurplePrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(finding.actionLabel, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SecurityEventCard(event: SecurityEventItem) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FirePurpleLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = FirePurplePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF20202B)
                )
                Text(
                    text = event.description,
                    fontSize = 11.sp,
                    color = Color(0xFF888896)
                )
            }
            Text(
                text = formatEventTime(event.createdAt),
                fontSize = 10.sp,
                color = Color.LightGray
            )
        }
    }
}

private fun formatEventTime(ts: com.google.firebase.Timestamp?): String {
    if (ts == null) return ""
    return SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(ts.toDate())
}
