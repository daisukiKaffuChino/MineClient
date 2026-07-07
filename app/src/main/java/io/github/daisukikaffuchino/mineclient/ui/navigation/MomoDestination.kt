package io.github.daisukikaffuchino.mineclient.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.daisukikaffuchino.mineclient.R

enum class MomoDestination(
    val route: MomoScreen,
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    @param:DrawableRes val selectedIcon: Int
) {
    Home(
        route = MomoScreen.Home,
        label = R.string.nav_home,
        icon = R.drawable.ic_home,
        selectedIcon = R.drawable.ic_home_filled
    ),
    Settings(
        route = MomoScreen.Settings,
        label = R.string.nav_settings,
        icon = R.drawable.ic_settings,
        selectedIcon = R.drawable.ic_settings_filled
    )
}
