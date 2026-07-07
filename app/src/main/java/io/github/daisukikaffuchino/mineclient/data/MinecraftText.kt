package io.github.daisukikaffuchino.mineclient.data

private val MinecraftColorMap = mapOf(
    '0' to 0xFF000000.toInt(),
    '1' to 0xFF0000AA.toInt(),
    '2' to 0xFF00AA00.toInt(),
    '3' to 0xFF00AAAA.toInt(),
    '4' to 0xFFAA0000.toInt(),
    '5' to 0xFFAA00AA.toInt(),
    '6' to 0xFFFFAA00.toInt(),
    '7' to 0xFFAAAAAA.toInt(),
    '8' to 0xFF555555.toInt(),
    '9' to 0xFF5555FF.toInt(),
    'a' to 0xFF55FF55.toInt(),
    'b' to 0xFF55FFFF.toInt(),
    'c' to 0xFFFF5555.toInt(),
    'd' to 0xFFFF55FF.toInt(),
    'e' to 0xFFFFFF55.toInt(),
    'f' to 0xFFFFFFFF.toInt(),
)

data class MinecraftText(
    val plainText: String,
    val spans: List<MinecraftTextSpan> = emptyList(),
) {
    companion object {
        val Empty = MinecraftText("")
    }
}

data class MinecraftTextSpan(
    val start: Int,
    val end: Int,
    val colorArgb: Int? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isObfuscated: Boolean = false,
)

private data class MinecraftTextStyle(
    val colorArgb: Int? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isObfuscated: Boolean = false,
)

private class MinecraftTextBuilder {
    private val text = StringBuilder()
    private val spans = mutableListOf<MinecraftTextSpan>()
    private var style = MinecraftTextStyle()
    private var spanStart = 0

    val length: Int get() = text.length

    fun append(input: String) {
        input.forEach { append(it) }
    }

    fun appendLegacy(input: String) {
        var expectStyleCode = false
        input.forEach { char ->
            when {
                expectStyleCode -> {
                    applyCode(char)
                    expectStyleCode = false
                }
                char == '§' || char == '\u6402' -> expectStyleCode = true
                else -> append(char)
            }
        }
    }

    fun append(char: Char) {
        text.append(char)
    }

    fun applyCode(code: Char) {
        val normalized = code.lowercaseChar()
        when {
            normalized in MinecraftColorMap -> updateStyle(MinecraftTextStyle(colorArgb = MinecraftColorMap[normalized]))
            normalized == 'r' -> updateStyle(MinecraftTextStyle())
            normalized == 'l' -> updateStyle(style.copy(isBold = true))
            normalized == 'm' -> updateStyle(style.copy(isStrikethrough = true))
            normalized == 'n' -> updateStyle(style.copy(isUnderlined = true))
            normalized == 'o' -> updateStyle(style.copy(isItalic = true))
            normalized == 'k' -> updateStyle(style.copy(isObfuscated = true))
        }
    }

    fun applyJsonStyle(component: JsonObjectValue) {
        val color = component.stringValue("color")?.let(::minecraftColorNameToArgb)
        updateStyle(
            style.copy(
                colorArgb = color ?: style.colorArgb,
                isBold = component.booleanValue("bold") ?: style.isBold,
                isItalic = component.booleanValue("italic") ?: style.isItalic,
                isUnderlined = component.booleanValue("underlined") ?: style.isUnderlined,
                isStrikethrough = component.booleanValue("strikethrough") ?: style.isStrikethrough,
                isObfuscated = component.booleanValue("obfuscated") ?: style.isObfuscated,
            )
        )
    }

    fun restoreStyle(previousStyle: MinecraftTextStyle) {
        finishSpan()
        style = previousStyle
        spanStart = text.length
    }

    fun snapshotStyle(): MinecraftTextStyle = style

    fun build(): MinecraftText {
        finishSpan()
        return MinecraftText(text.toString(), spans.toList())
    }

    private fun updateStyle(newStyle: MinecraftTextStyle) {
        if (newStyle == style) return
        finishSpan()
        style = newStyle
        spanStart = text.length
    }

    private fun finishSpan() {
        if (spanStart >= text.length || style == MinecraftTextStyle()) return
        spans += MinecraftTextSpan(
            start = spanStart,
            end = text.length,
            colorArgb = style.colorArgb,
            isBold = style.isBold,
            isItalic = style.isItalic,
            isUnderlined = style.isUnderlined,
            isStrikethrough = style.isStrikethrough,
            isObfuscated = style.isObfuscated,
        )
        spanStart = text.length
    }
}

fun parseMinecraftLegacyText(message: String): MinecraftText {
    if (message.isBlank()) return MinecraftText.Empty
    val builder = MinecraftTextBuilder()
    builder.appendLegacy(message)
    return builder.build()
}

internal fun JsonValue?.toMinecraftText(): MinecraftText {
    val builder = MinecraftTextBuilder()
    appendJsonTextTo(builder, this)
    return builder.build()
}

private fun appendJsonTextTo(builder: MinecraftTextBuilder, value: JsonValue?) {
    when (value) {
        is JsonStringValue -> builder.appendLegacy(value.value)
        is JsonNumberValue -> builder.append(value.value.toString())
        is JsonBooleanValue -> builder.append(value.value.toString())
        is JsonArrayValue -> value.values.forEach { appendJsonTextTo(builder, it) }
        is JsonObjectValue -> {
            val previousStyle = builder.snapshotStyle()
            builder.applyJsonStyle(value)
            value.stringValue("text")?.let(builder::appendLegacy)
            value.stringValue("translate")?.let(builder::appendLegacy)
            value.arrayValue("extra").orEmpty().forEach { appendJsonTextTo(builder, it) }
            builder.restoreStyle(previousStyle)
        }
        null -> Unit
    }
}

private fun minecraftColorNameToArgb(name: String): Int? {
    if (name.startsWith("#") && name.length == 7) {
        return runCatching { (0xFF000000 or name.drop(1).toLong(16)).toInt() }.getOrNull()
    }
    return when (name.lowercase()) {
        "black" -> MinecraftColorMap['0']
        "dark_blue" -> MinecraftColorMap['1']
        "dark_green" -> MinecraftColorMap['2']
        "dark_aqua" -> MinecraftColorMap['3']
        "dark_red" -> MinecraftColorMap['4']
        "dark_purple" -> MinecraftColorMap['5']
        "gold" -> MinecraftColorMap['6']
        "gray" -> MinecraftColorMap['7']
        "dark_gray" -> MinecraftColorMap['8']
        "blue" -> MinecraftColorMap['9']
        "green" -> MinecraftColorMap['a']
        "aqua" -> MinecraftColorMap['b']
        "red" -> MinecraftColorMap['c']
        "light_purple" -> MinecraftColorMap['d']
        "yellow" -> MinecraftColorMap['e']
        "white" -> MinecraftColorMap['f']
        else -> null
    }
}




