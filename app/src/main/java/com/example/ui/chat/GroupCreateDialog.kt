package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.data.repository.ChatRepository
import com.example.ui.theme.FirePurpleLight
import com.example.ui.theme.FirePurplePrimary
import kotlinx.coroutines.launch

@Composable
fun GroupCreateDialog(
    chatRepository: ChatRepository,
    availableUsers: List<UserProfile>,
    onDismiss: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<String>() }
    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(FirePurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = FirePurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("New Group", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Add Members (${selectedUserIds.size} selected):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF20202B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (availableUsers.isEmpty()) {
                    Text(
                        text = "No other users registered yet. They will appear here once they sign in.",
                        fontSize = 12.sp,
                        color = Color(0xFF888896),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                    ) {
                        items(availableUsers) { user ->
                            val isSelected = selectedUserIds.contains(user.userId)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isSelected) selectedUserIds.remove(user.userId)
                                        else selectedUserIds.add(user.userId)
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(FirePurpleLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!user.photoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = user.photoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = FirePurplePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.displayName.ifBlank { "User" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "@${user.username}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF888896)
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (it) selectedUserIds.add(user.userId)
                                        else selectedUserIds.remove(user.userId)
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = FirePurplePrimary)
                                )
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage!!, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.trim().isEmpty()) {
                        errorMessage = "Please enter a group name"
                        return@Button
                    }
                    if (selectedUserIds.isEmpty()) {
                        errorMessage = "Please select at least 1 member"
                        return@Button
                    }
                    isCreating = true
                    errorMessage = null
                    scope.launch {
                        val result = chatRepository.createGroupConversation(
                            title = groupName,
                            description = description.ifBlank { null },
                            memberIds = selectedUserIds.toList()
                        )
                        isCreating = false
                        if (result.isSuccess) {
                            onGroupCreated(result.getOrThrow())
                        } else {
                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create group"
                        }
                    }
                },
                enabled = !isCreating,
                colors = ButtonDefaults.buttonColors(containerColor = FirePurplePrimary),
                modifier = Modifier.testTag("confirm_create_group_button")
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
