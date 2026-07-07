package io.github.daisukikaffuchino.mineclient.data

internal sealed interface JsonValue

internal data class JsonObjectValue(val values: Map<String, JsonValue?>) : JsonValue {
    operator fun get(key: String): JsonValue? = values[key]
    fun objectValue(key: String): JsonObjectValue? = values[key] as? JsonObjectValue
    fun arrayValue(key: String): List<JsonValue?>? = (values[key] as? JsonArrayValue)?.values
    fun stringValue(key: String): String? = (values[key] as? JsonStringValue)?.value
    fun numberValue(key: String): Double? = (values[key] as? JsonNumberValue)?.value
    fun booleanValue(key: String): Boolean? = (values[key] as? JsonBooleanValue)?.value
}

internal data class JsonArrayValue(val values: List<JsonValue?>) : JsonValue
internal data class JsonStringValue(val value: String) : JsonValue
internal data class JsonNumberValue(val value: Double) : JsonValue
internal data class JsonBooleanValue(val value: Boolean) : JsonValue

internal class JsonParser(private val source: String) {
    private var index = 0

    fun parseObject(): JsonObjectValue {
        val value = parseRootValue()
        return value as? JsonObjectValue ?: error("JSON root is not an object")
    }

    fun parseArrayValue(): JsonArrayValue {
        val value = parseRootValue()
        return value as? JsonArrayValue ?: error("JSON root is not an array")
    }

    private fun parseRootValue(): JsonValue? {
        val value = parseValue()
        skipWhitespace()
        require(index == source.length) { "JSON parse failed" }
        return value
    }

    private fun parseValue(): JsonValue? {
        skipWhitespace()
        return when (peek()) {
            '{' -> parseJsonObject()
            '[' -> parseArray()
            '"' -> JsonStringValue(parseString())
            't' -> parseLiteral("true", JsonBooleanValue(true))
            'f' -> parseLiteral("false", JsonBooleanValue(false))
            'n' -> parseLiteral("null", null)
            else -> parseNumber()
        }
    }

    private fun parseJsonObject(): JsonObjectValue {
        expect('{')
        skipWhitespace()
        val values = mutableMapOf<String, JsonValue?>()
        if (consume('}')) return JsonObjectValue(values)
        while (true) {
            val key = parseString()
            skipWhitespace()
            expect(':')
            values[key] = parseValue()
            skipWhitespace()
            if (consume('}')) break
            expect(',')
        }
        return JsonObjectValue(values)
    }

    private fun parseArray(): JsonArrayValue {
        expect('[')
        skipWhitespace()
        val values = mutableListOf<JsonValue?>()
        if (consume(']')) return JsonArrayValue(values)
        while (true) {
            values += parseValue()
            skipWhitespace()
            if (consume(']')) break
            expect(',')
        }
        return JsonArrayValue(values)
    }

    private fun parseString(): String {
        expect('"')
        val builder = StringBuilder()
        while (index < source.length) {
            val char = source[index++]
            when (char) {
                '"' -> return builder.toString()
                '\\' -> builder.append(parseEscape())
                else -> builder.append(char)
            }
        }
        error("Unterminated string")
    }

    private fun parseEscape(): String {
        val escaped = source[index++]
        return when (escaped) {
            '"', '\\', '/' -> escaped.toString()
            'b' -> "\b"
            'f' -> "\u000C"
            'n' -> "\n"
            'r' -> "\r"
            't' -> "\t"
            'u' -> {
                val code = source.substring(index, index + 4).toInt(16)
                index += 4
                code.toChar().toString()
            }

            else -> error("Unknown escape character")
        }
    }

    private fun parseNumber(): JsonNumberValue {
        val start = index
        if (peek() == '-') index++
        while (peekOrNull()?.isDigit() == true) index++
        if (peekOrNull() == '.') {
            index++
            while (peekOrNull()?.isDigit() == true) index++
        }
        if (peekOrNull() == 'e' || peekOrNull() == 'E') {
            index++
            if (peekOrNull() == '+' || peekOrNull() == '-') index++
            while (peekOrNull()?.isDigit() == true) index++
        }
        return JsonNumberValue(source.substring(start, index).toDouble())
    }

    private fun parseLiteral(text: String, value: JsonValue?): JsonValue? {
        require(source.startsWith(text, index)) { "Invalid JSON literal" }
        index += text.length
        return value
    }

    private fun skipWhitespace() {
        while (peekOrNull()?.isWhitespace() == true) index++
    }

    private fun consume(char: Char): Boolean {
        if (peekOrNull() != char) return false
        index++
        return true
    }

    private fun expect(char: Char) {
        require(consume(char)) { "Expected character $char" }
    }

    private fun peek(): Char = peekOrNull() ?: error("Unexpected end of JSON")
    private fun peekOrNull(): Char? = source.getOrNull(index)
}

internal fun JsonValue?.asObjectOrNull(): JsonObjectValue? = this as? JsonObjectValue

internal fun JsonValue?.toPlainText(): String = when (this) {
    is JsonStringValue -> value
    is JsonNumberValue -> value.toString()
    is JsonBooleanValue -> value.toString()
    is JsonArrayValue -> values.joinToString(separator = "") { it.toPlainText() }
    is JsonObjectValue -> {
        val ownText = stringValue("text").orEmpty() + stringValue("translate").orEmpty()
        val extras = arrayValue("extra").orEmpty().joinToString(separator = "") { it.toPlainText() }
        ownText + extras
    }

    null -> ""
}







