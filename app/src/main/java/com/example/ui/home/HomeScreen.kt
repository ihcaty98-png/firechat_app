package com.example.ui.home

import com.example.ui.chat.GroupCreateDialog

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Conversation
import com.example.data.model.UserProfile
import com.example.data.repository.ChatRepository
import com.example.data.repository.UserRepository
import com.example.ui.theme.FireBackgroundLight
import com.example.ui.theme.FirePurpleLight
import com.example.ui.theme.FirePurplePrimary
import com.example.ui.theme.FirePurpleSecondary
import com.example.ui.theme.FireSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    currentUserId: String,
    currentUserProfile: UserProfile?,
    chatRepository: ChatRepository,
    userRepository: UserRepository,
    onOpenConversation: (String, String, String?) -> Unit, // id, title, photoUrl
    onOpenSecurityCenter: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit
) {
    val conversations by chatRepository.observeConversations().collectAsState(initial = emptyList())
    val allUsers by userRepository.observeAllUsers().collectAsState(initial = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Chats, 1: Groups, 2: Contacts
    var searchQuery by remember { mutableStateOf("") }
    var showSearchBox by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    val filteredConversations = remember(conversations, selectedTab, searchQuery) {
        conversations.filter { conv ->
            val matchesTab = when (selectedTab) {
                1 -> conv.type == "group"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                (conv.title ?: "").contains(searchQuery, ignoreCase = true) ||
                (conv.lastMessageText ?: "").contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesSearch
        }
    }

    val filteredContacts = remember(allUsers, searchQuery) {
        allUsers.filter { user ->
            if (searchQuery.isBlank()) true else {
                user.displayName.contains(searchQuery, ignoreCase = true) ||
                user.username.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = FireBackgroundLight,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 1) {
                        showCreateGroupDialog = true
                    } else {
                        // Switch to contacts to start new chat
                        selectedTab = 2
                    }
                },
                containerColor = FirePurplePrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("floating_new_chat_button")
            ) {
                Icon(
                    imageVector = if (selectedTab == 1) Icons.Default.Add else Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "New Chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Row: "Hello, {Name}" and Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello,",
                        fontSize = 13.sp,
                        color = Color(0xFF888896),
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = currentUserProfile?.displayName?.ifBlank { "User" } ?: "User",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF20202B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Search icon in rounded circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E5EB), CircleShape)
                            .clickable { showSearchBox = !showSearchBox },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF20202B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // More Menu icon in rounded circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE5E5EB), CircleShape)
                            .clickable { showMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color(0xFF20202B),
                            modifier = Modifier.size(20.dp)
                        )

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Security Center") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, tint = FirePurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    onOpenSecurityCenter()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Profile") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = FirePurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    onEditProfile()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("New Group") },
                                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = FirePurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    showCreateGroupDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = FirePurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // User Avatar with edit shortcut
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(FirePurpleLight)
                            .border(2.dp, FirePurplePrimary, CircleShape)
                            .clickable { onEditProfile() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!currentUserProfile?.photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentUserProfile?.photoUrl,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = FirePurplePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar (if visible)
            if (showSearchBox) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search messages, groups or contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("home_search_bar")
                )
            }

            // Navigation Tabs Pill (All Chats | Groups | Contacts)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFEBEBF4))
                    .padding(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    listOf("All Chats", "Groups", "Contacts").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) FirePurplePrimary else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF6B6B7C)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on tab
            when (selectedTab) {
                0, 1 -> {
                    // Conversations List
                    if (filteredConversations.isEmpty()) {
                        EmptyStateView(
                            title = if (selectedTab == 1) "No Groups Yet" else "No Conversations Yet",
                            description = if (selectedTab == 1) "Tap the '+' button below to start your first group." else "Switch to Contacts tab or tap the chat button to start messaging.",
                            actionLabel = if (selectedTab == 1) "Create Group" else "Find Contacts",
                            onAction = {
                                if (selectedTab == 1) showCreateGroupDialog = true else selectedTab = 2
                            }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredConversations, key = { it.conversationId.ifBlank { it.id } }) { conv ->
                                val convId = conv.conversationId.ifBlank { conv.id }
                                ConversationItemCard(
                                    conversation = conv,
                                    currentUserId = currentUserId,
                                    allUsers = allUsers,
                                    onClick = {
                                        val title = getConversationDisplayTitle(conv, currentUserId, allUsers)
                                        val photo = getConversationPhoto(conv, currentUserId, allUsers)
                                        onOpenConversation(convId, title, photo)
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Contacts List
                    if (filteredContacts.isEmpty()) {
                        EmptyStateView(
                            title = "No Other Users Found",
                            description = "When other users sign in to Fireonline Chati with Google, they will appear here automatically for you to message.",
                            actionLabel = "Invite Friends",
                            onAction = { onOpenSettings() }
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredContacts, key = { it.userId }) { user ->
                                ContactItemCard(
                                    user = user,
                                    onClick = {
                                        // Open or create direct conversation
                                        val currentName = currentUserProfile?.displayName ?: "User"
                                        val targetName = user.displayName.ifBlank { "@${user.username}" }
                                        // We will open conversation with target user
                                        onOpenConversation("DIRECT_${user.userId}", targetName, user.photoUrl)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        GroupCreateDialog(
            chatRepository = chatRepository,
            availableUsers = allUsers,
            onDismiss = { showCreateGroupDialog = false },
            onGroupCreated = { newConvId: String ->
                showCreateGroupDialog = false
                selectedTab = 1
                onOpenConversation(newConvId, "New Group", null)
            }
        )
    }
}

@Composable
fun ConversationItemCard(
    conversation: Conversation,
    currentUserId: String,
    allUsers: List<UserProfile>,
    onClick: () -> Unit
) {
    val title = getConversationDisplayTitle(conversation, currentUserId, allUsers)
    val photo = getConversationPhoto(conversation, currentUserId, allUsers)
    val isOtherOnline = remember(conversation, allUsers) {
        if (conversation.type == "direct") {
            val otherUid = conversation.memberIds.firstOrNull { it != currentUserId }
            allUsers.find { it.userId == otherUid }?.isOnline ?: false
        } else false
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online indicator
            Box(
                modifier = Modifier.size(52.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(FirePurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photo.isNullOrBlank()) {
                        AsyncImage(
                            model = photo,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = if (conversation.type == "group") Icons.Default.Group else Icons.Default.Person,
                            contentDescription = null,
                            tint = FirePurplePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Green Online Dot
                if (isOtherOnline) {
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(FireSuccess)
                            .border(2.dp, Color.White, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Conversation Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF20202B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (conversation.isPinned) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = FirePurplePrimary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // Timestamp
                    val timeStr = formatTimestamp(conversation.lastMessageTimestamp ?: conversation.updatedAt)
                    Text(
                        text = timeStr,
                        fontSize = 11.sp,
                        color = Color(0xFF888896)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.lastMessageText ?: "No messages yet",
                        fontSize = 13.sp,
                        color = Color(0xFF888896),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread badge or muted indicator
                    if (conversation.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(FirePurplePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.unreadCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else if (conversation.isMuted) {
                        Icon(
                            imageVector = Icons.Default.VolumeOff,
                            contentDescription = "Muted",
                            tint = Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItemCard(
    user: UserProfile,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(50.dp)) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(FirePurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    if (!user.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = user.photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = FirePurplePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (user.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(FireSuccess)
                            .border(2.dp, Color.White, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.displayName.ifBlank { "User" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF20202B)
                )
                Text(
                    text = "@${user.username} • ${user.statusMessage ?: "Available"}",
                    fontSize = 12.sp,
                    color = Color(0xFF888896),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(FirePurpleLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chat",
                    tint = FirePurplePrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(FirePurpleLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                tint = FirePurplePrimary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF20202B)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = Color(0xFF888896),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        androidx.compose.material3.Button(
            onClick = onAction,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = FirePurplePrimary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(actionLabel)
        }
    }
}

private fun getConversationDisplayTitle(conv: Conversation, currentUserId: String, allUsers: List<UserProfile>): String {
    if (conv.type == "group") {
        return conv.title ?: "Group Chat"
    }
    val otherUid = conv.memberIds.firstOrNull { it != currentUserId }
    val user = allUsers.find { it.userId == otherUid }
    return user?.displayName?.ifBlank { "@${user.username}" } ?: conv.title ?: "Chat"
}

private fun getConversationPhoto(conv: Conversation, currentUserId: String, allUsers: List<UserProfile>): String? {
    if (conv.type == "group") return conv.photoUrl
    val otherUid = conv.memberIds.firstOrNull { it != currentUserId }
    return allUsers.find { it.userId == otherUid }?.photoUrl ?: conv.photoUrl
}

private fun formatTimestamp(ts: com.google.firebase.Timestamp?): String {
    if (ts == null) return ""
    val date = ts.toDate()
    val now = Date()
    val diffDays = (now.time - date.time) / (1000 * 60 * 60 * 24)
    return when {
        diffDays == 0L -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        diffDays == 1L -> "Yesterday"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
    }
}
