package com.p2pchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.p2pchat.ui.ChatViewModel
import com.p2pchat.ui.screens.ChatScreen
import com.p2pchat.ui.screens.HomeScreen
import com.p2pchat.ui.screens.QRScannerScreen
import com.p2pchat.ui.theme.P2PChatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            P2PChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: ChatViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                uiState = uiState,
                                onStartServer = { viewModel.startServer() },
                                onConnect = { viewModel.connectToPeer(it) },
                                onDisconnect = { viewModel.disconnect() },
                                onClearError = { viewModel.clearError() },
                                onNavigateToChat = {
                                    navController.navigate("chat") {
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToScanner = {
                                    navController.navigate("scanner") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable("chat") {
                            ChatScreen(
                                uiState = uiState,
                                onSendMessage = { viewModel.sendMessage(it) },
                                onDisconnect = { viewModel.disconnect() },
                                onNavigateBack = {
                                    navController.popBackStack("home", inclusive = false)
                                }
                            )
                        }
                        composable("scanner") {
                            QRScannerScreen(
                                onCodeScanned = { code ->
                                    viewModel.connectToPeer(code)
                                    navController.popBackStack()
                                },
                                onClose = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
