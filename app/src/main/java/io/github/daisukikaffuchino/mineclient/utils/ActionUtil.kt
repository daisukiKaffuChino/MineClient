package io.github.daisukikaffuchino.mineclient.utils

import android.content.Context
import android.content.Intent
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.ui.ServerEntry
import io.github.daisukikaffuchino.mineclient.ui.queryAddress

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