package io.github.daisukikaffuchino.mineclient.utils

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.data.MinecraftText
import io.github.daisukikaffuchino.mineclient.data.MinecraftTextSpan
import io.github.daisukikaffuchino.mineclient.ui.ServerEntry

@Composable
fun ServerListMeta(server: ServerEntry) {
    val status = server.status
    val errorMessage = server.errorMessage
    when {
        status != null -> {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (server.isLoading) stringResource(R.string.status_refreshing) else "${status.latencyMillis} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (server.isLoading) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        latencyColor(status.latencyMillis)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "· ${status.onlinePlayers}/${status.maxPlayers}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (server.isLoading) {
                    AnnotatedString(stringResource(R.string.label_version) + " " + stringResource(R.string.status_refreshing))
                } else {
                    prefixedAnnotatedString(
                        stringResource(R.string.label_version) + " ",
                        status.protocolName
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        errorMessage != null -> Text(
            text = if (server.isLoading) stringResource(R.string.status_refreshing) else stringResource(
                R.string.status_offline_format,
                errorMessage
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (server.isLoading) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        else -> Text(
            text = if (server.isLoading) stringResource(R.string.status_first_query) else stringResource(
                R.string.status_waiting
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ServerIcon(faviconBase64: String?) {
    val bitmap = remember(faviconBase64) {
        faviconBase64?.let {
            runCatching {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }
    }
    Surface(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(R.string.server_icon_content_desc)
            )
        } else {
            Image(
                painter = painterResource(R.drawable.server_default),
                contentDescription = stringResource(R.string.server_icon_content_desc)
            )
        }
    }
}

private fun prefixedAnnotatedString(prefix: String, text: MinecraftText): AnnotatedString =
    buildAnnotatedString {
        append(prefix)
        val prefixLength = prefix.length
        append(text.plainText)
        text.spans.forEach { span ->
            addStyle(span.toSpanStyle(), prefixLength + span.start, prefixLength + span.end)
        }
    }

fun MinecraftTextSpan.toSpanStyle(): SpanStyle = SpanStyle(
    color = colorArgb?.let { Color(it) } ?: Color.Unspecified,
    fontWeight = if (isBold) FontWeight.Bold else null,
    fontStyle = if (isItalic) FontStyle.Italic else null,
    textDecoration = when {
        isUnderlined && isStrikethrough -> TextDecoration.combine(
            listOf(TextDecoration.Underline, TextDecoration.LineThrough)
        )

        isUnderlined -> TextDecoration.Underline
        isStrikethrough -> TextDecoration.LineThrough
        else -> null
    },
)

@Composable
fun latencyColor(latencyMillis: Long): Color = when {
    latencyMillis <= 100 -> Color(0xFF2E7D32)
    latencyMillis <= 250 -> Color(0xFFF57C00)
    else -> Color(0xFFD32F2F)
}

