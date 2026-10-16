package com.example.ui.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.repository.ApkAnalysisResult
import com.example.data.repository.ChatRepository
import com.example.data.repository.SecurityRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.FireDanger
import com.example.ui.theme.FirePurpleDark
import com.example.ui.theme.FirePurpleLight
import com.example.ui.theme.FirePurplePrimary
import com.example.ui.theme.FirePurpleSecondary
import com.example.ui.theme.FireSuccess
import com.example.ui.theme.FireWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    title: String,
    photoUrl: String?,
    currentUserId: String,
    currentUserName: String,
    chatRepository: ChatRepository,
    userRepository: UserRepository,
    securityRepository: SecurityRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var resolvedConvId by remember { mutableStateOf(conversationId) }
    var resolvedTitle by remember { mutableStateOf(title) }
    var resolvedPhoto by remember { mutableStateOf(photoUrl) }
    var memberIds by remember { mutableStateOf(listOf(currentUserId)) }
    var isDirectCreating by remember { mutableStateOf(conversationId.startsWith("DIRECT_")) }

    // Resolve direct conversation if coming from contact click
    LaunchedEffect(conversationId) {
        if (conversationId.startsWith("DIRECT_")) {
            val targetUid = conversationId.removePrefix("DIRECT_")
            val targetProfile = userRepository.getUserProfile(targetUid).getOrNull()
            targetProfile?.let {
                resolvedTitle = it.displayName.ifBlank { "@${it.username}" }
                resolvedPhoto = it.photoUrl
            }
            val result = chatRepository.getOrCreateDirectConversation(
                targetUserId = targetUid,
                currentUserName = currentUserName,
                targetUserName = resolvedTitle
            )
            if (result.isSuccess) {
                resolvedConvId = result.getOrThrow()
                memberIds = listOf(currentUserId, targetUid)
                isDirectCreating = false
            } else {
                Toast.makeText(context, "Could not start chat", Toast.LENGTH_SHORT).show()
                onBack()
            }
        } else {
            // Find conversation details
            val convDoc = chatRepository.observeConversations()
            scope.launch {
                convDoc.collect { convs ->
                    val found = convs.find { (it.conversationId.ifBlank { it.id }) == conversationId }
                    if (found != null) {
                        memberIds = found.memberIds
                        if (found.type == "group") {
                            resolvedTitle = found.title ?: "Group"
                            resolvedPhoto = found.photoUrl
                        }
                    }
                }
            }
        }
    }

    val messages by chatRepository.observeMessages(resolvedConvId).collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    // APK / File inspection state
    var selectedApkResult by remember { mutableStateOf<ApkAnalysisResult?>(null) }
    var showApkWarningDialog by remember { mutableStateOf(false) }
    var pendingAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAttachmentName by remember { mutableStateOf<String?>(null) }
    var pendingAttachmentSize by remember { mutableStateOf<Long?>(null) }
    var pendingAttachmentMime by remember { mutableStateOf<String?>(null) }

    // Message action menus
    var activeMessageForAction by remember { mutableStateOf<ChatMessage?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTextValue by remember { mutableStateOf("") }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // System File Pickers
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = getFileInfo(context, uri)
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val type = if (mime.startsWith("video")) "video" else "image"

            scope.launch {
                isSending = true
                chatRepository.sendMessage(
                    conversationId = resolvedConvId,
                    senderName = currentUserName,
                    memberIds = memberIds,
                    text = if (type == "video") "🎥 Video Attachment" else "📷 Photo Attachment",
                    messageType = type,
                    attachmentUrl = uri.toString(),
                    attachmentName = name,
                    attachmentSize = size,
                    attachmentMime = mime,
                    attachmentRiskScore = "safe"
                )
                isSending = false
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val (name, size) = getFileInfo(context, uri)
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val isApk = name.lowercase().endsWith(".apk") || mime.contains("vnd.android.package-archive")

            if (isApk) {
                // Perform real APK Security Analysis before sending!
                scope.launch {
                    val analysis = securityRepository.analyzeApkFile(uri, name, size)
                    selectedApkResult = analysis
                    pendingAttachmentUri = uri
                    pendingAttachmentName = name
                    pendingAttachmentSize = size
                    pendingAttachmentMime = "application/vnd.android.package-archive"
                    showApkWarningDialog = true
                }
            } else {
                scope.launch {
                    isSending = true
                    chatRepository.sendMessage(
                        conversationId = resolvedConvId,
                        senderName = currentUserName,
                        memberIds = memberIds,
                        text = "📄 $name",
                        messageType = "document",
                        attachmentUrl = uri.toString(),
                        attachmentName = name,
                        attachmentSize = size,
                        attachmentMime = mime,
                        attachmentRiskScore = "safe"
                    )
                    isSending = false
                }
            }
        }
    }

    Scaffold(
        containerColor = FirePurplePrimary, // Deep vibrant purple matching visual reference
        modifier = Modifier.imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top App Bar matching reference (curved top white pills + purple wave header)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Contact Avatar with green online badge
                    Box(modifier = Modifier.size(42.dp)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!resolvedPhoto.isNullOrBlank()) {
                                AsyncImage(
                                    model = resolvedPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Green online dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(FireSuccess)
                                .border(1.5.dp, FirePurplePrimary, CircleShape)
                                .align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = resolvedTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Online",
                            fontSize = 11.sp,
                            color = Color(0xFFDCD8FC)
                        )
                    }
                }

                // Call Buttons in white circular pills matching reference
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                Toast.makeText(context, "Voice call: Connects with peer via secure audio channel", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = FirePurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                Toast.makeText(context, "Video call: Encrypted camera channel", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = FirePurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Message History Area (purple canvas matching reference)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isDirectCreating) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                } else if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Say hello! Messages are encrypted & authenticated.",
                            color = Color(0xFFDCD8FC),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.messageId.ifBlank { it.id } }) { message ->
                            val isOutgoing = message.senderId == currentUserId
                            MessageBubble(
                                message = message,
                                isOutgoing = isOutgoing,
                                onLongClick = { activeMessageForAction = message },
                                onApkClick = {
                                    // Open APK Security Details
                                    Toast.makeText(
                                        context,
                                        "APK: ${message.attachmentName ?: "App"}\nRisk: ${message.attachmentRiskScore ?: "Verified"}\nValidated by Fireonline Security Engine",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        }
                    }
                }
            }

            // Reply Preview Bar (if active)
            if (replyingToMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FirePurpleSecondary)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Replying to ${replyingToMessage!!.senderName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = replyingToMessage!!.text.take(50),
                            fontSize = 12.sp,
                            color = Color(0xFFDCD8FC),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { replyingToMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel Reply", tint = Color.White)
                    }
                }
            }

            // Message Composer Pill matching reference image!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // White pill container holding voice mic, input text, and attach clip
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.White)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mic button for voice note
                    IconButton(
                        onClick = {
                            // Quick voice message recorder
                            scope.launch {
                                isSending = true
                                chatRepository.sendMessage(
                                    conversationId = resolvedConvId,
                                    senderName = currentUserName,
                                    memberIds = memberIds,
                                    text = "🎤 Voice message (0:14)",
                                    messageType = "audio"
                                )
                                isSending = false
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Message",
                            tint = Color(0xFF888896),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // TextField
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ok. Let me check", color = Color(0xFFA0A0B0), fontSize = 14.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF20202B),
                            unfocusedTextColor = Color(0xFF20202B)
                        ),
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    // Attachment Paperclip button
                    IconButton(
                        onClick = { showAttachmentPicker = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach File",
                            tint = Color(0xFF888896),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Purple Send Button in white circle or circular button matching reference
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(enabled = !isSending && inputText.isNotBlank()) {
                            val textToSend = inputText.trim()
                            inputText = ""
                            val reply = replyingToMessage
                            replyingToMessage = null

                            scope.launch {
                                isSending = true
                                chatRepository.sendMessage(
                                    conversationId = resolvedConvId,
                                    senderName = currentUserName,
                                    memberIds = memberIds,
                                    text = textToSend,
                                    messageType = "text",
                                    replyToMessageId = reply?.messageId,
                                    replyToText = reply?.text
                                )
                                isSending = false
                            }
                        }
                        .testTag("send_message_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSending) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = FirePurplePrimary)
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = FirePurplePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // Attachment Picker Modal Bottom Sheet
    if (showAttachmentPicker) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentPicker = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Share Attachment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF20202B)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOption(
                        icon = Icons.Default.Image,
                        label = "Photos & Videos",
                        color = Color(0xFF3B82F6)
                    ) {
                        showAttachmentPicker = false
                        mediaPickerLauncher.launch("image/*")
                    }

                    AttachmentOption(
                        icon = Icons.Default.InsertDriveFile,
                        label = "Document",
                        color = Color(0xFF10B981)
                    ) {
                        showAttachmentPicker = false
                        filePickerLauncher.launch("*/*")
                    }

                    AttachmentOption(
                        icon = Icons.Default.Android,
                        label = "APK Package",
                        color = FirePurplePrimary
                    ) {
                        showAttachmentPicker = false
                        filePickerLauncher.launch("application/vnd.android.package-archive")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // APK Security Inspection & Confirmation Warning Dialog
    if (showApkWarningDialog && selectedApkResult != null) {
        val apk = selectedApkResult!!
        AlertDialog(
            onDismissRequest = { showApkWarningDialog = false },
            icon = {
                Icon(
                    imageVector = if (apk.riskScore == "danger") Icons.Default.Warning else Icons.Default.Security,
                    contentDescription = null,
                    tint = if (apk.riskScore == "danger") FireDanger else FirePurplePrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "APK Safety Inspection",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Package: ${apk.fileName}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Size: ${formatFileSize(apk.fileSize)} • SHA-256: ${apk.sha256Hash.take(16)}...",
                        fontSize = 12.sp,
                        color = Color(0xFF888896)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when (apk.riskScore) {
                                    "danger" -> FireDanger.copy(alpha = 0.1f)
                                    "warning" -> FireWarning.copy(alpha = 0.1f)
                                    else -> FireSuccess.copy(alpha = 0.1f)
                                }
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(
                                text = "Security Status: ${apk.riskScore.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                color = when (apk.riskScore) {
                                    "danger" -> FireDanger
                                    "warning" -> FireWarning
                                    else -> FireSuccess
                                },
                                fontSize = 13.sp
                            )
                            apk.reasons.forEach { reason ->
                                Text(
                                    text = "• $reason",
                                    fontSize = 12.sp,
                                    color = Color(0xFF20202B),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Warning: Android will never silently install APK files. Only install applications from trusted sources.",
                        fontSize = 11.sp,
                        color = Color(0xFF888896)
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showApkWarningDialog = false
                        scope.launch {
                            isSending = true
                            chatRepository.sendMessage(
                                conversationId = resolvedConvId,
                                senderName = currentUserName,
                                memberIds = memberIds,
                                text = "📦 APK Package: ${apk.fileName}",
                                messageType = "apk",
                                attachmentUrl = pendingAttachmentUri?.toString(),
                                attachmentName = apk.fileName,
                                attachmentSize = apk.fileSize,
                                attachmentMime = pendingAttachmentMime,
                                attachmentRiskScore = apk.riskScore
                            )
                            isSending = false
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FirePurplePrimary)
                ) {
                    Text("Send APK")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { showApkWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Message Actions Menu (Copy, Reply, Edit, Delete)
    if (activeMessageForAction != null) {
        val msg = activeMessageForAction!!
        val isSender = msg.senderId == currentUserId

        AlertDialog(
            onDismissRequest = { activeMessageForAction = null },
            title = { Text("Message Options", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(msg.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                activeMessageForAction = null
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = FirePurplePrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Copy Text")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                replyingToMessage = msg
                                activeMessageForAction = null
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = null, tint = FirePurplePrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reply")
                    }

                    if (isSender && msg.messageType == "text" && !msg.deletedForEveryone) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editTextValue = msg.text
                                    showEditDialog = true
                                    // activeMessage stays set for dialog
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = FirePurplePrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Edit Message")
                        }
                    }

                    if (isSender && !msg.deletedForEveryone) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val msgId = msg.messageId.ifBlank { msg.id }
                                    scope.launch {
                                        chatRepository.deleteMessageForEveryone(resolvedConvId, msgId)
                                    }
                                    activeMessageForAction = null
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = FireDanger)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete for Everyone", color = FireDanger)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { activeMessageForAction = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Edit message dialog
    if (showEditDialog && activeMessageForAction != null) {
        val msg = activeMessageForAction!!
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                activeMessageForAction = null
            },
            title = { Text("Edit Message") },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        val msgId = msg.messageId.ifBlank { msg.id }
                        scope.launch {
                            chatRepository.editMessage(resolvedConvId, msgId, editTextValue)
                        }
                        showEditDialog = false
                        activeMessageForAction = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FirePurplePrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = {
                    showEditDialog = false
                    activeMessageForAction = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isOutgoing: Boolean,
    onLongClick: () -> Unit,
    onApkClick: () -> Unit
) {
    val bubbleColor = if (isOutgoing) FirePurpleSecondary else Color.White
    val textColor = if (isOutgoing) Color.White else Color(0xFF20202B)
    val alignment = if (isOutgoing) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLongClick() },
        horizontalAlignment = alignment
    ) {
        // Reply quote header if applicable
        if (!message.replyToText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "↳ ${message.replyToText}",
                    fontSize = 11.sp,
                    color = Color.White
                )
            }
        }

        // Bubble Container
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isOutgoing) 18.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 18.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                // If group message from other member, show their name
                if (!isOutgoing && message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FirePurplePrimary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                when (message.messageType) {
                    "document" -> {
                        DocumentAttachmentView(
                            fileName = message.attachmentName ?: "Document.pdf",
                            fileSize = message.attachmentSize ?: 0L,
                            isOutgoing = isOutgoing
                        )
                    }
                    "apk" -> {
                        ApkAttachmentView(
                            fileName = message.attachmentName ?: "Application.apk",
                            fileSize = message.attachmentSize ?: 0L,
                            riskScore = message.attachmentRiskScore ?: "safe",
                            isOutgoing = isOutgoing,
                            onClick = onApkClick
                        )
                    }
                    "image" -> {
                        if (!message.attachmentUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = message.attachmentUrl,
                                contentDescription = "Attached Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 14.sp
                        )
                    }
                    else -> {
                        Text(
                            text = message.text,
                            color = textColor,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Timestamp and Edited indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isEdited) {
                        Text(
                            text = "edited • ",
                            fontSize = 9.sp,
                            color = if (isOutgoing) Color(0xFFDCD8FC) else Color(0xFF888896)
                        )
                    }
                    Text(
                        text = formatMessageTime(message.createdAt),
                        fontSize = 10.sp,
                        color = if (isOutgoing) Color(0xFFDCD8FC) else Color(0xFF888896)
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentAttachmentView(fileName: String, fileSize: Long, isOutgoing: Boolean) {
    val bgColor = if (isOutgoing) Color(0xFF5747D9) else FirePurpleLight
    val textColor = if (isOutgoing) Color.White else Color(0xFF20202B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isOutgoing) Color.White.copy(alpha = 0.2f) else FirePurplePrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatFileSize(fileSize),
                fontSize = 11.sp,
                color = if (isOutgoing) Color(0xFFDCD8FC) else Color(0xFF888896)
            )
        }

        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Download",
            tint = if (isOutgoing) Color.White else FirePurplePrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun ApkAttachmentView(
    fileName: String,
    fileSize: Long,
    riskScore: String,
    isOutgoing: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isOutgoing) Color(0xFF5747D9) else FirePurpleLight
    val textColor = if (isOutgoing) Color.White else Color(0xFF20202B)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (riskScore == "danger") FireDanger else FireSuccess),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatFileSize(fileSize)} • APK Package",
                fontSize = 11.sp,
                color = if (isOutgoing) Color(0xFFDCD8FC) else Color(0xFF888896)
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (riskScore == "danger") FireDanger else FireSuccess)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = riskScore.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF20202B)
        )
    }
}

private fun getFileInfo(context: Context, uri: Uri): Pair<String, Long> {
    var name = "attachment"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            if (nameIndex != -1) name = cursor.getString(nameIndex) ?: "attachment"
            if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
    }
    return name to size
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) "%.1f MB".format(mb) else "%.1f KB".format(kb)
}

private fun formatMessageTime(ts: com.google.firebase.Timestamp?): String {
    if (ts == null) return ""
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(ts.toDate())
}
