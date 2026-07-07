package io.github.daisukikaffuchino.mineclient.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.ui.ServerEdition
import io.github.daisukikaffuchino.mineclient.ui.ServerEntry
import io.github.daisukikaffuchino.mineclient.ui.ServerStatusUiState
import io.github.daisukikaffuchino.mineclient.ui.queryAddress
import io.github.daisukikaffuchino.mineclient.utils.ServerIcon
import io.github.daisukikaffuchino.mineclient.utils.ServerListMeta

@Composable
fun HomePage(
    state: ServerStatusUiState,
    onAddClick: () -> Unit,
    onServerClick: (Long) -> Unit,
) {
    if (state.servers.isEmpty()) {
        EmptyServerList(
            modifier = Modifier.fillMaxSize(),
            onAddClick = onAddClick,
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.servers, key = { it.id }) { server ->
                ServerListItem(
                    server = server,
                    modifier = Modifier.animateItem(),
                    onClick = { onServerClick(server.id) },
                )
            }
        }
    }
}

@Composable
private fun EmptyServerList(modifier: Modifier = Modifier, onAddClick: () -> Unit) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.empty_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAddClick) {
                Text(stringResource(R.string.add_server))
            }
        }
    }
}

@Composable
private fun ServerListItem(
    server: ServerEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServerIcon(server.status?.faviconBase64)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditionTag(server.edition)
                    Text(
                        server.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = server.queryAddress(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ServerListMeta(server)
            }
        }
    }
}

@Composable
private fun EditionTag(edition: ServerEdition) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = edition.shortName,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val ServerEdition.shortName: String
    get() = when (this) {
        ServerEdition.Java -> "JE"
        ServerEdition.Bedrock -> "BE"
    }

