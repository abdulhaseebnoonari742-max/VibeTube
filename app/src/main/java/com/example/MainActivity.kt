package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ActiveCallScreen
import com.example.ui.screens.AiScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MessagesInboxScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.WatchScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                VibeTubeApp()
            }
        }
    }
}

@Composable
fun VibeTubeApp(viewModel: VibeTubeViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Handle system back navigation
    BackHandler(enabled = currentScreen != Screen.HOME) {
        when (currentScreen) {
            Screen.CHAT_DETAIL -> viewModel.navigateTo(Screen.MESSAGES)
            Screen.MESSAGES,
            Screen.WATCH,
            Screen.EXPLORE,
            Screen.CREATE,
            Screen.AI,
            Screen.PROFILE,
            Screen.NOTIFICATIONS,
            Screen.AUTH,
            Screen.SHORTS -> viewModel.navigateTo(Screen.HOME)
            Screen.ACTIVE_CALL -> viewModel.endCall()
            else -> {}
        }
    }

    val hideBottomBar = currentScreen == Screen.WATCH ||
            currentScreen == Screen.AUTH ||
            currentScreen == Screen.CHAT_DETAIL ||
            currentScreen == Screen.ACTIVE_CALL

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Show bottom navigation bar on primary browsing screens
            if (!hideBottomBar) {
                VibeTubeBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { screen -> viewModel.navigateTo(screen) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (!hideBottomBar)
                        innerPadding.calculateBottomPadding()
                    else 0.dp
                )
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(viewModel = viewModel)
                Screen.SHORTS -> ShortsScreen(viewModel = viewModel)
                Screen.EXPLORE -> ExploreScreen(viewModel = viewModel)
                Screen.CREATE -> CreateScreen(viewModel = viewModel)
                Screen.AI -> AiScreen(viewModel = viewModel)
                Screen.PROFILE -> ProfileScreen(viewModel = viewModel)
                Screen.WATCH -> WatchScreen(viewModel = viewModel)
                Screen.NOTIFICATIONS -> NotificationsScreen(viewModel = viewModel)
                Screen.AUTH -> AuthScreen(viewModel = viewModel)
                Screen.MESSAGES -> MessagesInboxScreen(viewModel = viewModel)
                Screen.CHAT_DETAIL -> ChatDetailScreen(viewModel = viewModel)
                Screen.ACTIVE_CALL -> ActiveCallScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VibeTubeBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    val isShorts = (currentScreen == Screen.SHORTS)
    val barColor = if (isShorts) Color.Black else MaterialTheme.colorScheme.surface

    Surface(
        color = barColor,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
            .testTag("main_bottom_nav_bar")
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp
        ) {
            // 1. Home
            NavigationBarItem(
                selected = currentScreen == Screen.HOME,
                onClick = { onNavigate(Screen.HOME) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen == Screen.HOME) Icons.Default.Home else Icons.Outlined.Home,
                        contentDescription = "Home"
                    )
                },
                label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_home")
            )

            // 2. Explore
            NavigationBarItem(
                selected = currentScreen == Screen.EXPLORE,
                onClick = { onNavigate(Screen.EXPLORE) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen == Screen.EXPLORE) Icons.Default.Explore else Icons.Outlined.Explore,
                        contentDescription = "Explore"
                    )
                },
                label = { Text("Explore", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_explore")
            )

            // 3. Create (Raised Squircle Button - Elegant Dark: #D0BCFF background, #381E72 icon, border #1C1B1F)
            NavigationBarItem(
                selected = currentScreen == Screen.CREATE,
                onClick = { onNavigate(Screen.CREATE) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .border(3.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                label = { Text("Create", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                ),
                modifier = Modifier.testTag("nav_item_create")
            )

            // 4. VibeAI (with notification badge)
            NavigationBarItem(
                selected = currentScreen == Screen.AI,
                onClick = { onNavigate(Screen.AI) },
                icon = {
                    Box {
                        Icon(
                            imageVector = if (currentScreen == Screen.AI) Icons.Default.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "VibeAI"
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                },
                label = { Text("AI", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_ai")
            )

            // 5. Profile
            NavigationBarItem(
                selected = currentScreen == Screen.PROFILE,
                onClick = { onNavigate(Screen.PROFILE) },
                icon = {
                    Icon(
                        imageVector = if (currentScreen == Screen.PROFILE) Icons.Default.Person else Icons.Outlined.Person,
                        contentDescription = "Profile"
                    )
                },
                label = { Text("Profile", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_item_profile")
            )
        }
    }
}

// Retained for backwards compatibility with GreetingScreenshotTest
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
