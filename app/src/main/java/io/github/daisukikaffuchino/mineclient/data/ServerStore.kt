package io.github.daisukikaffuchino.mineclient.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.daisukikaffuchino.mineclient.ui.ServerEdition
import io.github.daisukikaffuchino.mineclient.ui.ServerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.serverDataStore by preferencesDataStore(name = "servers")
private val SavedServersKey = stringPreferencesKey("saved_servers_json")
private val LegacyProtocolFallbackKey = booleanPreferencesKey("legacy_protocol_fallback")
private val MaxConcurrentRequestsKey = stringPreferencesKey("max_concurrent_requests")
private val AutoRefreshServersKey = booleanPreferencesKey("auto_refresh_servers")
private val DynamicColorsEnabledKey = booleanPreferencesKey("dynamic_colors_enabled")
private val HasSeenWelcomeKey = booleanPreferencesKey("has_seen_welcome")

data class AppSettings(
    val enableLegacyProtocolFallback: Boolean = false,
    val maxConcurrentRequests: Int = 4,
    val autoRefreshServers: Boolean = true,
    val enableDynamicColors: Boolean = false,
    val hasSeenWelcome: Boolean = false,
)

data class SavedServer(
    val id: Long,
    val name: String,
    val address: String,
    val port: Int?,
    val edition: ServerEdition = ServerEdition.Java,
)

class ServerStore(context: Context) {
    private val dataStore = context.applicationContext.serverDataStore

    private val preferencesFlow = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }

    val servers: Flow<List<SavedServer>> = preferencesFlow
        .map { preferences -> preferences[SavedServersKey].orEmpty().decodeSavedServers() }

    val settings: Flow<AppSettings> = preferencesFlow
        .map { preferences ->
            AppSettings(
                enableLegacyProtocolFallback = preferences[LegacyProtocolFallbackKey] ?: false,
                maxConcurrentRequests = preferences[MaxConcurrentRequestsKey]
                    ?.toIntOrNull()
                    ?.coerceIn(1, 16) ?: 4,
                autoRefreshServers = preferences[AutoRefreshServersKey] ?: true,
                enableDynamicColors = preferences[DynamicColorsEnabledKey] ?: false,
                hasSeenWelcome = preferences[HasSeenWelcomeKey] ?: false,
            )
        }

    suspend fun saveSettings(settings: AppSettings) {
        dataStore.edit { preferences ->
            preferences[LegacyProtocolFallbackKey] = settings.enableLegacyProtocolFallback
            preferences[MaxConcurrentRequestsKey] = settings.maxConcurrentRequests.toString()
            preferences[AutoRefreshServersKey] = settings.autoRefreshServers
            preferences[DynamicColorsEnabledKey] = settings.enableDynamicColors
            preferences[HasSeenWelcomeKey] = settings.hasSeenWelcome
        }
    }

    suspend fun saveServers(servers: List<ServerEntry>) {
        dataStore.edit { preferences ->
            preferences[SavedServersKey] = servers
                .map { SavedServer(it.id, it.name, it.address, it.port, it.edition) }
                .encodeSavedServers()
        }
    }
}

fun SavedServer.toServerEntry(): ServerEntry = ServerEntry(
    id = id,
    name = name,
    address = address,
    port = port,
    edition = edition,
)

private fun List<SavedServer>.encodeSavedServers(): String = buildString {
    append('[')
    this@encodeSavedServers.forEachIndexed { index, server ->
        if (index > 0) append(',')
        append('{')
        append("\"id\":").append(server.id).append(',')
        append("\"name\":\"").append(server.name.escapeJson()).append("\",")
        append("\"address\":\"").append(server.address.escapeJson()).append("\",")
        append("\"port\":").append(server.port?.toString() ?: "null").append(',')
        append("\"edition\":\"").append(server.edition.name).append("\"")
        append('}')
    }
    append(']')
}

private fun String.decodeSavedServers(): List<SavedServer> {
    if (isBlank()) return emptyList()
    return runCatching {
        val root = JsonParser(this).parseArrayValue()
        root.values.mapNotNull { value ->
            val item = value.asObjectOrNull() ?: return@mapNotNull null
            SavedServer(
                id = item.numberValue("id")?.toLong() ?: return@mapNotNull null,
                name = item.stringValue("name") ?: return@mapNotNull null,
                address = item.stringValue("address") ?: return@mapNotNull null,
                port = item.numberValue("port")?.toInt(),
                edition = item.stringValue("edition")
                    ?.let { runCatching { ServerEdition.valueOf(it) }.getOrNull() }
                    ?: ServerEdition.Java,
            )
        }
    }.getOrDefault(emptyList())
}

private fun String.escapeJson(): String = buildString {
    this@escapeJson.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
}
