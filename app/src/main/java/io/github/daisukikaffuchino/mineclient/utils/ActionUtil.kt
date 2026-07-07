package io.github.daisukikaffuchino.mineclient.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.ui.ServerEntry
import io.github.daisukikaffuchino.mineclient.ui.queryAddress
import kotlinx.coroutines.launch
import kotlin.math.abs

fun Context.shareServer(server: ServerEntry) {
    val status = server.status
    val shareText = buildString {
        appendLine(server.name)
        appendLine(getString(R.string.share_address_format, server.queryAddress()))
        if (status != null) {
            appendLine(getString(R.string.share_latency_format, status.latencyMillis))
            appendLine(
                getString(
                    R.string.share_players_format,
                    status.onlinePlayers,
                    status.maxPlayers
                )
            )
            appendLine(getString(R.string.share_version_format, status.protocolName.plainText))
            appendLine(getString(R.string.share_motd_format, status.motd.plainText))
        } else if (server.errorMessage != null) {
            appendLine(getString(R.string.share_status_offline_format, server.errorMessage))
        } else {
            appendLine(getString(R.string.share_status_waiting))
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    startActivity(Intent.createChooser(intent, getString(R.string.share_chooser_title)))
}

@SuppressLint("RememberInComposition")
@Stable
fun Modifier.verticalBounce(
    enabled: Boolean = true,
    maxOffset: Float = 100f,
    dragMultiplier: Float = 0.15f,
    settleBackMultiplier: Float = 0.3f,
): Modifier = composed {
    if (!enabled) return@composed this

    val scope = rememberCoroutineScope()
    val offsetY = Animatable(0f)

    val connection = object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero

            val current = offsetY.value
            if (current == 0f) return Offset.Zero

            val dragY = available.y

            val isDraggingBack =
                (current > 0f && dragY < 0f) || (current < 0f && dragY > 0f)

            if (!isDraggingBack) return Offset.Zero

            val target = (current + dragY * settleBackMultiplier)
                .coerceIn(-maxOffset, maxOffset)

            scope.launch {
                offsetY.snapTo(target)
            }

            return Offset(0f, dragY)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            if (available.y == 0f) return Offset.Zero

            val current = offsetY.value
            val target = (current + available.y * dragMultiplier)
                .coerceIn(-maxOffset, maxOffset)

            scope.launch {
                offsetY.snapTo(target)
            }

            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (abs(offsetY.value) > 0.5f) {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                return available
            }
            return Velocity.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (abs(offsetY.value) > 0.5f) {
                offsetY.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            return Velocity.Zero
        }
    }

    this
        .nestedScroll(connection)
        .graphicsLayer {
            translationY = offsetY.value
        }
}