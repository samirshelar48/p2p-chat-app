package com.p2pchat.network

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.*
import java.net.*

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Listening : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val peerAddress: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class ChatMessage(
    val content: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class P2PConnection {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var listenerJob: Job? = null
    private var readerJob: Job? = null

    val localPort: Int get() = serverSocket?.localPort ?: 0

    fun getLocalIPv6Addresses(): List<String> {
        val addresses = mutableListOf<String>()
        try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                if (networkInterface.isUp && !networkInterface.isLoopback) {
                    networkInterface.inetAddresses.toList()
                        .filterIsInstance<Inet6Address>()
                        .filter { !it.isLinkLocalAddress && !it.isLoopbackAddress }
                        .forEach { addresses.add(it.hostAddress?.split("%")?.get(0) ?: "") }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return addresses.filter { it.isNotEmpty() }
    }

    suspend fun startServer(port: Int = 0): Result<Int> = withContext(Dispatchers.IO) {
        try {
            stopConnection()

            // Create IPv6 server socket
            serverSocket = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("::", port))
            }

            val actualPort = serverSocket!!.localPort
            _connectionState.value = ConnectionState.Listening

            // Start listening for connections
            listenerJob = scope.launch {
                try {
                    while (isActive) {
                        val socket = serverSocket?.accept() ?: break
                        handleIncomingConnection(socket)
                    }
                } catch (e: SocketException) {
                    if (isActive) {
                        _connectionState.value = ConnectionState.Error("Server stopped: ${e.message}")
                    }
                }
            }

            Result.success(actualPort)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Failed to start server: ${e.message}")
            Result.failure(e)
        }
    }

    private fun handleIncomingConnection(socket: Socket) {
        clientSocket?.close()
        clientSocket = socket

        val peerAddress = socket.inetAddress.hostAddress ?: "unknown"
        setupStreams(socket)
        _connectionState.value = ConnectionState.Connected(peerAddress)
        startMessageReader()
    }

    suspend fun connectToPeer(address: String, port: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting

            // Parse IPv6 address
            val inetAddress = InetAddress.getByName(address)
            val socket = Socket()
            socket.connect(InetSocketAddress(inetAddress, port), 10000)

            clientSocket = socket
            setupStreams(socket)
            _connectionState.value = ConnectionState.Connected(address)
            startMessageReader()

            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}")
            Result.failure(e)
        }
    }

    private fun setupStreams(socket: Socket) {
        reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)
    }

    private fun startMessageReader() {
        readerJob?.cancel()
        readerJob = scope.launch {
            try {
                while (isActive) {
                    val line = reader?.readLine() ?: break
                    val message = ChatMessage(content = line, isFromMe = false)
                    _messages.value = _messages.value + message
                }
            } catch (e: Exception) {
                if (isActive) {
                    _connectionState.value = ConnectionState.Error("Connection lost: ${e.message}")
                }
            } finally {
                if (_connectionState.value is ConnectionState.Connected) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    fun sendMessage(content: String): Boolean {
        return try {
            writer?.println(content)
            val message = ChatMessage(content = content, isFromMe = true)
            _messages.value = _messages.value + message
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stopConnection() {
        listenerJob?.cancel()
        readerJob?.cancel()

        try { reader?.close() } catch (_: Exception) {}
        try { writer?.close() } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}

        reader = null
        writer = null
        clientSocket = null
        serverSocket = null

        _connectionState.value = ConnectionState.Disconnected
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun destroy() {
        stopConnection()
        scope.cancel()
    }
}
