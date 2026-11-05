package org.futo.inputmethod.latin.uix

// Pure logic for smart clipboard / snippet features. Intentionally free of Android
// dependencies so that it can be unit tested on the JVM.

/** A user-defined text snippet: typing [shortcut] offers to replace it with [text]. */
@kotlinx.serialization.Serializable
data class TextSnippet(
    val shortcut: String,
    val text: String,
)

enum class SmartSuggestionKind {
    Snippet,
    Command,
}

/**
 * A suggestion shown in the suggestion bar. Choosing it deletes the last [replaceLength]
 * characters before the cursor and inserts [insertText].
 */
data class SmartSuggestion(
    val kind: SmartSuggestionKind,
    val label: String,
    val insertText: String,
    val replaceLength: Int,
)

object SmartSuggestions {
    private const val MAX_SUGGESTIONS = 8

    /**
     * Returns the "word" directly before the cursor, delimited only by whitespace (so that
     * tokens like ";mail" or "/model" are kept intact), or null if there is none.
     */
    fun currentToken(textBeforeCursor: CharSequence?): String? {
        if (textBeforeCursor.isNullOrEmpty()) return null
        var start = textBeforeCursor.length
        while (start > 0 && !textBeforeCursor[start - 1].isWhitespace()) start--
        if (start == textBeforeCursor.length) return null
        return textBeforeCursor.substring(start)
    }

    /**
     * @param snippets user-defined snippets
     * @param commands candidate commands, e.g. the texts of pinned clips starting with "/"
     */
    fun compute(
        token: String?,
        snippets: List<TextSnippet>,
        commands: List<String>,
        snippetsEnabled: Boolean,
        commandsEnabled: Boolean,
    ): List<SmartSuggestion> {
        if (token.isNullOrEmpty()) return emptyList()

        val result = mutableListOf<SmartSuggestion>()

        if (snippetsEnabled) {
            // Exact (case-insensitive) matches first, then prefix matches once 2+ chars are typed
            val exact = snippets.filter { it.shortcut.isNotBlank() && it.shortcut.equals(token, ignoreCase = true) }
            val prefix = if (token.length >= 2) {
                snippets.filter {
                    it.shortcut.isNotBlank()
                            && it.shortcut.length > token.length
                            && it.shortcut.startsWith(token, ignoreCase = true)
                }
            } else emptyList()

            (exact + prefix).forEach {
                result.add(SmartSuggestion(SmartSuggestionKind.Snippet, it.text, it.text, token.length))
            }
        }

        if (commandsEnabled && token.startsWith("/")) {
            commands.asSequence()
                .map { it.trim() }
                .filter { it.startsWith("/") && !it.contains('\n') }
                .filter { it.startsWith(token, ignoreCase = true) && !it.equals(token, ignoreCase = false) }
                .distinct()
                .forEach {
                    result.add(SmartSuggestion(SmartSuggestionKind.Command, it, it, token.length))
                }
        }

        return result.distinctBy { it.insertText }.take(MAX_SUGGESTIONS)
    }
}

enum class ClipContentKind {
    OneTimeCode,
    Link,
    Email,
    Phone,
    Iban,
}

data class ClipContent(
    val kind: ClipContentKind,
    /** The detected value, normalized (e.g. IBAN without spaces) */
    val value: String,
)

object ClipContentDetector {
    private val linkRegex = """https?://[^\s<>"']+""".toRegex(RegexOption.IGNORE_CASE)
    private val emailRegex = """[\w\-.+]+@([\w-]+\.)+[\w-]{2,}""".toRegex()
    private val ibanRegex = """\b[A-Z]{2}\d{2}(?: ?[A-Z0-9]{4}){2,7}(?: ?[A-Z0-9]{1,4})?\b""".toRegex()
    private val phoneRegex = """(?<![\w+])(?:\+|00)?\d[\d ()/\-]{6,18}\d(?!\w)""".toRegex()

    // A whole clip that is just a short code (e.g. "123456", "G-482913", "AB12CD")
    private val wholeCodeRegex = """^(?:[A-Z]{1,3}-)?(?=[A-Z0-9]*\d)[A-Z0-9]{4,8}$""".toRegex()
    // A code embedded in a message like "Your verification code is 123456"
    private val codeKeywordRegex =
        """(?i)\b(code|codes|otp|pin|tan|passcode|bestätigungscode|verifizierungscode|sicherheitscode|einmalcode|kennwort|password|passwort|verification|verify|login|anmelde\w*)\b""".toRegex()
    private val embeddedCodeRegex = """(?<![\w.,])(?:\d{4,8}|\d{3}[ -]\d{3})(?![\w.,]?\d)""".toRegex()

    /** Returns the one-time code contained in [text], or null if it does not look like one. */
    fun findOneTimeCode(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > 300) return null

        if (wholeCodeRegex.matches(trimmed)) {
            // Avoid treating years like "2024" alone as codes? Codes are commonly 4 digits too,
            // so only skip things that look like a plain year in the 1900-2099 range.
            if (trimmed.length == 4 && trimmed.all { it.isDigit() } && trimmed.toInt() in 1900..2099) return null
            return trimmed
        }

