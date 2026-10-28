package com.lightlock.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.lightlock.app.ui.navigation.LightLockNavHost
import com.lightlock.app.ui.navigation.navItems
import com.lightlock.app.ui.navigation.NavItem
import com.lightlock.app.ui.theme.LightLockTheme

/**
 * Main Activity for LightLock Smartphone App
 * Uses Jetpack Compose with Navigation 3
 */
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LightLockTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    LightLockApp(navController = rememberNavController())
                }
            }
        }
    }

    private fun enableEdgeToEdge() {
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
        window.navigationBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
    }
}

@Composable
fun LightLockApp(navController: NavHostController) {
    // Bottom navigation bar
    val scaffoldState = rememberScaffoldState()

    androidx.compose.material3.Scaffold(
        scaffoldState = scaffoldState,
        bottomBar = {
            LightLockBottomBar(
                navController = navController,
                items = navItems
            )
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LightLockNavHost(navController)
        }
    }
}

@Composable
fun LightLockBottomBar(
    navController: NavHostController,
    items: List<NavItem>
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: ""
    val currentTab = items.find { currentRoute.startsWith(it.route) } ?: items[0]

    androidx.compose.material3.NavigationBar(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            val isSelected = (currentTab == item)
            androidx.compose.material3.NavigationBarItem(
                icon = {
                    androidx.compose.material.Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected)
                            androidx.compose.material3.MaterialTheme.colorScheme.primary
                        else
                            androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = { androidx.compose.material3.Text(text = item.label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) },
                selected = isSelected,
                onClick = {
                    // Navigate to the tab's start destination, preserving back stack
                    val targetRoute = item.route
                    if (!currentRoute.startsWith(targetRoute)) {
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                alwaysShowLabel = true
            )
        }
    }
}

/**
 * ViewModel for app-level state
 */
class MainViewModel : ViewModel() {
    // App-level state can go here
    // e.g., pairing state, crypto manager, etc.
}

class MainViewModelProvider(
    private val context: android.content.Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel() as T
    }
}