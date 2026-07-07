package io.github.daisukikaffuchino.mineclient

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.daisukikaffuchino.mineclient.data.AppSettings
import io.github.daisukikaffuchino.mineclient.data.MinecraftText
import io.github.daisukikaffuchino.mineclient.data.MinecraftTextSpan
import io.github.daisukikaffuchino.mineclient.data.ServerAddress
import io.github.daisukikaffuchino.mineclient.data.ServerStatus
import io.github.daisukikaffuchino.mineclient.data.ServerStore
import io.github.daisukikaffuchino.mineclient.data.parseMinecraftLegacyText
import io.github.daisukikaffuchino.mineclient.ui.AppPage
import io.github.daisukikaffuchino.mineclient.ui.ServerEdition
import io.github.daisukikaffuchino.mineclient.ui.ServerEntry
import io.github.daisukikaffuchino.mineclient.ui.ServerFormState
import io.github.daisukikaffuchino.mineclient.ui.ServerStatusUiState
import io.github.daisukikaffuchino.mineclient.ui.ServerStatusViewModel
import io.github.daisukikaffuchino.mineclient.ui.navigation.MomoDestination
import io.github.daisukikaffuchino.mineclient.ui.navigation.MomoScreen
import io.github.daisukikaffuchino.mineclient.ui.navigation.TopLevelBackStack
import io.github.daisukikaffuchino.mineclient.ui.pages.HomePage
import io.github.daisukikaffuchino.mineclient.ui.pages.SettingsPage
import io.github.daisukikaffuchino.mineclient.ui.pages.WelcomePage
import io.github.daisukikaffuchino.mineclient.ui.queryAddress
import io.github.daisukikaffuchino.mineclient.ui.theme.MineClientTheme
import io.github.daisukikaffuchino.mineclient.utils.ServerIcon
import io.github.daisukikaffuchino.mineclient.utils.latencyColor
import io.github.daisukikaffuchino.mineclient.utils.shareServer
import io.github.daisukikaffuchino.mineclient.utils.toSpanStyle

