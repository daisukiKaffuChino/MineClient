package io.github.daisukikaffuchino.mineclient.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class AppScreen : NavKey {
    @Serializable
    data object Home : AppScreen()

    @Serializable
    data object Settings : AppScreen()
}
