package org.futo.inputmethod.latin.uix

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.futo.inputmethod.latin.R

val TextSnippetsSetting = SettingsKey(
    stringPreferencesKey("text_snippets_json"),
    "[]"
)

val TextSnippetsEnabled = SettingsKey(
    booleanPreferencesKey("text_snippets_enabled"),
    true
)

val CommandSuggestionsEnabled = SettingsKey(
    booleanPreferencesKey("clipboard_command_suggestions_enabled"),
    true
)

val ClipboardAutoDeleteCodes = SettingsKey(
    booleanPreferencesKey("clipboard_auto_delete_one_time_codes"),
    true
)

val ClipboardCleanLinks = SettingsKey(
    booleanPreferencesKey("clipboard_clean_tracking_links"),
    true
)

/** Comma separated list of folder names for pinned clips. Empty means: use the defaults. */
val ClipboardPinnedFolders = SettingsKey(
    stringPreferencesKey("clipboard_pinned_folders"),
    ""
)

/** One-time codes are removed from the clipboard history after this time */
const val OneTimeCodeLifetimeMs = 2L * 60L * 1000L

private val snippetJson = Json { ignoreUnknownKeys = true }

object TextSnippetStore {
    private var cachedJson: String? = null
    private var cachedList: List<TextSnippet> = emptyList()

    fun parse(json: String): List<TextSnippet> {
        if (json == cachedJson) return cachedList
        val list = try {
            snippetJson.decodeFromString<List<TextSnippet>>(json)
        } catch (e: Exception) {
            emptyList()
        }
        cachedJson = json
        cachedList = list
        return list
    }

    fun serialize(list: List<TextSnippet>): String =
        snippetJson.encodeToString(list)

    fun get(context: Context): List<TextSnippet> =
        parse(context.getSetting(TextSnippetsSetting))
}

fun parseFolderList(value: String): List<String> =
    value.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

fun Context.getPinnedFolders(): List<String> {
    val value = getSetting(ClipboardPinnedFolders)
    return parseFolderList(value.ifBlank { getString(R.string.clipboard_folders_default) })
}
