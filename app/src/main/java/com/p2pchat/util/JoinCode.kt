package com.p2pchat.util

import android.util.Base64
import java.nio.ByteBuffer

/**
 * Encodes/decodes IPv6 address + port into a compact, shareable join code.
 *
 * Format: Base64(IPv6_bytes[16] + port_bytes[2])
 * Results in a ~24 character code that's easy to share.
 */
object JoinCode {

    data class PeerInfo(
        val address: String,
        val port: Int
    )

    /**
     * Encodes an IPv6 address and port into a compact join code.
     */
    fun encode(address: String, port: Int): String {
        // Parse IPv6 address to bytes
        val addressBytes = parseIPv6ToBytes(address)

        // Create buffer: 16 bytes for IPv6 + 2 bytes for port
        val buffer = ByteBuffer.allocate(18)
        buffer.put(addressBytes)
        buffer.putShort(port.toShort())

        // Encode to URL-safe Base64
        return Base64.encodeToString(buffer.array(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Decodes a join code back to IPv6 address and port.
     */
    fun decode(code: String): PeerInfo? {
        return try {
            val bytes = Base64.decode(code.trim(), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            if (bytes.size != 18) return null

            val buffer = ByteBuffer.wrap(bytes)
            val addressBytes = ByteArray(16)
            buffer.get(addressBytes)
            val port = buffer.short.toInt() and 0xFFFF

            val address = bytesToIPv6String(addressBytes)
            PeerInfo(address, port)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseIPv6ToBytes(address: String): ByteArray {
        // Remove zone identifier if present (e.g., %eth0)
        val cleanAddress = address.split("%")[0]

        // Handle :: expansion
        val parts = expandIPv6(cleanAddress)
        val bytes = ByteArray(16)

        for (i in 0 until 8) {
            val value = parts.getOrNull(i)?.toIntOrNull(16) ?: 0
            bytes[i * 2] = (value shr 8).toByte()
            bytes[i * 2 + 1] = value.toByte()
        }

        return bytes
    }

    private fun expandIPv6(address: String): List<String> {
        if (!address.contains("::")) {
            return address.split(":")
        }

        val parts = address.split("::")
        val left = if (parts[0].isEmpty()) emptyList() else parts[0].split(":")
        val right = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].split(":") else emptyList()

        val missing = 8 - left.size - right.size
        val middle = List(missing) { "0" }

        return left + middle + right
    }

    private fun bytesToIPv6String(bytes: ByteArray): String {
        val groups = mutableListOf<String>()
        for (i in 0 until 8) {
            val value = ((bytes[i * 2].toInt() and 0xFF) shl 8) or (bytes[i * 2 + 1].toInt() and 0xFF)
            groups.add(value.toString(16))
        }

        // Find longest run of zeros for :: compression
        var longestStart = -1
        var longestLen = 0
        var currentStart = -1
        var currentLen = 0

        for (i in groups.indices) {
            if (groups[i] == "0") {
                if (currentStart == -1) currentStart = i
                currentLen++
                if (currentLen > longestLen) {
                    longestStart = currentStart
                    longestLen = currentLen
                }
            } else {
                currentStart = -1
                currentLen = 0
            }
        }

        // Build compressed string
        return if (longestLen > 1) {
            val before = groups.take(longestStart).joinToString(":")
            val after = groups.drop(longestStart + longestLen).joinToString(":")
            when {
                before.isEmpty() && after.isEmpty() -> "::"
                before.isEmpty() -> "::$after"
                after.isEmpty() -> "$before::"
                else -> "$before::$after"
            }
        } else {
            groups.joinToString(":")
        }
    }

    /**
     * Validates if a string looks like a valid join code.
     */
    fun isValidCode(code: String): Boolean {
        return decode(code) != null
    }

    /**
     * Checks if the input is a raw IPv6 address (contains colons but not a join code).
     */
    fun isRawIPv6(input: String): Boolean {
        return input.contains(":") && !isValidCode(input)
    }

    /**
     * Parses user input that could be either a join code or raw IPv6:port.
     */
    fun parseInput(input: String): PeerInfo? {
        val trimmed = input.trim()

        // Try as join code first
        decode(trimmed)?.let { return it }

        // Try as [IPv6]:port format
        val bracketMatch = Regex("""\[([^\]]+)\]:(\d+)""").find(trimmed)
        if (bracketMatch != null) {
            val address = bracketMatch.groupValues[1]
            val port = bracketMatch.groupValues[2].toIntOrNull() ?: return null
            return PeerInfo(address, port)
        }

        // Try as IPv6 port (last segment after last colon if it's numeric)
        val lastColon = trimmed.lastIndexOf(':')
        if (lastColon > 0) {
            val possiblePort = trimmed.substring(lastColon + 1)
            val port = possiblePort.toIntOrNull()
            if (port != null && port in 1..65535) {
                val address = trimmed.substring(0, lastColon)
                return PeerInfo(address, port)
            }
        }

        return null
    }
}
