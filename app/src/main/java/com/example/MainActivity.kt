package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.model.UserProfile
import com.example.data.model.UserSettingsModel
import com.example.data.repository.ChatRepository
import com.example.data.repository.SecurityRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.UserRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.performSignOut
import com.example.ui.chat.ChatDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.lock.AppLockScreen
import com.example.ui.profile.EditProfileDialog
import com.example.ui.security.SecurityCenterScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.FireonlineChatiTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    data class ChatDetail(val conversationId: String, val title: String, val photoUrl: String?) : Screen()
    object SecurityCenter : Screen()
    object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FireonlineChatiTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        AuthenticatedApp(
            currentUserId = currentUser!!.uid,
            onSignOutRequested = {
                performSignOut(
                    context = context,
                    credentialManager = CredentialManager.create(context),
                    onSignOutComplete = {
                        currentUser = null
                    },
                    scope = scope
                )
            }
        )
    }
}

@Composable
fun AuthenticatedApp(
    currentUserId: String,
    onSignOutRequested: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val userRepository = remember { UserRepository(context) }
    val chatRepository = remember { ChatRepository(context) }
    val securityRepository = remember { SecurityRepository(context) }
    val settingsRepository = remember { SettingsRepository(context) }

    val userProfile by userRepository.observeUserProfile(currentUserId).collectAsState(initial = null)
    val userSettings by settingsRepository.observeUserSettings().collectAsState(
        initial = UserSettingsModel(userId = currentUserId)
    )

    var isUnlocked by remember { mutableStateOf(!userSettings.appLockEnabled) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Screen navigation stack
    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Home

    // Maintain online presence while app is in foreground
    DisposableEffect(currentUserId) {
        scope.launch {
            userRepository.updatePresence(isOnline = true)
        }
        onDispose {
            scope.launch {
                userRepository.updatePresence(isOnline = false)
            }
        }
    }

    // First time setup prompt if profile is not configured yet
    LaunchedEffect(userProfile) {
        if (userProfile != null && userProfile!!.username.isEmpty()) {
            showEditProfileDialog = true
        }
    }

    // App Lock Gate
    if (userSettings.appLockEnabled && !isUnlocked) {
        AppLockScreen(
            settingsRepository = settingsRepository,
            onUnlockSuccess = { isUnlocked = true }
        )
        return
    }

    // Main App Navigation
    Box(modifier = Modifier.fillMaxSize()) {
        when (val screen = currentScreen) {
            is Screen.Home -> {
                HomeScreen(
                    currentUserId = currentUserId,
                    currentUserProfile = userProfile,
                    chatRepository = chatRepository,
                    userRepository = userRepository,
                    onOpenConversation = { id, title, photo ->
                        backStack.add(Screen.ChatDetail(id, title, photo))
                    },
                    onOpenSecurityCenter = {
                        backStack.add(Screen.SecurityCenter)
                    },
                    onOpenSettings = {
                        backStack.add(Screen.Settings)
                    },
                    onEditProfile = {
                        showEditProfileDialog = true
                    }
                )
            }
            is Screen.ChatDetail -> {
                BackHandler {
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                }
                ChatDetailScreen(
                    conversationId = screen.conversationId,
                    title = screen.title,
                    photoUrl = screen.photoUrl,
                    currentUserId = currentUserId,
                    currentUserName = userProfile?.displayName?.ifBlank { "User" } ?: "User",
                    chatRepository = chatRepository,
                    userRepository = userRepository,
                    securityRepository = securityRepository,
                    onBack = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }
            is Screen.SecurityCenter -> {
                BackHandler {
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                }
                SecurityCenterScreen(
                    securityRepository = securityRepository,
                    onBack = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }
            is Screen.Settings -> {
                BackHandler {
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                }
                SettingsScreen(
                    currentProfile = userProfile,
                    settingsRepository = settingsRepository,
                    userRepository = userRepository,
                    onBack = {
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    },
                    onEditProfile = {
                        showEditProfileDialog = true
                    },
                    onSignOut = {
                        scope.launch {
                            userRepository.updatePresence(isOnline = false)
                            onSignOutRequested()
                        }
                    },
                    onOpenSecurityCenter = {
                        backStack.add(Screen.SecurityCenter)
                    }
                )
            }
        }

        if (showEditProfileDialog) {
            EditProfileDialog(
                currentProfile = userProfile,
                userRepository = userRepository,
                onDismiss = { showEditProfileDialog = false },
                onSaved = { showEditProfileDialog = false }
            )
        }
    }
}
