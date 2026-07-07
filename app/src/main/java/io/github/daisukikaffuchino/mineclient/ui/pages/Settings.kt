package io.github.daisukikaffuchino.mineclient.ui.pages

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.ui.ServerStatusUiState

@Composable
fun SettingsPage(
    state: ServerStatusUiState,
    onAutoRefreshServersChange: (Boolean) -> Unit,
    onLegacyProtocolFallbackChange: (Boolean) -> Unit,
    onMaxConcurrentRequestsChange: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SettingsGroup(title = stringResource(R.string.settings_connection)) {
                SettingSwitchRow(
                    title = stringResource(R.string.settings_auto_refresh_title),
                    description = stringResource(R.string.settings_auto_refresh_desc),
                    checked = state.settings.autoRefreshServers,
                    onCheckedChange = onAutoRefreshServersChange,
                )
                SettingSwitchRow(
                    title = stringResource(R.string.settings_legacy_protocol_title),
                    description = stringResource(R.string.settings_legacy_protocol_desc),
                    checked = state.settings.enableLegacyProtocolFallback,
                    onCheckedChange = onLegacyProtocolFallbackChange,
                )
                SettingStepperRow(
                    title = stringResource(R.string.settings_max_concurrent_title),
                    description = stringResource(R.string.settings_max_concurrent_desc),
                    value = state.settings.maxConcurrentRequests,
                    onValueChange = onMaxConcurrentRequestsChange,
                )
            }
        }
        item {
            val context = LocalContext.current
            SettingsGroup(title = stringResource(R.string.settings_about)) {
                SettingTextRow(
                    title = stringResource(R.string.settings_version_title),
                    description = "1.0"
                )
                SettingTextRow(
                    title = stringResource(R.string.settings_github_title),
                    description = "github.com/example",
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://github.com/example".toUri())
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingStepperRow(
    title: String,
    description: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onValueChange(value - 1) },
                enabled = value > 1
            ) { Text("-") }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { onValueChange(value + 1) },
                enabled = value < 16
            ) { Text("+") }
        }
    }
}

@Composable
private fun SettingTextRow(title: String, description: String, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}