package io.github.daisukikaffuchino.mineclient.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import com.kyant.m3color.dynamiccolor.ColorSpec
import com.kyant.m3color.dynamiccolor.DynamicScheme
import com.kyant.m3color.hct.Hct
import com.kyant.m3color.scheme.SchemeTonalSpot

@Composable
fun MineClientTheme(
    customKeyColor: Color = Color(0xff7db9de),
    darkTheme: Boolean = isSystemInDarkTheme(),
    contrastLevel: Double = 0.0,
    dynamicColor: Boolean = false,
    specVersion: ColorSpec.SpecVersion = ColorSpec.SpecVersion.SPEC_2021,
    content: @Composable () -> Unit
) {
    val baseColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dynamicColor) {
        colorResource(id = android.R.color.system_accent1_500)
    } else {
        customKeyColor
    }

    val colorScheme = colorScheme(
        keyColor = baseColor,
        isDark = darkTheme,
        contrastLevel = contrastLevel,
        specVersion = specVersion
    )

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

@Composable
@Stable
private fun colorScheme(
    keyColor: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
    animationSpec: AnimationSpec<Color> = spring(),
    specVersion: ColorSpec.SpecVersion,
    platform: DynamicScheme.Platform = DynamicScheme.Platform.PHONE
): ColorScheme {
    val hct = Hct.fromInt(keyColor.toArgb())
    val scheme = SchemeTonalSpot(
        hct,
        isDark,
        contrastLevel,
        specVersion,
        platform
    )

    return ColorScheme(
        primary = scheme.primary.toColor().animate(animationSpec),
        onPrimary = scheme.onPrimary.toColor().animate(animationSpec),
        primaryContainer = scheme.primaryContainer.toColor().animate(animationSpec),
        onPrimaryContainer = scheme.onPrimaryContainer.toColor().animate(animationSpec),
        inversePrimary = scheme.inversePrimary.toColor().animate(animationSpec),
        secondary = scheme.secondary.toColor().animate(animationSpec),
        onSecondary = scheme.onSecondary.toColor().animate(animationSpec),
        secondaryContainer = scheme.secondaryContainer.toColor().animate(animationSpec),
        onSecondaryContainer = scheme.onSecondaryContainer.toColor().animate(animationSpec),
        tertiary = scheme.tertiary.toColor().animate(animationSpec),
        onTertiary = scheme.onTertiary.toColor().animate(animationSpec),
        tertiaryContainer = scheme.tertiaryContainer.toColor().animate(animationSpec),
        onTertiaryContainer = scheme.onTertiaryContainer.toColor().animate(animationSpec),
        background = scheme.background.toColor().animate(animationSpec),
        onBackground = scheme.onBackground.toColor().animate(animationSpec),
        surface = scheme.surface.toColor().animate(animationSpec),
        onSurface = scheme.onSurface.toColor().animate(animationSpec),
        surfaceVariant = scheme.surfaceVariant.toColor().animate(animationSpec),
        onSurfaceVariant = scheme.onSurfaceVariant.toColor().animate(animationSpec),
        surfaceTint = scheme.surfaceTint.toColor().animate(animationSpec),
        inverseSurface = scheme.inverseSurface.toColor().animate(animationSpec),
        inverseOnSurface = scheme.inverseOnSurface.toColor().animate(animationSpec),
        error = scheme.error.toColor().animate(animationSpec),
        onError = scheme.onError.toColor().animate(animationSpec),
        errorContainer = scheme.errorContainer.toColor().animate(animationSpec),
        onErrorContainer = scheme.onErrorContainer.toColor().animate(animationSpec),
        outline = scheme.outline.toColor().animate(animationSpec),
        outlineVariant = scheme.outlineVariant.toColor().animate(animationSpec),
        scrim = scheme.scrim.toColor().animate(animationSpec),
        surfaceBright = scheme.surfaceBright.toColor().animate(animationSpec),
        surfaceDim = scheme.surfaceDim.toColor().animate(animationSpec),
        surfaceContainer = scheme.surfaceContainer.toColor().animate(animationSpec),
        surfaceContainerHigh = scheme.surfaceContainerHigh.toColor().animate(animationSpec),
        surfaceContainerHighest = scheme.surfaceContainerHighest.toColor().animate(animationSpec),
        surfaceContainerLow = scheme.surfaceContainerLow.toColor().animate(animationSpec),
        surfaceContainerLowest = scheme.surfaceContainerLowest.toColor().animate(animationSpec),
        primaryFixed = scheme.primaryFixed.toColor().animate(animationSpec),
        primaryFixedDim = scheme.primaryFixedDim.toColor().animate(animationSpec),
        onPrimaryFixed = scheme.onPrimaryFixed.toColor().animate(animationSpec),
        onPrimaryFixedVariant = scheme.onPrimaryFixedVariant.toColor().animate(animationSpec),
        secondaryFixed = scheme.secondaryFixed.toColor().animate(animationSpec),
        secondaryFixedDim = scheme.secondaryFixedDim.toColor().animate(animationSpec),
        onSecondaryFixed = scheme.onSecondaryFixed.toColor().animate(animationSpec),
        onSecondaryFixedVariant = scheme.onSecondaryFixedVariant.toColor().animate(animationSpec),
        tertiaryFixed = scheme.tertiaryFixed.toColor().animate(animationSpec),
        tertiaryFixedDim = scheme.tertiaryFixedDim.toColor().animate(animationSpec),
        onTertiaryFixed = scheme.onTertiaryFixed.toColor().animate(animationSpec),
        onTertiaryFixedVariant = scheme.onTertiaryFixedVariant.toColor().animate(animationSpec),
    )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toColor(): Color = Color(this)

@Composable
private fun Color.animate(animationSpec: AnimationSpec<Color> = spring()): Color =
    animateColorAsState(this, animationSpec).value


