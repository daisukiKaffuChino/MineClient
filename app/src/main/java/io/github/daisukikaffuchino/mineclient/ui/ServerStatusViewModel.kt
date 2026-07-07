package io.github.daisukikaffuchino.mineclient.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.daisukikaffuchino.mineclient.data.AppSettings
import io.github.daisukikaffuchino.mineclient.data.MinecraftPingClient
import io.github.daisukikaffuchino.mineclient.data.ServerStatus
import io.github.daisukikaffuchino.mineclient.data.ServerStore
import io.github.daisukikaffuchino.mineclient.data.toServerEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration.Companion.milliseconds

private const val DefaultJavaPort = 25565
private const val DefaultBedrockPort = 19132
private const val AutoRefreshIntervalMillis = 30_000L

enum class ServerEdition { Java, Bedrock }

data class ServerEntry(
    val id: Long,
    val name: String,
    val address: String,
    val port: Int?,
    val edition: ServerEdition = ServerEdition.Java,
    val isLoading: Boolean = false,
    val status: ServerStatus? = null,
    val errorMessage: String? = null,
)

data class ServerFormState(
    val name: String = "",
    val address: String = "",
    val port: String = "",
    val edition: ServerEdition = ServerEdition.Java,
) {
    val canSubmit: Boolean
        get() = name.isNotBlank() && address.isNotBlank() && (port.isBlank() || port.toIntOrNull()?.let { it in 1..65535 } == true)
}

enum class AppPage { Home, Settings }

data class ServerStatusUiState(
    val servers: List<ServerEntry> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    val form: ServerFormState = ServerFormState(),
    val selectedServerId: Long? = null,
    val selectedPage: AppPage = AppPage.Home,
    val isSettingsLoaded: Boolean = false,
    val settings: AppSettings = AppSettings(),
)

