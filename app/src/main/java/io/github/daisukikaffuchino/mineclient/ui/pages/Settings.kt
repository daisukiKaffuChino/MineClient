package io.github.daisukikaffuchino.mineclient.ui.pages

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.ui.ServerStatusUiState
import io.github.daisukikaffuchino.mineclient.ui.components.ListItemContainer
import io.github.daisukikaffuchino.mineclient.ui.components.SettingsItem
import io.github.daisukikaffuchino.mineclient.ui.components.SwitchSettingsItem
import io.github.daisukikaffuchino.mineclient.ui.components.segmentedGroup
import io.github.daisukikaffuchino.mineclient.ui.components.segmentedSection

@Composable
fun SettingsPage(
    state: ServerStatusUiState,
    onAutoRefreshServersChange: (Boolean) -> Unit,
    onLegacyProtocolFallbackChange: (Boolean) -> Unit,
    onDynamicColorsChange: (Boolean) -> Unit,
    onMaxConcurrentRequestsChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var clickCount by remember { mutableIntStateOf(0) }
    var showDnsDialog by remember { mutableStateOf(false) }
    var dnsServers by remember { mutableStateOf<List<String>>(emptyList()) }

    ListItemContainer(Modifier.fillMaxSize()) {
        segmentedSection(R.string.settings_connection) {
            segmentedGroup {
                SwitchSettingsItem(
                    leadingIconRes = R.drawable.ic_sync,
                    title = stringResource(R.string.settings_auto_refresh_title),
                    description = stringResource(R.string.settings_auto_refresh_desc),
                    checked = state.settings.autoRefreshServers,
                    onCheckedChange = onAutoRefreshServersChange,
                )
                SwitchSettingsItem(
                    leadingIconRes = R.drawable.ic_rebase,
                    title = stringResource(R.string.settings_legacy_protocol_title),
                    description = stringResource(R.string.settings_legacy_protocol_desc),
                    checked = state.settings.enableLegacyProtocolFallback,
                    onCheckedChange = onLegacyProtocolFallbackChange,
                )
                SettingsItem(
                    leadingIconRes = R.drawable.ic_router,
                    title = stringResource(R.string.settings_max_concurrent_title),
                    description = stringResource(R.string.settings_max_concurrent_desc),
                    onClick = {},
                    trailingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedIconButton(
                                onClick = {
                                    onMaxConcurrentRequestsChange(state.settings.maxConcurrentRequests - 1)
                                },
                                enabled = state.settings.maxConcurrentRequests > 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_remove),
                                    contentDescription = null
                                )
                            }
                            Text(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                text=state.settings.maxConcurrentRequests.toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                            OutlinedIconButton(
                                onClick = {
                                    onMaxConcurrentRequestsChange(state.settings.maxConcurrentRequests + 1)
                                },
                                enabled = state.settings.maxConcurrentRequests < 16,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
                SettingsItem(
                    leadingIconRes = R.drawable.ic_dns,
                    title = stringResource(R.string.settings_dns_query_title),
                    description = stringResource(R.string.settings_dns_query_desc),
                    onClick = {
                        dnsServers = try {
                            queryDnsServers(context)
                        } catch (_: Exception) {
                            emptyList()
                        }
                        if (dnsServers.isEmpty()) {
                            dnsServers = listOf("8.8.8.8", "8.8.4.4")
                        }
                        showDnsDialog = true
                    }
                )
            }
        }
        segmentedSection(R.string.settings_appearance) {
            segmentedGroup {
                SwitchSettingsItem(
                    leadingIconRes = R.drawable.ic_palette,
                    title = stringResource(R.string.settings_dynamic_colors_title),
                    description = stringResource(R.string.settings_dynamic_colors_desc),
                    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    checked = state.settings.enableDynamicColors,
                    onCheckedChange = onDynamicColorsChange,
                )
            }
        }
        segmentedSection(R.string.settings_about) {
            segmentedGroup {
                SettingsItem(
                    leadingIconRes = R.drawable.ic_info,
                    title = stringResource(R.string.settings_version_title),
                    description = context.appVersion(),
                    onClick = {
                        clickCount++
                        if (clickCount == 5) {
                            Toast.makeText(context, "Herobrine", Toast.LENGTH_SHORT).show()
                            clickCount = 0
                        }
                    }
                )
                SettingsItem(
                    leadingIconRes = R.drawable.ic_github,
                    title = stringResource(R.string.settings_open_source_title),
                    description = "github.com/daisukiKaffuChino/MineClient\nGPLv3 Licensed",
                    onClick = {
                        uriHandler.openUri("https://github.com/daisukiKaffuChino/MineClient")
                    }
                )
            }
        }
        item{
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Made with ♥ by daisukiKaffuChino",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    if (showDnsDialog) {
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text(stringResource(R.string.dns_dialog_title)) },
            text = {
                if (dnsServers.isEmpty()) {
                    Text(stringResource(R.string.dns_dialog_empty))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        dnsServers.forEach { adder ->
                            Text(adder)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDnsDialog = false }) {
                    Text(stringResource(R.string.action_ok))
                }
            }
        )
    }
}

private fun queryDnsServers(context: Context): List<String> {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return emptyList()

    val network = cm.activeNetwork ?: return emptyList()

    val linkProperties = cm.getLinkProperties(network)
        ?: return emptyList()

    return linkProperties.dnsServers
        .mapNotNull { it.hostAddress }
        .distinct()
}

private fun Context.appVersion(): String {
    val pkgInfo = this.packageManager.getPackageInfo(this.packageName, 0)
    val verName = pkgInfo.versionName
    val verCode = pkgInfo.longVersionCode.toInt()
    return "$verName ($verCode)"
}
