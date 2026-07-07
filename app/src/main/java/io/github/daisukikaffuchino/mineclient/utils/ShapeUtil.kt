package io.github.daisukikaffuchino.mineclient.utils

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import io.github.daisukikaffuchino.mineclient.ui.theme.shapeByInteraction

object ShapeUtil {
    val defaultShape: CornerBasedShape
        @Composable
        get() = MaterialTheme.shapes.extraSmall

    val largeCornerShape: CornerBasedShape
        @Composable
        get() = MaterialTheme.shapes.large

    val pressedShape: CornerBasedShape
        @Composable
        get() = MaterialTheme.shapes.small

    @Composable
    fun shapes() = ButtonDefaults.shapes(
        shape = defaultShape,
        pressedShape = pressedShape
    )

    @Composable
    fun largerShapes() = ButtonDefaults.shapes(
        shape = largeCornerShape,
        pressedShape = pressedShape
    )

    val shapesDefaultAnimationSpec: FiniteAnimationSpec<Float>
        @Composable
        get() = MaterialTheme.motionScheme.defaultEffectsSpec()

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun animatedShape(shapes: ButtonShapes, interactionSource: MutableInteractionSource?): Shape {
        val userInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
        val pressed by userInteractionSource.collectIsPressedAsState()
        val animatedShape =
            shapeByInteraction(shapes, pressed, shapesDefaultAnimationSpec)
        return animatedShape
    }
}