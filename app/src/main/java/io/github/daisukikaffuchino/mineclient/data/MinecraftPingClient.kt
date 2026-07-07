package io.github.daisukikaffuchino.mineclient.data

import android.net.DnsResolver
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import io.github.daisukikaffuchino.mineclient.ui.ServerEdition
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlin.math.roundToInt
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

private const val DefaultMinecraftPort = 25565
private const val DefaultTimeoutMillis = 5_000
private const val DnsServer = "8.8.8.8"
private const val DnsPort = 53
private const val CurrentStatusProtocol = 764
private const val LegacyStatusProtocol = 47

data class ServerAddress(
    val host: String,
    val port: Int = DefaultMinecraftPort,
)

data class ServerStatus(
    val address: ServerAddress,
    val latencyMillis: Long,
    val protocolName: MinecraftText,
    val protocolVersion: Int,
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val motd: MinecraftText,
    val samplePlayers: List<MinecraftText>,
    val faviconBase64: String?,
)

class MinecraftPingClient {
    suspend fun query(
        input: String,
        edition: ServerEdition = ServerEdition.Java,
        enableLegacyProtocolFallback: Boolean = false,
    ): Result<ServerStatus> = withContext(Dispatchers.IO) {
        runCatching {
            when (edition) {
                ServerEdition.Java -> {
                    val address = resolveAddress(input)
                    queryJavaServerWithFallback(address, enableLegacyProtocolFallback)
                }
                ServerEdition.Bedrock -> {
                    val address = resolveBedrockAddress(input)
                    queryBedrockServer(address)
                }
            }
        }
    }

    private suspend fun resolveAddress(input: String): ServerAddress {
        val trimmed = input.trim().removePrefix("minecraft://")
        require(trimmed.isNotBlank()) { "Server address is required" }

        val parsed = when {
            trimmed.startsWith("[") -> parseIpv6Address(trimmed)
            trimmed.count { it == ':' } == 1 -> {
                val host = trimmed.substringBefore(':').trim()
                val port = trimmed.substringAfter(':').trim().toIntOrNull() ?: DefaultMinecraftPort
                ServerAddress(host, port)
            }
            else -> ServerAddress(trimmed, DefaultMinecraftPort)
        }

        require(parsed.host.isNotBlank()) { "Server address is required" }
        require(parsed.port in 1..65535) { "Port must be between 1 and 65535" }

        return if (parsed.port == DefaultMinecraftPort && !isIpLiteral(parsed.host)) {
            lookupSrv(parsed.host) ?: parsed
        } else {
            parsed
        }
    }

    private fun resolveBedrockAddress(input: String): ServerAddress {
        val trimmed = input.trim().removePrefix("minecraft://")
        require(trimmed.isNotBlank()) { "Server address is required" }
        val parsed = when {
            trimmed.startsWith("[") -> parseIpv6Address(trimmed)
            trimmed.count { it == ':' } == 1 -> {
                val host = trimmed.substringBefore(':').trim()
                val port = trimmed.substringAfter(':').trim().toIntOrNull() ?: 19132
                ServerAddress(host, port)
            }
            else -> ServerAddress(trimmed, 19132)
        }
        require(parsed.host.isNotBlank()) { "Server address is required" }
        require(parsed.port in 1..65535) { "Port must be between 1 and 65535" }
        return parsed
    }
    private fun parseIpv6Address(value: String): ServerAddress {
        val endIndex = value.indexOf(']')
        require(endIndex > 1) { "Invalid IPv6 address" }
        val host = value.substring(1, endIndex)
        val port = if (value.length > endIndex + 1 && value[endIndex + 1] == ':') {
            value.substring(endIndex + 2).toIntOrNull() ?: DefaultMinecraftPort
        } else {
            DefaultMinecraftPort
        }
        return ServerAddress(host, port)
    }

    private suspend fun lookupSrv(host: String): ServerAddress? = runCatching {
        DnsSrvResolver().resolve("_minecraft._tcp.$host")
    }.getOrNull()

