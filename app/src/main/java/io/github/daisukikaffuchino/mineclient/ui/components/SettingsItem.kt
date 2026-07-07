@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.daisukikaffuchino.mineclient.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.daisukikaffuchino.mineclient.R
import io.github.daisukikaffuchino.mineclient.utils.ShapeUtil
import io.github.daisukikaffuchino.mineclient.utils.ShapeUtil.animatedShape

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    @DrawableRes leadingIconRes: Int,
    title: String,
    description: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    background: Color = MaterialTheme.colorScheme.surfaceBright,
    shapes: ButtonShapes = ShapeUtil.shapes(),
    onClick: () -> Unit = {}
) = SettingsItem(
    leadingIcon = painterResource(leadingIconRes),
    title = title,
    description = description,
    trailingContent = trailingContent,
    background = background,
    shapes = shapes,
    onClick = onClick,
    modifier = modifier
)

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    title: String,
    description: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    background: Color = MaterialTheme.colorScheme.surfaceBright,
    shapes: ButtonShapes = ShapeUtil.shapes(),
    onClick: () -> Unit = {}
) = SettingsItem(
    leadingIcon = {
        leadingIcon?.let {
            Icon(
                painter = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 24.dp)
            )
        }
    },
    title = title,
    description = description,
    trailingContent = trailingContent,
    background = background,
    shapes = shapes,
    onClick = onClick,
    modifier = modifier
)

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    title: String,
    description: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    background: Color = MaterialTheme.colorScheme.surfaceBright,
    shapes: ButtonShapes = ShapeUtil.shapes(),
    onClick: () -> Unit = {},
) = SettingsItem(
    modifier = modifier,
    leadingIcon = leadingIcon,
    headlineContent = {
        Text(
            text = title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp
            )
        )
    },
    supportingContent = {
        description?.let {
            Text(
                text = it,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    },
    trailingContent = trailingContent,
    background = background,
    shapes = shapes,
    onClick = onClick
)

@Composable
fun SettingsItem(
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    headlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    background: Color = MaterialTheme.colorScheme.surfaceBright,
    shapes: ButtonShapes = ShapeUtil.shapes(),
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit = {},
) {
    val userInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = animatedShape(shapes, userInteractionSource),
        color = background,
        interactionSource = userInteractionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    horizontal = 24.dp,
                    vertical = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let { it() }
            Column(modifier = Modifier.weight(1f)) {
                headlineContent?.let { it() }
                supportingContent?.let { it() }
            }
            trailingContent?.let { it() }
        }
    }
}

@Composable
fun SwitchSettingsItem(
    modifier: Modifier = Modifier,
    @DrawableRes leadingIconRes: Int,
    title: String,
    description: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItem(
        leadingIcon = painterResource(leadingIconRes),
        title = title,
        description = description,
        trailingContent = {
            Switch(
                checked = checked,
                enabled = enabled,
                thumbContent = if (checked) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    {
                        Icon(
                            painter = painterResource(R.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                },
                onCheckedChange = null,
                modifier = Modifier.padding(start = 12.dp)
            )
        },
        onClick = {
            if (enabled) onCheckedChange(!checked)
        },
        modifier = modifier,
    )
}
