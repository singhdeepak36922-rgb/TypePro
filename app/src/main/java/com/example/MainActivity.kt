package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.TypeProRepository
import com.example.ui.DataStatus
import com.example.ui.TypeProViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.GoogleAuthHelper
import com.example.ui.components.EmptyStateCard
import com.example.ui.screens.AchievementsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FreePracticeScreen
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.HindiTypingScreen
import com.example.ui.screens.KeyboardStudioScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsAndProfileScreen
import com.example.ui.screens.TestScreen
import com.example.ui.theme.TypeProTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class TypeProDestination(
  val route: String,
  val label: String,
  val icon: ImageVector,
  val isPrimaryTab: Boolean,
) {
  DASHBOARD("dashboard", "Home", Icons.Filled.Home, true),
  LEARN("learn", "Learn", Icons.Filled.School, true),
  TEST("test", "Test", Icons.Filled.Timer, true),
  HINDI("hindi", "Hindi", Icons.Filled.Translate, true),
  GAMES("games", "Games", Icons.Filled.SportsEsports, true),
  PRACTICE("practice", "Free Practice", Icons.Filled.EditNote, false),
  KEYBOARD("keyboard", "Keyboard Studio", Icons.Filled.Keyboard, false),
  REPORTS("reports", "Reports", Icons.Filled.Analytics, false),
  ACHIEVEMENTS("achievements", "Badges", Icons.Filled.EmojiEvents, false),
  SETTINGS("settings", "Settings & Profile", Icons.Filled.Settings, false),
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      TypeProApp()
    }
  }
}