class MainActivity : ComponentActivity() {
    private val viewModel: ServerStatusViewModel by lazy {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ServerStatusViewModel(serverStore = ServerStore(applicationContext)) as T
            }
        }
        ViewModelProvider(this, factory)[ServerStatusViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MineClientTheme {
                val state by viewModel.state.collectAsState()
                ServerStatusApp(
                    state = state,
                    onAddClick = viewModel::openAddDialog,
                    onDismissAddDialog = viewModel::closeAddDialog,
                    onNameChange = viewModel::updateServerName,
                    onAddressChange = viewModel::updateServerAddress,
                    onPortChange = viewModel::updateServerPort,
                    onEditionChange = viewModel::updateServerEdition,
                    onSubmitServer = viewModel::addServer,
                    onServerClick = viewModel::selectServer,
                    onDeleteServer = viewModel::deleteServer,
                    onPageSelected = viewModel::selectPage,
                    onWelcomeDone = viewModel::completeWelcome,
                    onAutoRefreshServersChange = viewModel::updateAutoRefreshServers,
                    onLegacyProtocolFallbackChange = viewModel::updateLegacyProtocolFallback,
                    onMaxConcurrentRequestsChange = viewModel::updateMaxConcurrentRequests,
                    onDismissDetails = viewModel::clearSelectedServer,
                )
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ServerStatusApp(
    state: ServerStatusUiState,
    onAddClick: () -> Unit,
    onDismissAddDialog: () -> Unit,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onEditionChange: (ServerEdition) -> Unit,
    onSubmitServer: () -> Unit,
    onServerClick: (Long) -> Unit,
    onDeleteServer: (Long) -> Unit,
    onPageSelected: (AppPage) -> Unit,
    onWelcomeDone: () -> Unit,
    onAutoRefreshServersChange: (Boolean) -> Unit,
    onLegacyProtocolFallbackChange: (Boolean) -> Unit,
    onMaxConcurrentRequestsChange: (Int) -> Unit,
    onDismissDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isSettingsLoaded) {
        Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        return
    }

    if (!state.settings.hasSeenWelcome) {
        WelcomePage(onStartClick = onWelcomeDone, modifier = modifier)
        return
    }

    val destinations = MomoDestination.entries
    val mainBackStack = remember { TopLevelBackStack<MomoScreen>(MomoScreen.Home) }
    val pagerState = rememberPagerState(
        initialPage = destinations.indexOfFirst { it.route == mainBackStack.topLevelKey }
            .coerceAtLeast(0),
        pageCount = { destinations.size },
    )
    val selectedServer = state.servers.firstOrNull { it.id == state.selectedServerId }

    LaunchedEffect(state.selectedPage) {
        val targetScreen = state.selectedPage.toMomoScreen()
        if (mainBackStack.topLevelKey != targetScreen) {
            mainBackStack.addTopLevel(targetScreen)
        }
    }
    LaunchedEffect(mainBackStack.topLevelKey) {
        val targetPage = destinations.indexOfFirst { it.route == mainBackStack.topLevelKey }
        if (targetPage >= 0 && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
        onPageSelected(mainBackStack.topLevelKey.toAppPage())
    }
    LaunchedEffect(pagerState.currentPage) {
        val destination = destinations.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (mainBackStack.topLevelKey != destination.route) {
            mainBackStack.addTopLevel(destination.route)
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            destinations.forEach { destination ->
                val selected = destination.route == mainBackStack.topLevelKey
                item(
                    icon = {
                        Crossfade(selected, label = "navigationIcon") { isSelected ->
                            Icon(
                                painter = painterResource(if (isSelected) destination.selectedIcon else destination.icon),
                                contentDescription = null,
                            )
                        }
                    },
                    label = { Text(stringResource(destination.label)) },
                    selected = selected,
                    onClick = {
                        mainBackStack.addTopLevel(destination.route)
                        onPageSelected(destination.route.toAppPage())
                    },
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = mainBackStack.topLevelKey == MomoScreen.Home,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    FloatingActionButton(onClick = onAddClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.add_server),
                        )
                    }
                }
            },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) { pageIndex ->
                when (destinations[pageIndex].route) {
                    MomoScreen.Home -> HomePage(
                        state = state,
                        onAddClick = onAddClick,
                        onServerClick = onServerClick,
                    )

                    MomoScreen.Settings -> SettingsPage(
                        state = state,
                        onAutoRefreshServersChange = onAutoRefreshServersChange,
                        onLegacyProtocolFallbackChange = onLegacyProtocolFallbackChange,
                        onMaxConcurrentRequestsChange = onMaxConcurrentRequestsChange,
                    )
                }
            }
        }
    }
    if (state.isAddDialogOpen) {
        AddServerDialog(
            form = state.form,
            onNameChange = onNameChange,
            onAddressChange = onAddressChange,
            onPortChange = onPortChange,
            onEditionChange = onEditionChange,
            onDismiss = onDismissAddDialog,
            onSubmit = onSubmitServer,
        )
    }

    if (selectedServer != null) {
        ModalBottomSheet(onDismissRequest = onDismissDetails) {
            ServerDetailsSheet(
                server = selectedServer,
                onDelete = {
                    onDeleteServer(selectedServer.id)
                    onDismissDetails()
                },
            )
        }
    }
}

private fun AppPage.toMomoScreen(): MomoScreen = when (this) {
    AppPage.Home -> MomoScreen.Home
    AppPage.Settings -> MomoScreen.Settings
}

