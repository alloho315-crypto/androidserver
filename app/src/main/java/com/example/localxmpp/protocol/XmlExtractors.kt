package com.example.localxmpp.protocol

/**
 * Minimal XML extractor helpers for stream stanzas.
 * Assumes valid XMPP stanzas and intentionally keeps strict patterns.
 */
object XmlExtractors {
    fun attr(xml: String, name: String): String? {
        val regex = Regex("""\b$name\s*=\s*['\"]([^'\"]+)['\"]""")
        return regex.find(xml)?.groupValues?.get(1)
    }

    fun body(xml: String): String {
        val match = Regex("""<body>(.*?)</body>""", RegexOption.DOT_MATCHES_ALL).find(xml)
        return match?.groupValues?.get(1)?.trim() ?: ""
    }

    fun bindResource(xml: String): String? {
        val match = Regex("""<resource>(.*?)</resource>""", RegexOption.DOT_MATCHES_ALL).find(xml)
        return match?.groupValues?.get(1)?.trim()
    }
}