@Composable
private fun TypeProApp() {
  val authFlow = remember { Firebase.auth.authStateFlow() }
  val currentUser by authFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  val user = currentUser
  if (user == null) {
    TypeProTheme {
      AuthScreen(onAuthSuccess = {})
    }
  } else {
    key(user.uid) {
      AuthenticatedTypeProShell(user = user)
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedTypeProShell(user: FirebaseUser) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val vm: TypeProViewModel = viewModel(
    key = user.uid,
    factory = viewModelFactory {
      initializer {
        val app = this[APPLICATION_KEY]!!
        val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
        TypeProViewModel(
          repository = TypeProRepository(db, Firebase.auth),
          currentUserId = user.uid,
          initialDisplayName = user.displayName ?: "Typist",
        )
      }
    },
  )

  val state by vm.appState.collectAsStateWithLifecycle()
  val newlyUnlocked by vm.newlyUnlocked.collectAsStateWithLifecycle()
  val notice by vm.notice.collectAsStateWithLifecycle()

  var currentDest by rememberSaveable { mutableStateOf(TypeProDestination.DASHBOARD) }
  var selectedLessonSlug by rememberSaveable { mutableStateOf<String?>(null) }

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(newlyUnlocked) {
    if (newlyUnlocked.isNotEmpty()) {
      val titles = newlyUnlocked.joinToString(", ") { it.title }
      snackbarHostState.showSnackbar("Unlocked badge: $titles")
      vm.dismissNewAchievements()
    }
  }

  LaunchedEffect(notice) {
    val msg = notice ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    vm.clearNotice()
  }

  BackHandler(enabled = currentDest != TypeProDestination.DASHBOARD && selectedLessonSlug == null) {
    currentDest = TypeProDestination.DASHBOARD
  }

  TypeProTheme(themeMode = state.settings.theme) {
    ModalNavigationDrawer(
      drawerState = drawerState,
      drawerContent = {
        ModalDrawerSheet {
          Column(
            modifier = Modifier
              .fillMaxHeight()
              .verticalScroll(rememberScrollState())
              .padding(vertical = 16.dp),
          ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = state.settings.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TypeProDestination.entries.forEach { dest ->
              NavigationDrawerItem(
                selected = currentDest == dest,
                onClick = {
                  currentDest = dest
                  if (dest != TypeProDestination.LEARN) selectedLessonSlug = null
                  scope.launch { drawerState.close() }
                },
                icon = { Icon(dest.icon, contentDescription = null) },
                label = { Text(dest.label) },
                modifier = Modifier
                  .padding(horizontal = 12.dp, vertical = 2.dp)
                  .testTag("drawer_item_${dest.route}"),
              )
            }
          }
        }
      },
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp
        val primaryTabs = remember { TypeProDestination.entries.filter { it.isPrimaryTab } }

        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
            TopAppBar(
              title = {
                Text(
                  text = currentDest.label,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.SemiBold,
                )
              },
              navigationIcon = {
                IconButton(
                  onClick = { scope.launch { drawerState.open() } },
                  modifier = Modifier.testTag("open_drawer_button"),
                ) {
                  Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
                }
              },
              actions = {
                IconButton(
                  onClick = { currentDest = TypeProDestination.REPORTS },
                  modifier = Modifier.testTag("top_reports_button"),
                ) {
                  Icon(Icons.Filled.Analytics, contentDescription = "Performance Reports")
                }
                IconButton(
                  onClick = { currentDest = TypeProDestination.ACHIEVEMENTS },
                  modifier = Modifier.testTag("top_badges_button"),
                ) {
                  Icon(Icons.Filled.EmojiEvents, contentDescription = "Achievements")
                }
                IconButton(
                  onClick = { currentDest = TypeProDestination.SETTINGS },
                  modifier = Modifier.testTag("top_settings_button"),
                ) {
                  Icon(Icons.Filled.Settings, contentDescription = "Settings & Profile")
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
              ),
            )
          },
          bottomBar = {
            if (!useRail) {
              NavigationBar {
                primaryTabs.forEach { dest ->
                  NavigationBarItem(
                    selected = currentDest == dest,
                    onClick = {
                      currentDest = dest
                      if (dest != TypeProDestination.LEARN) selectedLessonSlug = null
                    },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label, maxLines = 1) },
                    modifier = Modifier.testTag("bottom_nav_${dest.route}"),
                  )
                }
              }
            }
          },
        ) { innerPadding ->
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            if (useRail) {
              NavigationRail(
                modifier = Modifier.fillMaxHeight(),
              ) {
                Spacer(Modifier.height(8.dp))
                primaryTabs.forEach { dest ->
                  NavigationRailItem(
                    selected = currentDest == dest,
                    onClick = {
                      currentDest = dest
                      if (dest != TypeProDestination.LEARN) selectedLessonSlug = null
                    },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                    modifier = Modifier.testTag("rail_nav_${dest.route}"),
                  )
                }
              }
            }

            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.TopCenter,
            ) {
              val contentModifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()

              when (val status = state.status) {
                is DataStatus.Loading -> {
                  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      CircularProgressIndicator()
                      Text(
                        text = "Syncing TypePro studio…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                }
                is DataStatus.Error -> {
                  Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                      title = "Sync error",
                      description = status.message,
                      actionLabel = "Sign out",
                      onAction = {
                        GoogleAuthHelper.signOut(credentialManager, scope)
                      },
                    )
                  }
                }
                is DataStatus.Ready -> {
                  when (currentDest) {
                    TypeProDestination.DASHBOARD -> DashboardScreen(
                      state = state,
                      onStartTest = { currentDest = TypeProDestination.TEST },
                      onOpenLesson = { slug ->
                        selectedLessonSlug = slug
                        currentDest = TypeProDestination.LEARN
                      },
                      onBrowseLessons = {
                        selectedLessonSlug = null
                        currentDest = TypeProDestination.LEARN
                      },
                      onOpenHindi = { currentDest = TypeProDestination.HINDI },
                      onOpenGames = { currentDest = TypeProDestination.GAMES },
                      onOpenReports = { currentDest = TypeProDestination.REPORTS },
                      modifier = contentModifier,
                    )
                    TypeProDestination.LEARN -> LearnScreen(
                      state = state,
                      selectedLessonSlug = selectedLessonSlug,
                      onSelectLesson = { selectedLessonSlug = it },
                      onSaveResult = { vm.saveSessionResult(it) },
                      modifier = contentModifier,
                    )
                    TypeProDestination.TEST -> TestScreen(
                      state = state,
                      onUpdateSettings = { vm.updateSettings(it) },
                      onSaveResult = { vm.saveSessionResult(it) },
                      modifier = contentModifier,
                    )
                    TypeProDestination.PRACTICE -> FreePracticeScreen(
                      state = state,
                      onSaveResult = { vm.saveSessionResult(it) },
                      modifier = contentModifier,
                    )
                    TypeProDestination.HINDI -> HindiTypingScreen(
                      state = state,
                      onSaveResult = { vm.saveSessionResult(it) },
                      modifier = contentModifier,
                    )
                    TypeProDestination.GAMES -> GamesScreen(
                      state = state,
                      onSaveResult = { vm.saveSessionResult(it) },
                      modifier = contentModifier,
                    )
                    TypeProDestination.KEYBOARD -> KeyboardStudioScreen(
                      state = state,
                      onSaveResult = { vm.saveSessionResult(it) },
                      modifier = contentModifier,
                    )
                    TypeProDestination.REPORTS -> ReportsScreen(
                      state = state,
                      modifier = contentModifier,
                    )
                    TypeProDestination.ACHIEVEMENTS -> AchievementsScreen(
                      state = state,
                      modifier = contentModifier,
                    )
                    TypeProDestination.SETTINGS -> SettingsAndProfileScreen(
                      state = state,
                      userEmail = user.email,
                      onUpdateSettings = { vm.updateSettings(it) },
                      onClearHistory = { vm.clearHistory() },
                      onSignOut = {
                        GoogleAuthHelper.signOut(credentialManager, scope)
                      },
                      modifier = contentModifier,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
