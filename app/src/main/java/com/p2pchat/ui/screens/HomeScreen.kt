package com.p2pchat.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.p2pchat.network.ConnectionState
import com.p2pchat.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: UiState,
    onStartServer: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onClearError: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToScanner: () -> Unit
) {
    val context = LocalContext.current
    var peerInput by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }
    var showQRDialog by remember { mutableStateOf(false) }

    // Navigate to chat when connected
    LaunchedEffect(uiState.connectionState) {
        if (uiState.connectionState is ConnectionState.Connected) {
            onNavigateToChat()
        }
    }

    // Show error snackbar
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            onClearError()
        }
    }

    // QR Code Dialog
    if (showQRDialog && uiState.joinCode.isNotEmpty()) {
        QRDisplayDialog(
            joinCode = uiState.joinCode,
            onDismiss = { showQRDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("P2P Chat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (uiState.connectionState) {
                        is ConnectionState.Listening -> MaterialTheme.colorScheme.primaryContainer
                        is ConnectionState.Connected -> MaterialTheme.colorScheme.tertiaryContainer
                        is ConnectionState.Error -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = when (uiState.connectionState) {
                            is ConnectionState.Listening -> Icons.Default.Wifi
                            is ConnectionState.Connected -> Icons.Default.CheckCircle
                            is ConnectionState.Connecting -> Icons.Default.Sync
                            else -> Icons.Default.WifiOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (uiState.connectionState) {
                            is ConnectionState.Disconnected -> "Not Connected"
                            is ConnectionState.Listening -> "Waiting for peer..."
                            is ConnectionState.Connecting -> "Connecting..."
                            is ConnectionState.Connected -> "Connected!"
                            is ConnectionState.Error -> "Error"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Your Join Code Section
            if (uiState.connectionState is ConnectionState.Listening && uiState.joinCode.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Your Join Code",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.joinCode,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Action buttons row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Copy button
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Join Code", uiState.joinCode))
                                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy")
                            }

                            // Show QR button
                            Button(
                                onClick = { showQRDialog = true }
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Show QR")
                            }
                        }

                        // Show raw address toggle
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "Hide Details" else "Show Details")
                        }

                        if (showAdvanced && uiState.localAddresses.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "IPv6: ${uiState.localAddresses.first()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Port: ${uiState.localPort}",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Start Server / Stop Button
            if (uiState.connectionState is ConnectionState.Disconnected) {
                Button(
                    onClick = onStartServer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Server (Host)")
                }
            } else if (uiState.connectionState is ConnectionState.Listening) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Stop Server")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Connect to Peer Section
            Text(
                text = "Or connect to someone",
                style = MaterialTheme.typography.titleMedium
            )

            // Scan QR Button
            OutlinedButton(
                onClick = onNavigateToScanner,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan QR Code")
            }

            Text(
                text = "or enter code manually",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            OutlinedTextField(
                value = peerInput,
                onValueChange = { peerInput = it },
                label = { Text("Join Code or [IPv6]:port") },
                placeholder = { Text("Paste join code here") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    if (peerInput.isNotEmpty()) {
                        IconButton(onClick = { peerInput = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            Button(
                onClick = { onConnect(peerInput) },
                enabled = peerInput.isNotBlank() && uiState.connectionState !is ConnectionState.Connecting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.connectionState is ConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect")
                }
            }

            // Info Card
            if (uiState.localAddresses.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No global IPv6 address found. Make sure you have IPv6 connectivity.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
