package org.futo.inputmethod.latin.uix.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.TextSnippet
import org.futo.inputmethod.latin.uix.TextSnippetStore
import org.futo.inputmethod.latin.uix.TextSnippetsSetting
import org.futo.inputmethod.latin.uix.theme.Typography

/** Settings component to add and remove text snippets */
@Composable
fun TextSnippetEditor() {
    val setting = useDataStore(TextSnippetsSetting)
    val snippets = remember(setting.value) { TextSnippetStore.parse(setting.value) }

    val shortcut = remember { mutableStateOf("") }
    val text = remember { mutableStateOf("") }

    fun save(list: List<TextSnippet>) {
        setting.setValue(TextSnippetStore.serialize(list))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp, 4.dp)) {
        if(snippets.isEmpty()) {
            Text(
                stringResource(R.string.text_snippets_empty),
                style = Typography.Body.Regular,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        snippets.forEach { snippet ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(snippet.shortcut, style = Typography.Body.Regular.copy(fontWeight = FontWeight.Bold))
                    Text(
                        snippet.text.let { if(it.length > 120) it.take(118) + "…" else it },
                        style = Typography.SmallMl
                    )
                }
                IconButton(onClick = {
                    save(snippets.filter { it != snippet })
                }) {
                    Icon(
                        painterResource(R.drawable.trash),
                        contentDescription = stringResource(R.string.text_snippets_delete, snippet.shortcut)
                    )
                }
            }
        }

        TextField(
            value = shortcut.value,
            onValueChange = { shortcut.value = it.filterNot { c -> c.isWhitespace() } },
            label = { Text(stringResource(R.string.text_snippets_shortcut)) },
            placeholder = { Text(";mail") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        TextField(
            value = text.value,
            onValueChange = { text.value = it },
            label = { Text(stringResource(R.string.text_snippets_text)) },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Button(
                enabled = shortcut.value.isNotBlank() && text.value.isNotEmpty(),
                onClick = {
                    val newSnippet = TextSnippet(shortcut.value.trim(), text.value)
                    // Replace a snippet that uses the same shortcut
                    save(snippets.filter { !it.shortcut.equals(newSnippet.shortcut, ignoreCase = true) } + newSnippet)
                    shortcut.value = ""
                    text.value = ""
                }
            ) {
                Text(stringResource(R.string.text_snippets_add))
            }
        }
    }
}