class ServerStatusViewModel(
    private val client: MinecraftPingClient = MinecraftPingClient(),
    private val serverStore: ServerStore? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ServerStatusUiState())
    val state: StateFlow<ServerStatusUiState> = mutableState.asStateFlow()
    private var nextServerId = 1L
    private var refreshSemaphore = Semaphore(mutableState.value.settings.maxConcurrentRequests)

    init {
        viewModelScope.launch {
            restoreSavedServers()
            restoreSettings()
            refreshAllServers()
        }
        viewModelScope.launch {
            while (true) {
                delay(AutoRefreshIntervalMillis.milliseconds)
                if (mutableState.value.selectedPage == AppPage.Home && mutableState.value.settings.autoRefreshServers) {
                    refreshAllServers()
                }
            }
        }
    }

    fun selectPage(page: AppPage) {
        mutableState.update { it.copy(selectedPage = page) }
    }

    fun updateAutoRefreshServers(enabled: Boolean) {
        mutableState.update { it.copy(settings = it.settings.copy(autoRefreshServers = enabled)) }
        persistSettings()
    }

    fun updateDynamicColorsEnabled(enabled: Boolean) {
        mutableState.update { it.copy(settings = it.settings.copy(enableDynamicColors = enabled)) }
        persistSettings()
    }

    fun completeWelcome() {
        mutableState.update { it.copy(settings = it.settings.copy(hasSeenWelcome = true)) }
        persistSettings()
    }

    fun updateLegacyProtocolFallback(enabled: Boolean) {
        mutableState.update { it.copy(settings = it.settings.copy(enableLegacyProtocolFallback = enabled)) }
        persistSettings()
    }

    fun openAddDialog() {
        mutableState.update { it.copy(isAddDialogOpen = true, form = ServerFormState()) }
    }

    fun closeAddDialog() {
        mutableState.update { it.copy(isAddDialogOpen = false, form = ServerFormState()) }
    }

    fun updateServerName(value: String) {
        mutableState.update { it.copy(form = it.form.copy(name = value)) }
    }

    fun updateServerAddress(value: String) {
        mutableState.update { it.copy(form = it.form.copy(address = value)) }
    }

    fun updateServerEdition(value: ServerEdition) {
        mutableState.update { it.copy(form = it.form.copy(edition = value)) }
    }

    fun updateMaxConcurrentRequests(value: Int) {
        val boundedValue = value.coerceIn(1, 16)
        mutableState.update { it.copy(settings = it.settings.copy(maxConcurrentRequests = boundedValue)) }
        refreshSemaphore = Semaphore(boundedValue)
        persistSettings()
    }

    fun updateServerPort(value: String) {
        val filtered = value.filter(Char::isDigit).take(5)
        mutableState.update { it.copy(form = it.form.copy(port = filtered)) }
    }

    fun addServer() {
        val form = mutableState.value.form
        if (!form.canSubmit) return

        val entry = ServerEntry(
            id = nextServerId++,
            name = form.name.trim(),
            address = form.address.trim(),
            port = form.port.toIntOrNull(),
            edition = form.edition,
        )
        mutableState.update {
            it.copy(
                servers = it.servers + entry,
                isAddDialogOpen = false,
                form = ServerFormState(),
            )
        }
        persistServers()
        refreshServer(entry.id)
    }

    fun deleteServer(serverId: Long) {
        mutableState.update { state ->
            state.copy(
                servers = state.servers.filterNot { it.id == serverId },
                selectedServerId = state.selectedServerId.takeUnless { it == serverId },
            )
        }
        persistServers()
    }

    fun refreshAllServers() {
        mutableState.value.servers
            .filterNot { it.isLoading }
            .forEach { refreshServer(it.id) }
    }

    fun refreshServer(serverId: Long) {
        val entry = mutableState.value.servers.firstOrNull { it.id == serverId } ?: return
        if (entry.isLoading) return
        viewModelScope.launch {
            updateServer(serverId) { it.copy(isLoading = true, errorMessage = null) }
            val result = refreshSemaphore.withPermit {
                client.query(
                    input = entry.queryAddress(),
                    edition = entry.edition,
                    enableLegacyProtocolFallback = mutableState.value.settings.enableLegacyProtocolFallback,
                )
            }
            mutableState.update { state ->
                state.copy(
                    servers = state.servers.map { current ->
                        if (current.id != serverId) {
                            current
                        } else {
                            result.fold(
                                onSuccess = { current.copy(isLoading = false, status = it, errorMessage = null) },
                                onFailure = {
                                    current.copy(
                                        isLoading = false,
                                        errorMessage = it.localizedMessage ?: "Query failed",
                                    )
                                },
                            )
                        }
                    }
                )
            }
        }
    }
    fun selectServer(serverId: Long) {
        mutableState.update { it.copy(selectedServerId = serverId) }
    }

    fun clearSelectedServer() {
        mutableState.update { it.copy(selectedServerId = null) }
    }

    private suspend fun restoreSavedServers() {
        val restoredServers = serverStore?.servers?.first().orEmpty().map { it.toServerEntry() }
        if (restoredServers.isEmpty()) return
        nextServerId = (restoredServers.maxOfOrNull { it.id } ?: 0L) + 1L
        mutableState.update { it.copy(servers = restoredServers) }
    }

    private suspend fun restoreSettings() {
        val settings = serverStore?.settings?.first()
        if (settings == null) {
            mutableState.update { it.copy(isSettingsLoaded = true) }
            return
        }
        mutableState.update { it.copy(settings = settings, isSettingsLoaded = true) }
        refreshSemaphore = Semaphore(settings.maxConcurrentRequests)
    }

    private fun persistSettings() {
        val store = serverStore ?: return
        val settings = mutableState.value.settings
        viewModelScope.launch {
            store.saveSettings(settings)
        }
    }

    private fun persistServers() {
        val store = serverStore ?: return
        val servers = mutableState.value.servers
        viewModelScope.launch {
            store.saveServers(servers)
        }
    }

    private fun updateServer(serverId: Long, transform: (ServerEntry) -> ServerEntry) {
        mutableState.update { state ->
            state.copy(
                servers = state.servers.map { if (it.id == serverId) transform(it) else it }
            )
        }
    }
}

fun ServerEntry.queryAddress(): String {
    val defaultPort = when (edition) {
        ServerEdition.Java -> DefaultJavaPort
        ServerEdition.Bedrock -> DefaultBedrockPort
    }
    return if (port == null || port == defaultPort) address else "$address:$port"
}