private fun MomoScreen.toAppPage(): AppPage = when (this) {
    MomoScreen.Home -> AppPage.Home
    MomoScreen.Settings -> AppPage.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddServerDialog(
    form: ServerFormState,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onEditionChange: (ServerEdition) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    var isEditionExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_server)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = isEditionExpanded,
                    onExpandedChange = { isEditionExpanded = it },
                ) {
                    OutlinedTextField(
                        value = form.edition.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.server_type)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEditionExpanded) },
                        modifier = Modifier
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                enabled = true
                            )
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = isEditionExpanded,
                        onDismissRequest = { isEditionExpanded = false },
                    ) {
                        ServerEdition.entries.forEach { edition ->
                            DropdownMenuItem(
                                text = { Text(edition.displayName()) },
                                onClick = {
                                    onEditionChange(edition)
                                    isEditionExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = form.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.server_name_required)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.address,
                    onValueChange = onAddressChange,
                    label = { Text(stringResource(R.string.server_address_required)) },
                    placeholder = { Text(stringResource(if (form.edition == ServerEdition.Java) R.string.server_address_java_hint else R.string.server_address_bedrock_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.port,
                    onValueChange = onPortChange,
                    label = { Text(stringResource(R.string.server_port)) },
                    placeholder = { Text(stringResource(if (form.edition == ServerEdition.Java) R.string.server_port_java_hint else R.string.server_port_bedrock_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = form.port.isNotBlank() && form.port.toIntOrNull()
                        ?.let { it in 1..65535 } != true,
                    supportingText = { Text(stringResource(R.string.server_port_supporting)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = form.canSubmit
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}





@Composable
private fun ServerEdition.displayName(): String = when (this) {
    ServerEdition.Java -> stringResource(R.string.edition_java)
    ServerEdition.Bedrock -> stringResource(R.string.edition_bedrock)
}



@Composable
private fun ServerDetailsSheet(server: ServerEntry, onDelete: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ServerIcon(server.status?.faviconBase64)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    server.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(server.queryAddress(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            server.status != null -> StatusDetails(
                status = server.status,
                isRefreshing = server.isLoading
            )

            server.errorMessage != null -> ErrorDetails(server.errorMessage)
            server.isLoading -> Text(stringResource(R.string.status_first_query))
            else -> Text(stringResource(R.string.status_no_result))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.action_delete))
            }
            Button(
                onClick = { context.shareServer(server) },
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.action_share))
            }
        }
    }
}


@Composable
private fun ErrorDetails(message: String) {
    Text(
        stringResource(R.string.error_query_failed_format, message),
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun StatusDetails(status: ServerStatus, isRefreshing: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(status.motd.toAnnotatedString(), style = MaterialTheme.typography.bodyLarge)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoChip(
                stringResource(R.string.label_latency),
                if (isRefreshing) stringResource(R.string.status_refreshing) else "${status.latencyMillis} ms",
                valueColor = if (isRefreshing) Color.Unspecified else latencyColor(status.latencyMillis),
            )
            InfoChip(
                stringResource(R.string.label_players),
                if (isRefreshing) stringResource(R.string.status_refreshing) else "${status.onlinePlayers}/${status.maxPlayers}"
            )
            InfoChip(
                stringResource(R.string.label_version),
                if (isRefreshing) MinecraftText(stringResource(R.string.status_refreshing)) else status.protocolName
            )
            if (status.protocolVersion >= 0) InfoChip(
                stringResource(R.string.label_protocol),
                if (isRefreshing) stringResource(R.string.status_refreshing) else status.protocolVersion.toString()
            )
            InfoChip(
                stringResource(R.string.label_resolved_address),
                "${status.address.host}:${status.address.port}"
            )
        }
        if (status.samplePlayers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.sample_players_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(status.samplePlayers.joinToAnnotatedString(", "))
            }
        }
    }
}



private fun List<MinecraftText>.joinToAnnotatedString(separator: String): AnnotatedString =
    buildAnnotatedString {
        this@joinToAnnotatedString.forEachIndexed { index, item ->
            if (index > 0) append(separator)
            val start = length
            append(item.plainText)
            item.spans.forEach { span ->
                addStyle(span.toSpanStyle(), start + span.start, start + span.end)
            }
        }
    }

private fun MinecraftText.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    append(plainText)
    spans.forEach { span ->
        addStyle(span.toSpanStyle(), span.start, span.end)
    }
}

@Composable
private fun InfoChip(label: String, value: MinecraftText, valueColor: Color = Color.Unspecified) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toAnnotatedString(), fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold, color = valueColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ServerStatusPreview() {
    MineClientTheme {
        ServerStatusApp(
            state = ServerStatusUiState(
                isSettingsLoaded = true,
                settings = AppSettings(hasSeenWelcome = true),
                servers = listOf(
                    ServerEntry(
                        id = 1L,
                        name = "Hypixel",
                        address = "mc.hypixel.net",
                        port = null,
                        status = ServerStatus(
                            address = ServerAddress("mc.hypixel.net"),
                            latencyMillis = 42,
                            protocolName = parseMinecraftLegacyText("§e1.8.x-1.21.x"),
                            protocolVersion = 764,
                            onlinePlayers = 42100,
                            maxPlayers = 100000,
                            motd = parseMinecraftLegacyText("§bHypixel §eNetwork"),
                            samplePlayers = listOf(
                                parseMinecraftLegacyText("§aSteve"),
                                parseMinecraftLegacyText("§bAlex")
                            ),
                            faviconBase64 = null,
                        ),
                    )
                ),
            ),
            onAddClick = {},
            onDismissAddDialog = {},
            onNameChange = {},
            onAddressChange = {},
            onPortChange = {},
            onEditionChange = {},
            onSubmitServer = {},
            onServerClick = {},
            onDeleteServer = {},
            onPageSelected = {},
            onWelcomeDone = {},
            onAutoRefreshServersChange = {},
            onLegacyProtocolFallbackChange = {},
            onMaxConcurrentRequestsChange = {},
            onDismissDetails = {},
        )
    }
}




























