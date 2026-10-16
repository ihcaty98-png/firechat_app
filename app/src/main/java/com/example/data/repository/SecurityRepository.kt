package com.example.data.repository

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.SecurityEventItem
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID

data class SecurityScanResult(
    val score: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val findings: List<SecurityFinding>,
    val scanTimestamp: Long = System.currentTimeMillis()
)

data class SecurityFinding(
    val id: String = UUID.randomUUID().toString(),
    val category: String, // "MALWARE", "PERMISSION", "ANTI_SPY", "NETWORK", "USB"
    val title: String,
    val description: String,
    val severity: String, // "critical", "high", "medium", "low", "info"
    val isResolved: Boolean = false,
    val actionLabel: String = "Review",
    val actionType: String = "SETTINGS" // "SETTINGS", "DISMISS", "INFO"
)

data class ApkAnalysisResult(
    val fileName: String,
    val fileSize: Long,
    val sha256Hash: String,
    val riskScore: String, // "safe", "warning", "caution", "danger"
    val reasons: List<String>,
    val permissionsRequested: List<String>
)

class SecurityRepository(
    private val context: Context,
    private val db: FirebaseFirestore
) {

    constructor(context: Context) : this(
        context.applicationContext,
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun runComprehensiveScan(): SecurityScanResult = withContext(Dispatchers.IO) {
        val findings = mutableListOf<SecurityFinding>()

        // 1. USB Debugging & Developer Options check
        val usbDebugging = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        if (usbDebugging) {
            findings.add(
                SecurityFinding(
                    category = "USB",
                    title = "USB Debugging is Enabled",
                    description = "ADB is enabled. An attached computer can access app data and install unauthorized packages.",
                    severity = "high",
                    actionLabel = "Open Dev Options",
                    actionType = "DEVELOPER_SETTINGS"
                )
            )
        }

        // 2. Active Accessibility Services check (often abused by stalkerware/keyloggers)
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServices = am?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) ?: emptyList()
        val nonSystemServices = enabledServices.filter {
            !it.resolveInfo.serviceInfo.packageName.startsWith("com.google.android.") &&
            !it.resolveInfo.serviceInfo.packageName.startsWith("com.android.")
        }
        if (nonSystemServices.isNotEmpty()) {
            findings.add(
                SecurityFinding(
                    category = "ANTI_SPY",
                    title = "Third-Party Accessibility Service Active",
                    description = "Active services: ${nonSystemServices.map { it.resolveInfo.serviceInfo.packageName }.joinToString(", ")}. Accessibility can read all screen text.",
                    severity = "critical",
                    actionLabel = "Review Services",
                    actionType = "ACCESSIBILITY_SETTINGS"
                )
            )
        }

        // 3. System Overlay (Draw over other apps)
        if (Settings.canDrawOverlays(context)) {
            findings.add(
                SecurityFinding(
                    category = "ANTI_SPY",
                    title = "Overlay Permission Active",
                    description = "Screen overlays can potentially capture touches or obscure sensitive prompts.",
                    severity = "medium",
                    actionLabel = "Check Overlays",
                    actionType = "OVERLAY_SETTINGS"
                )
            )
        }

        // 4. Network Security (VPN and WiFi Security)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
        if (isVpn) {
            findings.add(
                SecurityFinding(
                    category = "NETWORK",
                    title = "Active VPN Connection Detected",
                    description = "All network traffic is routed through a VPN tunnel.",
                    severity = "info",
                    actionLabel = "View Status",
                    actionType = "INFO"
                )
            )
        }

        // 5. App Permission Audit
        val criticalPerms = listOf(
            android.Manifest.permission.CAMERA to "Camera access is permitted for photo messages.",
            android.Manifest.permission.POST_NOTIFICATIONS to "Notification permission is active."
        )
        for ((perm, desc) in criticalPerms) {
            val status = ContextCompat.checkSelfPermission(context, perm)
            if (status == PackageManager.PERMISSION_GRANTED) {
                findings.add(
                    SecurityFinding(
                        category = "PERMISSION",
                        title = "Permission Granted: ${perm.substringAfterLast('.')}",
                        description = desc,
                        severity = "low",
                        actionLabel = "Manage",
                        actionType = "APP_PERMISSIONS"
                    )
                )
            }
        }

        // Calculate score
        val crit = findings.count { it.severity == "critical" }
        val high = findings.count { it.severity == "high" }
        val med = findings.count { it.severity == "medium" }
        val low = findings.count { it.severity == "low" }

        val score = (100 - (crit * 30) - (high * 15) - (med * 8) - (low * 2)).coerceIn(10, 100)

        // Log scan event to Firestore
        logSecurityEvent(
            eventType = "scan",
            title = "Comprehensive Security Scan",
            description = "Completed scan. Found $crit critical, $high high, $med medium risks. Score: $score/100",
            severity = if (crit > 0) "critical" else if (high > 0) "high" else "low"
        )

        SecurityScanResult(
            score = score,
            criticalCount = crit,
            highCount = high,
            mediumCount = med,
            lowCount = low,
            findings = findings
        )
    }

    suspend fun analyzeApkFile(uri: Uri, fileName: String, fileSize: Long): ApkAnalysisResult = withContext(Dispatchers.IO) {
        var hash = ""
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                hash = calculateSha256(stream)
            }
        } catch (_: Exception) {}

        val reasons = mutableListOf<String>()
        var risk = "safe"

        // Check file extension and size
        if (fileSize > 100 * 1024 * 1024) {
            reasons.add("Large APK file (>100MB)")
            risk = "warning"
        }

        // Check for suspicious names or blacklisted tooling
        val lowerName = fileName.lowercase()
        val suspiciousKeywords = listOf("mod", "crack", "hack", "spy", "nethunter", "rat", "payload")
        if (suspiciousKeywords.any { lowerName.contains(it) }) {
            reasons.add("File name contains suspicious keywords")
            risk = "danger"
        }

        // APK package parse if possible
        val permissions = mutableListOf<String>()
        try {
            val pm = context.packageManager
            // Copy temporarily to cache to inspect
            val cacheFile = java.io.File(context.cacheDir, "temp_inspect.apk")
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output -> input.copyTo(output) }
            }
            val pkgInfo = pm.getPackageArchiveInfo(cacheFile.absolutePath, PackageManager.GET_PERMISSIONS)
            pkgInfo?.requestedPermissions?.let {
                permissions.addAll(it)
            }
            cacheFile.delete()

            if (permissions.contains("android.permission.BIND_ACCESSIBILITY_SERVICE") ||
                permissions.contains("android.permission.INSTALL_PACKAGES") ||
                permissions.contains("android.permission.RECEIVE_BOOT_COMPLETED")
            ) {
                reasons.add("Requests high-risk system permissions (Accessibility / Install / Boot)")
                risk = "danger"
            }
        } catch (_: Exception) {}

        if (reasons.isEmpty()) {
            reasons.add("No known signatures of high-risk malware. Always verify sender identity.")
        }

        // Log to security events
        logSecurityEvent(
            eventType = "apk_inspect",
            title = "APK Inspection: $fileName",
            description = "Analyzed APK with SHA256: ${hash.take(12)}... Risk: $risk",
            severity = if (risk == "danger") "critical" else if (risk == "warning") "medium" else "low"
        )

        ApkAnalysisResult(
            fileName = fileName,
            fileSize = fileSize,
            sha256Hash = hash,
            riskScore = risk,
            reasons = reasons,
            permissionsRequested = permissions
        )
    }

    fun checkUrlSafety(url: String): Pair<String, String> {
        val u = url.trim().lowercase()
        if (u.contains("bit.ly") || u.contains("tinyurl") || u.contains("is.gd")) {
            return "caution" to "Shortened URL can hide actual destination domain."
        }
        if (u.matches(Regex(".*\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}.*"))) {
            return "warning" to "Raw IP address link instead of registered domain."
        }
        if (u.contains("login") || u.contains("verify") || u.contains("secure") || u.contains("bank")) {
            if (!u.startsWith("https://")) {
                return "danger" to "Insecure HTTP connection requesting credentials."
            }
        }
        return "safe" to "Standard verified domain link."
    }

    private fun calculateSha256(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private suspend fun logSecurityEvent(
        eventType: String,
        title: String,
        description: String,
        severity: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val eventId = "sec_" + UUID.randomUUID().toString().replace("-", "").take(20)
        val path = "users/$uid/securityEvents/$eventId"
        try {
            db.collection("users").document(uid).collection("securityEvents").document(eventId).set(
                mapOf(
                    "eventId" to eventId,
                    "userId" to uid,
                    "eventType" to eventType,
                    "title" to title,
                    "description" to description,
                    "severity" to severity,
                    "remediationStatus" to "pending",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
        }
    }

    fun observeSecurityEvents(): Flow<List<SecurityEventItem>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/securityEvents"
        db.collection("users")
            .document(uid)
            .collection("securityEvents")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { it.toObjects(SecurityEventItem::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
            .collect { emit(it) }
    }
}
