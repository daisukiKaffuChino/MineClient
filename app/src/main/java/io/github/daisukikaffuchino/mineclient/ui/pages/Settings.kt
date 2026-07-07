package io.github.daisukikaffuchino.mineclient.ui.pages

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.ui.ServerStatusUiState
import io.github.daisukikaffuchino.mineclient.ui.components.ListItemContainer
import io.github.daisukikaffuchino.mineclient.ui.components.SettingsItem
import io.github.daisukikaffuchino.mineclient.ui.components.SwitchSettingsItem
import io.github.daisukikaffuchino.mineclient.ui.components.segmentedGroup
import io.github.daisukikaffuchino.mineclient.ui.components.segmentedSection
import kotlinx.coroutines.launch

@Composable
fun SettingsPage(
    state: ServerStatusUiState,
    onAutoRefreshServersChange: (Boolean) -> Unit,
    onLegacyProtocolFallbackChange: (Boolean) -> Unit,
    onMaxConcurrentRequestsChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var clickCount by remember { mutableIntStateOf(0) }

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
    }
}

private fun Context.appVersion(): String {
    val pkgInfo = this.packageManager.getPackageInfo(this.packageName, 0)
    val verName = pkgInfo.versionName
    val verCode = pkgInfo.longVersionCode.toInt()
    return "$verName ($verCode)"
}
