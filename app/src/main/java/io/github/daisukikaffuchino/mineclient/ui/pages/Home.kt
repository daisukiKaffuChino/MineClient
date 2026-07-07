package io.github.daisukikaffuchino.mineclient.ui.pages

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.ui.ServerEdition
import io.github.daisukikaffuchino.mineclient.ui.ServerEntry
import io.github.daisukikaffuchino.mineclient.ui.ServerStatusUiState
import io.github.daisukikaffuchino.mineclient.ui.queryAddress
import io.github.daisukikaffuchino.mineclient.utils.ServerIcon
import io.github.daisukikaffuchino.mineclient.utils.ServerListMeta
import io.github.daisukikaffuchino.mineclient.utils.ShapeUtil
import io.github.daisukikaffuchino.mineclient.utils.ShapeUtil.animatedShape
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.nativeClipboardManager
import androidx.compose.ui.text.AnnotatedString

@Composable
fun HomePage(
    state: ServerStatusUiState,
    listState: LazyListState = rememberLazyListState(),
    onServerClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit = {},
) {
    if (state.servers.isEmpty()) {
        EmptyServerList(
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(state.servers, key = { it.id }) { server ->
                ServerListItem(
                    server = server,
                    modifier = Modifier.animateItem(),
                    onClick = { onServerClick(server.id) },
                    onEditClick = { onEditClick(server.id) },
                )
            }
        }
    }
}

@Composable
private fun EmptyServerList(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_dns),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.empty_desc),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServerListItem(
    server: ServerEntry,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceBright,
    shapes: ButtonShapes = ShapeUtil.largerShapes(),
    onClick: () -> Unit,
    onEditClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    var showMenu by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(animatedShape(shapes, interactionSource))
            .indication(interactionSource, LocalIndication.current)
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = { showMenu = true },
            ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = animatedShape(shapes, interactionSource),
            color = background,
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
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_server)) },
                onClick = {
                    showMenu = false
                    onEditClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_copy_address)) },
                onClick = {
                    showMenu = false
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "server_address",
                            server.queryAddress()
                        )
                    )
                },
            )
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