        if (trimmed.contains('\n') && trimmed.length > 200) return null
        if (!codeKeywordRegex.containsMatchIn(trimmed)) return null
        val matches = embeddedCodeRegex.findAll(trimmed).map { it.value }.toList()
        if (matches.size != 1) return null
        return matches.first().replace(" ", "").replace("-", "")
    }

    fun isIbanValid(iban: String): Boolean {
        val compact = iban.replace(" ", "").uppercase()
        if (compact.length !in 15..34) return false
        if (!compact.all { it.isLetterOrDigit() && it.code < 128 }) return false
        val rearranged = compact.substring(4) + compact.substring(0, 4)
        var remainder = 0
        for (c in rearranged) {
            val digits = if (c.isDigit()) c.toString() else (c - 'A' + 10).toString()
            for (d in digits) {
                remainder = (remainder * 10 + (d - '0')) % 97
            }
        }
        return remainder == 1
    }

    /** Detects the most relevant kind of content in a clip, or null for plain text. */
    fun detect(text: String?): ClipContent? {
        if (text.isNullOrBlank()) return null
        if (text.length > 5_000) return null
        val trimmed = text.trim()

        findOneTimeCode(trimmed)?.let { return ClipContent(ClipContentKind.OneTimeCode, it) }

        ibanRegex.findAll(trimmed.uppercase()).firstOrNull { isIbanValid(it.value) }?.let {
            return ClipContent(ClipContentKind.Iban, it.value.replace(" ", ""))
        }

        linkRegex.find(trimmed)?.let {
            return ClipContent(ClipContentKind.Link, it.value.trimEnd('.', ',', ')', ';', ':', '!', '?'))
        }

        emailRegex.find(trimmed)?.let { return ClipContent(ClipContentKind.Email, it.value) }

        phoneRegex.find(trimmed)?.let { match ->
            val digits = match.value.count { it.isDigit() }
            if (digits in 7..15) {
                val normalized = match.value.filter { it.isDigit() || it == '+' }
                return ClipContent(ClipContentKind.Phone, normalized)
            }
        }

        return null
    }
}

object LinkCleaner {
    private val trackingParams = setOf(
        "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid", "twclid",
        "ttclid", "li_fat_id", "igshid", "igsh", "mc_cid", "mc_eid", "_hsenc", "_hsmi", "mkt_tok",
        "ref_src", "ref_url", "spm", "scm", "vero_id", "vero_conv", "oly_anon_id", "oly_enc_id",
        "rb_clickid", "wickedid", "trk", "trkcampaign", "sc_campaign", "sc_channel", "sc_content",
        "sc_medium", "sc_outcome", "sc_geo", "sc_country", "s_cid", "ncid", "cmpid", "_ga", "_gl",
        "si", "feature", "pp", "share_id", "mibextid", "rcm", "_branch_match_id",
        "_branch_referrer", "srsltid", "ved", "ei", "sca_esv",
    )
    private val trackingPrefixes = listOf("utm_", "pk_", "hsa_", "__hs", "ga_")

    // Parameters that are only tracking on specific hosts (they can be meaningful elsewhere)
    private val hostSpecificParams = mapOf(
        "youtube.com" to setOf("si", "feature", "pp"),
        "youtu.be" to setOf("si", "feature", "pp"),
        "spotify.com" to setOf("si", "context"),
        "amazon." to setOf("ref", "ref_", "tag", "psc", "pd_rd_w", "pd_rd_wg", "pd_rd_r", "pd_rd_i", "pf_rd_p", "pf_rd_r", "content-id", "crid", "sprefix", "qid", "sr", "keywords", "dib", "dib_tag"),
        "x.com" to setOf("s", "t"),
        "twitter.com" to setOf("s", "t"),
    )
    // Generic params that should only be removed on the hosts listed above
    private val onlyHostSpecific = setOf("si", "feature", "pp", "ei", "ved", "sca_esv")

    // "amazon." matches any amazon TLD, otherwise the host or one of its subdomains must match
    private fun hostMatches(host: String, rule: String): Boolean =
        if (rule.endsWith(".")) {
            host.startsWith(rule) || host.contains(".$rule")
        } else {
            host == rule || host.endsWith(".$rule")
        }

    private fun isTrackingParam(name: String, host: String): Boolean {
        val lower = name.lowercase()
        if (trackingPrefixes.any { lower.startsWith(it) }) return true
        val hostRules = hostSpecificParams.entries.filter { hostMatches(host, it.key) }
        if (hostRules.any { lower in it.value }) return true
        if (lower in onlyHostSpecific) {
            // Google search tracking params
            return (lower == "ved" || lower == "ei" || lower == "sca_esv") && host.contains("google.")
        }
        return lower in trackingParams
    }

    /** Removes known tracking parameters from a single URL. Returns the URL unchanged if nothing applies. */
    fun clean(url: String): String {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd < 0) return url

        val fragmentIdx = url.indexOf('#')
        val beforeFragment = if (fragmentIdx >= 0) url.substring(0, fragmentIdx) else url
        val fragment = if (fragmentIdx >= 0) url.substring(fragmentIdx) else ""

        val queryIdx = beforeFragment.indexOf('?')
        if (queryIdx < 0) return url

        val base = beforeFragment.substring(0, queryIdx)
        val query = beforeFragment.substring(queryIdx + 1)

        val hostStart = schemeEnd + 3
        val hostEnd = base.indexOfAny(charArrayOf('/', ':'), hostStart).let { if (it < 0) base.length else it }
        val host = base.substring(hostStart, hostEnd).lowercase().substringAfter('@')

        val params = query.split('&').filter { it.isNotEmpty() }
        val kept = params.filter { param ->
            val name = param.substringBefore('=')
            !isTrackingParam(name, host)
        }

        if (kept.size == params.size) return url

        val newQuery = kept.joinToString("&")
        return base + (if (newQuery.isNotEmpty()) "?$newQuery" else "") + fragment
    }

    /**
     * If [text] consists of a single link (optionally surrounded by whitespace), returns the
     * cleaned text, otherwise returns [text] unchanged.
     */
    fun cleanIfSingleLink(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return text
        if (!(trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true))) return text
        val cleaned = clean(trimmed)
        return if (cleaned == trimmed) text else cleaned
    }
}