    private fun queryJavaServerWithFallback(
        address: ServerAddress,
        enableLegacyProtocolFallback: Boolean,
    ): ServerStatus {
        return runCatching { queryJavaServer(address, CurrentStatusProtocol) }
            .recoverCatching { error ->
                if (enableLegacyProtocolFallback) queryJavaServer(address, LegacyStatusProtocol) else throw error
            }
            .getOrThrow()
    }

    private fun queryJavaServer(address: ServerAddress, protocolVersion: Int): ServerStatus {
        Socket().use { socket ->
            socket.soTimeout = DefaultTimeoutMillis
            val start = System.nanoTime()
            socket.connect(InetSocketAddress(address.host, address.port), DefaultTimeoutMillis)

            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            sendPacket(output, buildHandshakePacket(address, protocolVersion))
            sendPacket(output, byteArrayOf(0x00))

            val packetLength = input.readVarInt()
            require(packetLength > 0) { "Empty server response" }
            val packetId = input.readVarInt()
            require(packetId == 0) { "Invalid server response" }

            val responseJson = input.readUtf()
            val latencyMillis = ((System.nanoTime() - start) / 1_000_000.0).roundToInt().toLong()
            return parseStatus(responseJson, address, latencyMillis)
        }
    }

    private fun buildHandshakePacket(address: ServerAddress, protocolVersion: Int): ByteArray {
        val body = ByteArrayOutputStream()
        body.writeVarInt(0x00)
        body.writeVarInt(protocolVersion)
        body.writeUtf(address.host)
        body.writeShort(address.port)
        body.writeVarInt(1)
        return body.toByteArray()
    }

    private fun queryBedrockServer(address: ServerAddress): ServerStatus {
        DatagramSocket().use { socket ->
            socket.soTimeout = DefaultTimeoutMillis
            val start = System.nanoTime()
            val request = buildBedrockUnconnectedPing()
            val target = java.net.InetAddress.getByName(address.host)
            socket.send(DatagramPacket(request, request.size, target, address.port))
            val response = ByteArray(2048)
            val packet = DatagramPacket(response, response.size)
            socket.receive(packet)
            val latencyMillis = ((System.nanoTime() - start) / 1_000_000.0).roundToInt().toLong()
            return parseBedrockStatus(response.copyOf(packet.length), address, latencyMillis)
        }
    }

    private fun buildBedrockUnconnectedPing(): ByteArray {
        val magic = byteArrayOf(
            0x00, 0xff.toByte(), 0xff.toByte(), 0x00,
            0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(),
            0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(),
            0x12, 0x34, 0x56, 0x78,
        )
        val output = ByteArrayOutputStream()
        output.write(0x01)
        output.writeLong(System.currentTimeMillis())
        output.write(magic)
        output.writeLong(Random.nextLong())
        return output.toByteArray()
    }

    private fun parseBedrockStatus(payload: ByteArray, address: ServerAddress, latencyMillis: Long): ServerStatus {
        require(payload.isNotEmpty() && payload[0] == 0x1c.toByte()) { "Invalid server response" }
        var offset = 1 + 8 + 8 + 16
        require(payload.size >= offset + 2) { "Empty server response" }
        val length = ((payload[offset].toInt() and 0xFF) shl 8) or (payload[offset + 1].toInt() and 0xFF)
        offset += 2
        require(payload.size >= offset + length) { "Incomplete server response" }
        val parts = String(payload, offset, length, StandardCharsets.UTF_8).split(';')
        return ServerStatus(
            address = address,
            latencyMillis = latencyMillis,
            protocolName = parseMinecraftLegacyText(parts.getOrNull(3).orEmpty().ifBlank { "Unknown version" }),
            protocolVersion = parts.getOrNull(2)?.toIntOrNull() ?: -1,
            onlinePlayers = parts.getOrNull(4)?.toIntOrNull() ?: 0,
            maxPlayers = parts.getOrNull(5)?.toIntOrNull() ?: 0,
            motd = parseMinecraftLegacyText(parts.getOrNull(1).orEmpty().ifBlank { "Bedrock Server" }),
            samplePlayers = emptyList(),
            faviconBase64 = null,
        )
    }
    private fun sendPacket(output: DataOutputStream, payload: ByteArray) {
        output.writeVarInt(payload.size)
        output.write(payload)
        output.flush()
    }

