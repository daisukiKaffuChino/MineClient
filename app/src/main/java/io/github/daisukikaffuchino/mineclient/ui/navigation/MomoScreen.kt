package io.github.daisukikaffuchino.mineclient.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class MomoScreen : NavKey {
    @Serializable
    data object Home : MomoScreen()

    @Serializable
    data object Settings : MomoScreen()
}
