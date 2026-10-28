package com.lightlock.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.lightlock.app.ui.screen.receive.ReceiveScreen
import com.lightlock.app.ui.screen.security.SecurityScreen
import com.lightlock.app.ui.screen.transmit.TransmitScreen
import com.lightlock.app.ui.screen.transmit.QrScannerScreen
import com.lightlock.app.ui.screen.transmit.TransmitProgressScreen
import com.lightlock.app.ui.screen.security.KeyManagementScreen
import com.lightlock.app.ui.screen.security.NonceHistoryScreen

/**
 * LightLock Navigation Graph with Navigation 3
 * Three tabs with independent back stacks: Transmit, Receive, Security
 */

@Composable
fun LightLockNavHost(
    navController: NavHostController,
    startDestination: String = "transmit"
) {
    NavHost(navController, startDestination) {
        // Transmit Tab - Independent back stack
        navigation(
            startDestination = "transmit",
            route = "transmit"
        ) {
            composable("transmit") {
                TransmitScreen(onNavigateToQrScanner = {
                    navController.navigate("transmit/qr_scanner")
                })
            }
            composable("qr_scanner") {
                QrScannerScreen(
                    onChallengeScanned = { challenge, expiry ->
                        navController.navigate("transmit/transmit_progress?challenge=${challenge}&expiry=$expiry")
                    },
                    onClose = {
                        navController.popBackStack("transmit", inclusive = false)
                    }
                )
            }
            composable(
                route = "transmit_progress?challenge={challenge}&expiry={expiry}",
                arguments = listOf(
                    androidx.navigation.navArgument("challenge") { type = androidx.navigation.NavType.StringType },
                    androidx.navigation.navArgument("expiry") { type = androidx.navigation.NavType.LongType }
                )
            ) { backStackEntry ->
                val challenge = backStackEntry.getString() ?: ""
                val expiry = backStackEntry.getLong() ?: 0L
                TransmitProgressScreen(
                    challenge = challenge,
                    challengeExpiry = expiry,
                    onComplete = {
                        navController.popBackStack("transmit", inclusive = false)
                    },
                    onCancel = {
                        navController.popBackStack("transmit", inclusive = false)
                    }
                )
            }
        }

        // Receive Tab - Independent back stack
        navigation(
            startDestination = "receive",
            route = "receive"
        ) {
            composable("receive") {
                ReceiveScreen(onNavigateToDetail = {
                    navController.navigate("receive/frame_detail")
                })
            }
            composable("frame_detail") {
                FrameDetailScreen(onClose = {
                    navController.popBackStack("receive", inclusive = false)
                })
            }
        }

        // Security Tab - Independent back stack
        navigation(
            startDestination = "security",
            route = "security"
        ) {
            composable("security") {
                SecurityScreen(
                    onNavigateToKeyManagement = {
                        navController.navigate("security/key_management")
                    },
                    onNavigateToNonceHistory = {
                        navController.navigate("security/nonce_history")
                    }
                )
            }
            composable("key_management") {
                KeyManagementScreen(onClose = {
                    navController.popBackStack("security", inclusive = false)
                })
            }
            composable("nonce_history") {
                NonceHistoryScreen(onClose = {
                    navController.popBackStack("security", inclusive = false)
                })
            }
        }
    }
}

// Navigation items for bottom bar
data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.material.icons.filled.Icon
)

val navItems = listOf(
    NavItem("transmit", "Transmit", androidx.compose.material.icons.filled.Send),
    NavItem("receive", "Receive", androidx.compose.material.icons.filled.Download),
    NavItem("security", "Security", androidx.compose.material.icons.filled.Security),
)