    private fun parseStatus(json: String, address: ServerAddress, latencyMillis: Long): ServerStatus {
        val root = JsonParser(json).parseObject()
        val version = root.objectValue("version")
        val players = root.objectValue("players")
        val description = root["description"]
        val samplePlayers = players?.arrayValue("sample")
            ?.mapNotNull { it.asObjectOrNull()?.get("name")?.toMinecraftText() }
            .orEmpty()
        val favicon = root.stringValue("favicon")

        return ServerStatus(
            address = address,
            latencyMillis = latencyMillis,
            protocolName = version?.get("name")?.toMinecraftText()?.ifBlankText { MinecraftText("Unknown version") } ?: MinecraftText("Unknown version"),
            protocolVersion = version?.numberValue("protocol")?.toInt() ?: -1,
            onlinePlayers = players?.numberValue("online")?.toInt() ?: 0,
            maxPlayers = players?.numberValue("max")?.toInt() ?: 0,
            motd = description.toMinecraftText().ifBlankText { MinecraftText("No MOTD") },
            samplePlayers = samplePlayers,
            faviconBase64 = favicon?.substringAfter(',', missingDelimiterValue = favicon),
        )
    }

    private fun isIpLiteral(host: String): Boolean = host.all { it.isDigit() || it == '.' } || host.contains(':')
}

private class DnsSrvResolver {
    suspend fun resolve(name: String): ServerAddress? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { resolveWithPlatformDns(name) }.getOrNull()?.let { return it }
        }
        return resolveWithTcpDns(name)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun resolveWithPlatformDns(name: String): ServerAddress? = suspendCancellableCoroutine { continuation ->
        DnsResolver.getInstance().rawQuery(
            null,
            name,
            1,
            33,
            DnsResolver.FLAG_EMPTY,
            Dispatchers.IO.asExecutor(),
            null,
            object : DnsResolver.Callback<ByteArray> {
                override fun onAnswer(answer: ByteArray, rcode: Int) {
                    if (rcode == 0) {
                        continuation.resume(runCatching { parseResponse(answer) }.getOrNull())
                    } else {
                        continuation.resume(null)
                    }
                }

                override fun onError(error: DnsResolver.DnsException) {
                    continuation.resumeWithException(error)
                }
            },
        )
    }

    private fun resolveWithTcpDns(name: String): ServerAddress? {
        Socket().use { socket ->
            socket.soTimeout = DefaultTimeoutMillis
            socket.connect(InetSocketAddress(DnsServer, DnsPort), DefaultTimeoutMillis)
            val query = buildQuery(name)
            val output = DataOutputStream(socket.getOutputStream())
            output.writeShort(query.size)
            output.write(query)
            output.flush()

            val input = DataInputStream(socket.getInputStream())
            val length = input.readUnsignedShort()
            val response = ByteArray(length)
            input.readFully(response)
            return parseResponse(response)
        }
    }

    private fun buildQuery(name: String): ByteArray {
        val output = ByteArrayOutputStream()
        output.writeShort(Random.nextInt(0, 65535))
        output.writeShort(0x0100)
        output.writeShort(1)
        output.writeShort(0)
        output.writeShort(0)
        output.writeShort(0)
        output.writeDnsName(name)
        output.writeShort(33)
        output.writeShort(1)
        return output.toByteArray()
    }

    private fun parseResponse(bytes: ByteArray): ServerAddress? {
        if (bytes.size < 12) return null
        val answerCount = bytes.readUnsignedShort(6)
        var offset = 12
        offset = skipName(bytes, offset) + 4
        repeat(answerCount) {
            offset = skipName(bytes, offset)
            val type = bytes.readUnsignedShort(offset)
            offset += 2
            offset += 2
            offset += 4
            val dataLength = bytes.readUnsignedShort(offset)
            offset += 2
            if (type == 33 && offset + dataLength <= bytes.size) {
                val port = bytes.readUnsignedShort(offset + 4)
                val target = readName(bytes, offset + 6).first.trimEnd('.')
                if (target.isNotBlank() && port in 1..65535) return ServerAddress(target, port)
            }
            offset += dataLength
        }
        return null
    }

    private fun skipName(bytes: ByteArray, start: Int): Int {
        var offset = start
        while (offset < bytes.size) {
            val length = bytes[offset].toInt() and 0xFF
            offset++
            if (length == 0) return offset
            if ((length and 0xC0) == 0xC0) return offset + 1
            offset += length
        }
        return offset
    }

    private fun readName(bytes: ByteArray, start: Int): Pair<String, Int> {
        val labels = mutableListOf<String>()
        var offset = start
        var consumedOffset = -1
        while (offset < bytes.size) {
            val length = bytes[offset].toInt() and 0xFF
            offset++
            when {
                length == 0 -> return labels.joinToString(".") to if (consumedOffset == -1) offset else consumedOffset
                (length and 0xC0) == 0xC0 -> {
                    val pointer = ((length and 0x3F) shl 8) or (bytes[offset].toInt() and 0xFF)
                    if (consumedOffset == -1) consumedOffset = offset + 1
                    offset = pointer
                }
                else -> {
                    labels += String(bytes, offset, length, StandardCharsets.UTF_8)
                    offset += length
                }
            }
        }
        return labels.joinToString(".") to offset
    }
}

