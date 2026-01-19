package com.p2pchat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.p2pchat.network.ChatMessage
import com.p2pchat.network.ConnectionState
import com.p2pchat.network.P2PConnection
import com.p2pchat.util.JoinCode
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val messages: List<ChatMessage> = emptyList(),
    val localAddresses: List<String> = emptyList(),
    val localPort: Int = 0,
    val joinCode: String = "",
    val errorMessage: String? = null
)

class ChatViewModel : ViewModel() {
    private val connection = P2PConnection()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Collect connection state changes
        viewModelScope.launch {
            connection.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        // Collect messages
        viewModelScope.launch {
            connection.messages.collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // Get local addresses
        refreshLocalAddresses()
    }

    fun refreshLocalAddresses() {
        val addresses = connection.getLocalIPv6Addresses()
        _uiState.update { it.copy(localAddresses = addresses) }
    }

    fun startServer(port: Int = 0) {
        viewModelScope.launch {
            connection.startServer(port).onSuccess { actualPort ->
                val addresses = connection.getLocalIPv6Addresses()
                val joinCode = if (addresses.isNotEmpty()) {
                    JoinCode.encode(addresses.first(), actualPort)
                } else ""

                _uiState.update {
                    it.copy(
                        localPort = actualPort,
                        localAddresses = addresses,
                        joinCode = joinCode,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun connectToPeer(input: String) {
        viewModelScope.launch {
            val peerInfo = JoinCode.parseInput(input)
            if (peerInfo == null) {
                _uiState.update { it.copy(errorMessage = "Invalid join code or address format") }
                return@launch
            }

            connection.connectToPeer(peerInfo.address, peerInfo.port).onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        connection.sendMessage(content)
    }

    fun disconnect() {
        connection.stopConnection()
        connection.clearMessages()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        connection.destroy()
    }
}
