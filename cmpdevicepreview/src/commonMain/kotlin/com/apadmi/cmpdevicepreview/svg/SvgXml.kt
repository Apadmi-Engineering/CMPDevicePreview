package com.apadmi.cmpdevicepreview.svg

/**
 * A parsed XML element: a name, its attributes, and its element children.
 *
 * @property name The local element name, with any namespace prefix stripped (`svg:path` → `path`).
 * @property attributes The element's attributes, keyed by their raw (prefixed) name.
 */
internal class XmlElement(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<XmlElement>,
) {
    /**
     * Returns the value of [name], or null when absent or empty.
     *
     * @param name The attribute's raw name, including any namespace prefix.
     * @return The attribute's value, or null when it is absent or empty.
     */
    fun attribute(name: String): String? = attributes[name]?.takeIf { it.isNotEmpty() }

    override fun toString(): String = "<$name ${attributes.keys.joinToString(" ")}>"
}

/**
 * Parses [source] into its root [XmlElement]: elements, attributes and entity references in
 * attribute values. Text, comments, CDATA, processing instructions and doctypes are skipped, as the
 * SVG subset this library renders is entirely attribute-driven. Namespaces are not resolved.
 *
 * @param source The XML text to parse.
 * @return The document's root element.
 * @throws IllegalArgumentException on malformed input.
 */
internal fun parseXmlDocument(source: String): XmlElement = XmlParser(source).parseDocument()

private class XmlParser(private val source: String) {

    private var index = 0

    fun parseDocument(): XmlElement {
        while (true) {
            skipWhitespace()
            if (index >= source.length) fail("No XML element found")
            if (!startsWith("<")) fail("Expected an element")
            when {
                startsWith("<?") -> skipUntil("?>")
                startsWith("<!--") -> skipUntil("-->")
                startsWith("<!") -> skipDeclaration()
                else -> return parseElement()
            }
        }
    }

    private fun parseElement(): XmlElement {
        expect("<")
        val name = readName()
        val attributes = mutableMapOf<String, String>()
        while (true) {
            skipWhitespace()
            if (startsWith("/>")) {
                index += 2
                return XmlElement(localName(name), attributes, emptyList())
            }
            if (startsWith(">")) {
                index++
                break
            }
            val attributeName = readName()
            skipWhitespace()
            expect("=")
            skipWhitespace()
            attributes[attributeName] = readAttributeValue()
        }

        val children = mutableListOf<XmlElement>()
        while (true) {
            val textStart = index
            while (index < source.length && source[index] != '<') index++
            if (index >= source.length) fail("Unterminated element \"<$name>\"", textStart)
            when {
                startsWith("</") -> {
                    index += 2
                    val closing = readName()
                    skipWhitespace()
                    expect(">")
                    if (closing != name) fail("Closing tag \"</$closing>\" does not match \"<$name>\"")
                    return XmlElement(localName(name), attributes, children)
                }
                startsWith("<!--") -> skipUntil("-->")
                startsWith("<![CDATA[") -> skipUntil("]]>")
                startsWith("<?") -> skipUntil("?>")
                startsWith("<!") -> skipDeclaration()
                else -> children += parseElement()
            }
        }
    }

    private fun localName(name: String): String = name.substringAfter(':')

    private fun startsWith(pattern: String): Boolean = source.startsWith(pattern, index)

    private fun expect(pattern: String) {
        if (!startsWith(pattern)) fail("Expected \"$pattern\"")
        index += pattern.length
    }

    private fun skipUntil(pattern: String) {
        val end = source.indexOf(pattern, index)
        if (end == -1) fail("Expected \"$pattern\"")
        index = end + pattern.length
    }

    /** Skips `<!DOCTYPE …>`, including a bracketed internal subset. */
    private fun skipDeclaration() {
        index += 2
        while (index < source.length && source[index] != '>') {
            if (source[index] == '[') skipUntil("]") else index++
        }
        if (index >= source.length) fail("Unterminated declaration")
        index++
    }

    private fun skipWhitespace() {
        while (index < source.length && source[index].let { it == ' ' || it == '\t' || it == '\n' || it == '\r' }) {
            index++
        }
    }

    private fun readName(): String {
        val start = index
        while (index < source.length && source[index].isNameChar()) index++
        if (index == start) fail("Expected a name", start)
        return source.substring(start, index)
    }

    private fun Char.isNameChar(): Boolean =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '_' || this == '.' || this == ':'

    private fun readAttributeValue(): String {
        if (index >= source.length) fail("Expected an attribute value")
        val quote = source[index]
        if (quote != '"' && quote != '\'') fail("Attribute values must be quoted")
        index++
        val start = index
        while (index < source.length && source[index] != quote) index++
        if (index >= source.length) fail("Unterminated attribute value", start)
        val raw = source.substring(start, index)
        index++
        return decodeEntities(raw)
    }

    private fun fail(message: String, offset: Int = index): Nothing =
        throw IllegalArgumentException("$message (at offset $offset)")

    private companion object {
        val entityPattern = Regex("&([a-zA-Z]+|#[xX]?[0-9a-fA-F]+);")

        fun decodeEntities(value: String): String {
            if ('&' !in value) return value
            return entityPattern.replace(value) { match ->
                val entity = match.groupValues[1]
                when {
                    entity == "amp" -> "&"
                    entity == "lt" -> "<"
                    entity == "gt" -> ">"
                    entity == "quot" -> "\""
                    entity == "apos" -> "'"
                    entity.startsWith("#x") || entity.startsWith("#X") ->
                        entity.substring(2).toIntOrNull(16)?.let(::codePointToString) ?: match.value
                    entity.startsWith("#") ->
                        entity.substring(1).toIntOrNull()?.let(::codePointToString) ?: match.value
                    else -> match.value
                }
            }
        }

        fun codePointToString(codePoint: Int): String? = when {
            codePoint < 0 || codePoint > 0x10FFFF -> null
            codePoint < 0x10000 -> codePoint.toChar().toString()
            else -> {
                val offset = codePoint - 0x10000
                charArrayOf(
                    (0xD800 + (offset shr 10)).toChar(),
                    (0xDC00 + (offset and 0x3FF)).toChar(),
                ).concatToString()
            }
        }
    }
}
