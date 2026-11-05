package org.futo.inputmethod.latin.uix

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.theme.Typography

private fun shortenLabel(text: String): String {
    val oneLine = text.replace("\n", " ")
    return if (oneLine.length > 28) oneLine.take(26) + "…" else oneLine
}

@Composable
private fun SmartSuggestionPill(suggestion: SmartSuggestion, onActivate: () -> Unit) {
    val description = when (suggestion.kind) {
        SmartSuggestionKind.Snippet -> stringResource(R.string.smart_suggestion_snippet, suggestion.label)
        SmartSuggestionKind.Command -> stringResource(R.string.smart_suggestion_command, suggestion.label)
    }

    Box(Modifier.fillMaxHeight().clickable { onActivate() }.padding(4.dp).clearAndSetSemantics {
        this.role = Role.Button
        this.contentDescription = description
        this.onClick(action = { onActivate(); true })
    }) {
        Surface(
            color = LocalKeyboardScheme.current.keyboardContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides LocalKeyboardScheme.current.onKeyboardContainer) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = CenterVertically,
                    modifier = Modifier.padding(6.dp).fillMaxHeight()
                ) {
                    Icon(
                        painterResource(
                            when (suggestion.kind) {
                                SmartSuggestionKind.Snippet -> R.drawable.clipboard
                                SmartSuggestionKind.Command -> R.drawable.push_pin
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(shortenLabel(suggestion.label), style = Typography.Small)
                }
            }
        }
    }
}

/** Shows snippet and command suggestions for the text typed before the cursor */
@Composable
fun RowScope.SmartSuggestionsView(
    suggestions: List<SmartSuggestion>,
    onActivate: (SmartSuggestion) -> Unit
) {
    LazyRow(Modifier.weight(1.0f)) {
        items(suggestions) { suggestion ->
            SmartSuggestionPill(suggestion) { onActivate(suggestion) }
        }
    }
}