private fun DataInputStream.readVarInt(): Int {
    var value = 0
    var position = 0
    while (true) {
        val currentByte = readUnsignedByte()
        value = value or ((currentByte and 0x7F) shl position)
        if ((currentByte and 0x80) == 0) return value
        position += 7
        require(position < 32) { "VarInt is too long" }
    }
}

private fun DataInputStream.readUtf(): String {
    val length = readVarInt()
    val bytes = ByteArray(length)
    readFully(bytes)
    return String(bytes, StandardCharsets.UTF_8)
}

private fun DataOutputStream.writeVarInt(value: Int) {
    var remaining = value
    while (true) {
        if ((remaining and 0x7F.inv()) == 0) {
            writeByte(remaining)
            return
        }
        writeByte((remaining and 0x7F) or 0x80)
        remaining = remaining ushr 7
    }
}

private fun ByteArrayOutputStream.writeVarInt(value: Int) {
    var remaining = value
    while (true) {
        if ((remaining and 0x7F.inv()) == 0) {
            write(remaining)
            return
        }
        write((remaining and 0x7F) or 0x80)
        remaining = remaining ushr 7
    }
}

private fun ByteArrayOutputStream.writeUtf(value: String) {
    val bytes = value.toByteArray(StandardCharsets.UTF_8)
    writeVarInt(bytes.size)
    write(bytes)
}

private fun ByteArrayOutputStream.writeShort(value: Int) {
    write((value ushr 8) and 0xFF)
    write(value and 0xFF)
}

private fun ByteArrayOutputStream.writeDnsName(name: String) {
    name.split('.').forEach { label ->
        val bytes = label.toByteArray(StandardCharsets.UTF_8)
        write(bytes.size)
        write(bytes)
    }
    write(0)
}

private fun ByteArray.readUnsignedShort(offset: Int): Int =
    ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)


private inline fun MinecraftText.ifBlankText(defaultValue: () -> MinecraftText): MinecraftText =
    if (plainText.isBlank()) defaultValue() else this







private fun ByteArrayOutputStream.writeLong(value: Long) {
    for (shift in 56 downTo 0 step 8) write(((value ushr shift) and 0xFF).toInt())
}





