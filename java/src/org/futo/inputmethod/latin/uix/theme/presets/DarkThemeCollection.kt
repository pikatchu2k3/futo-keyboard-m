package org.futo.inputmethod.latin.uix.theme.presets

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.extendedDarkColorScheme
import org.futo.inputmethod.latin.uix.theme.ThemeOption
import org.futo.inputmethod.latin.uix.theme.selector.ThemePreview

// Dark themes based on popular open color palettes. Key labels have a contrast ratio of at
// least 7:1 (WCAG AAA) against the keys, other text at least 4.5:1 (WCAG AA).

// Colors: Dracula Theme (draculatheme.com, MIT)
val Dracula = ThemeOption(
    dynamic = false,
    key = "Dracula",
    name = R.string.theme_dracula,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFFBD93F9),
        onPrimary = Color(0xFF191A21),
        primaryContainer = Color(0xFF4D4265),
        onPrimaryContainer = Color(0xFFE9DFF4),
        secondary = Color(0xFFFF79C6),
        onSecondary = Color(0xFF191A21),
        secondaryContainer = Color(0xFF583852),
        onSecondaryContainer = Color(0xFFF9DFE9),
        tertiary = Color(0xFF8BE9FD),
        onTertiary = Color(0xFF191A21),
        tertiaryContainer = Color(0xFF3C5460),
        onTertiaryContainer = Color(0xFFE2F5F4),
        error = Color(0xFFFF5555),
        onError = Color(0xFF191A21),
        errorContainer = Color(0xFF6F343A),
        onErrorContainer = Color(0xFFF9E0DA),
        outline = Color(0xFFA7AFD6),
        outlineVariant = Color(0xFF44475A),
        surface = Color(0xFF191A21),
        onSurface = Color(0xFFF8F8F2),
        onSurfaceVariant = Color(0xFFA7AFD6),
        surfaceContainerHighest = Color(0xFF44475A),
        keyboardSurface = Color(0xFF21222C),
        keyboardContainer = Color(0xFF343746),
        keyboardContainerVariant = Color(0xFF44475A),
        onKeyboardContainer = Color(0xFFF8F8F2),
        keyboardPress = Color(0xFF565A70),
        keyboardFade0 = Color(0xFF21222C),
        keyboardFade1 = Color(0xFF21222C),
        primaryTransparent = Color(0xFFBD93F9).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFF8F8F2).copy(alpha = 0.1f),
        shadow = Color(0xFF191A21).copy(alpha = 0.7f),
    )
}

// Colors: Nord (nordtheme.com, MIT)
val Nord = ThemeOption(
    dynamic = false,
    key = "Nord",
    name = R.string.theme_nord,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFF88C0D0),
        onPrimary = Color(0xFF242933),
        primaryContainer = Color(0xFF475B68),
        onPrimaryContainer = Color(0xFFD3E3EB),
        secondary = Color(0xFF81A1C1),
        onSecondary = Color(0xFF242933),
        secondaryContainer = Color(0xFF434F60),
        onSecondaryContainer = Color(0xFFD7DFEA),
        tertiary = Color(0xFFA3BE8C),
        onTertiary = Color(0xFF242933),
        tertiaryContainer = Color(0xFF4B5653),
        onTertiaryContainer = Color(0xFFDDE5DF),
        error = Color(0xFFBF616A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF61444F),
        onErrorContainer = Color(0xFFE5DADF),
        outline = Color(0xFFC7CEDB),
        outlineVariant = Color(0xFF434C5E),
        surface = Color(0xFF242933),
        onSurface = Color(0xFFECEFF4),
        onSurfaceVariant = Color(0xFFC7CEDB),
        surfaceContainerHighest = Color(0xFF434C5E),
        keyboardSurface = Color(0xFF2E3440),
        keyboardContainer = Color(0xFF3B4252),
        keyboardContainerVariant = Color(0xFF434C5E),
        onKeyboardContainer = Color(0xFFECEFF4),
        keyboardPress = Color(0xFF4C566A),
        keyboardFade0 = Color(0xFF2E3440),
        keyboardFade1 = Color(0xFF2E3440),
        primaryTransparent = Color(0xFF88C0D0).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFECEFF4).copy(alpha = 0.1f),
        shadow = Color(0xFF242933).copy(alpha = 0.7f),
    )
}

// Colors: Gruvbox (github.com/morhetz/gruvbox, MIT)
val GruvboxDark = ThemeOption(
    dynamic = false,
    key = "GruvboxDark",
    name = R.string.theme_gruvbox_dark,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFFFABD2F),
        onPrimary = Color(0xFF1D2021),
        primaryContainer = Color(0xFF63522A),
        onPrimaryContainer = Color(0xFFFBE4A1),
        secondary = Color(0xFFFE8019),
        onSecondary = Color(0xFF1D2021),
        secondaryContainer = Color(0xFF5E3E24),
        onSecondaryContainer = Color(0xFFFCDAA4),
        tertiary = Color(0xFF8EC07C),
        onTertiary = Color(0xFF1D2021),
        tertiaryContainer = Color(0xFF424E3D),
        onTertiaryContainer = Color(0xFFE5E7B8),
        error = Color(0xFFFB4934),
        onError = Color(0xFF1D2021),
        errorContainer = Color(0xFF72342C),
        onErrorContainer = Color(0xFFFBD8B1),
        outline = Color(0xFFD5C4A1),
        outlineVariant = Color(0xFF504945),
        surface = Color(0xFF1D2021),
        onSurface = Color(0xFFFBF1C7),
        onSurfaceVariant = Color(0xFFD5C4A1),
        surfaceContainerHighest = Color(0xFF504945),
        keyboardSurface = Color(0xFF282828),
        keyboardContainer = Color(0xFF3C3836),
        keyboardContainerVariant = Color(0xFF504945),
        onKeyboardContainer = Color(0xFFFBF1C7),
        keyboardPress = Color(0xFF665C54),
        keyboardFade0 = Color(0xFF282828),
        keyboardFade1 = Color(0xFF282828),
        primaryTransparent = Color(0xFFFABD2F).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFFBF1C7).copy(alpha = 0.1f),
        shadow = Color(0xFF1D2021).copy(alpha = 0.7f),
    )
}

// Colors: Tokyo Night (github.com/folke/tokyonight.nvim, Apache-2.0)
val TokyoNight = ThemeOption(
    dynamic = false,
    key = "TokyoNight",
    name = R.string.theme_tokyo_night,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFF7AA2F7),
        onPrimary = Color(0xFF16161E),
        primaryContainer = Color(0xFF354161),
        onPrimaryContainer = Color(0xFFAEC0F6),
        secondary = Color(0xFFBB9AF7),
        onSecondary = Color(0xFF16161E),
        secondaryContainer = Color(0xFF423B5A),
        onSecondaryContainer = Color(0xFFBFC0F5),
        tertiary = Color(0xFF7DCFFF),
        onTertiary = Color(0xFF16161E),
        tertiaryContainer = Color(0xFF33485C),
        onTertiaryContainer = Color(0xFFB3CBF7),
        error = Color(0xFFF7768E),
        onError = Color(0xFF16161E),
        errorContainer = Color(0xFF673B4A),
        onErrorContainer = Color(0xFFC8BDE6),
        outline = Color(0xFFA9B1D6),
        outlineVariant = Color(0xFF292E42),
        surface = Color(0xFF16161E),
        onSurface = Color(0xFFC0CAF5),
        onSurfaceVariant = Color(0xFFA9B1D6),
        surfaceContainerHighest = Color(0xFF292E42),
        keyboardSurface = Color(0xFF1A1B26),
        keyboardContainer = Color(0xFF24283B),
        keyboardContainerVariant = Color(0xFF292E42),
        onKeyboardContainer = Color(0xFFC0CAF5),
        keyboardPress = Color(0xFF3B4261),
        keyboardFade0 = Color(0xFF1A1B26),
        keyboardFade1 = Color(0xFF1A1B26),
        primaryTransparent = Color(0xFF7AA2F7).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFC0CAF5).copy(alpha = 0.1f),
        shadow = Color(0xFF16161E).copy(alpha = 0.7f),
    )
}

// Colors: One Dark (github.com/atom/atom, MIT)
val OneDark = ThemeOption(
    dynamic = false,
    key = "OneDark",
    name = R.string.theme_one_dark,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFF61AFEF),
        onPrimary = Color(0xFF1B1F27),
        primaryContainer = Color(0xFF334C62),
        onPrimaryContainer = Color(0xFFC5DAEF),
        secondary = Color(0xFFC678DD),
        onSecondary = Color(0xFF1B1F27),
        secondaryContainer = Color(0xFF4A3A58),
        onSecondaryContainer = Color(0xFFE0D2EB),
        tertiary = Color(0xFF98C379),
        onTertiary = Color(0xFF1B1F27),
        tertiaryContainer = Color(0xFF3F4C3E),
        onTertiaryContainer = Color(0xFFD6E1D7),
        error = Color(0xFFE06C75),
        onError = Color(0xFF1B1F27),
        errorContainer = Color(0xFF643E45),
        onErrorContainer = Color(0xFFE5D6DD),
        outline = Color(0xFFABB2BF),
        outlineVariant = Color(0xFF3E4451),
        surface = Color(0xFF1B1F27),
        onSurface = Color(0xFFE6E9EF),
        onSurfaceVariant = Color(0xFFABB2BF),
        surfaceContainerHighest = Color(0xFF3E4451),
        keyboardSurface = Color(0xFF21252B),
        keyboardContainer = Color(0xFF2C313A),
        keyboardContainerVariant = Color(0xFF3E4451),
        onKeyboardContainer = Color(0xFFE6E9EF),
        keyboardPress = Color(0xFF4B5263),
        keyboardFade0 = Color(0xFF21252B),
        keyboardFade1 = Color(0xFF21252B),
        primaryTransparent = Color(0xFF61AFEF).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFE6E9EF).copy(alpha = 0.1f),
        shadow = Color(0xFF1B1F27).copy(alpha = 0.7f),
    )
}

// Colors: Solarized (ethanschoonover.com/solarized, MIT)
val SolarizedDark = ThemeOption(
    dynamic = false,
    key = "SolarizedDark",
    name = R.string.theme_solarized_dark,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFF2AA198),
        onPrimary = Color(0xFF001F27),
        primaryContainer = Color(0xFF0C4C51),
        onPrimaryContainer = Color(0xFFBDD6C6),
        secondary = Color(0xFF268BD2),
        onSecondary = Color(0xFF001F27),
        secondaryContainer = Color(0xFF0A435D),
        onSecondaryContainer = Color(0xFFC6D5D4),
        tertiary = Color(0xFFB58900),
        onTertiary = Color(0xFF001F27),
        tertiaryContainer = Color(0xFF2D4228),
        onTertiaryContainer = Color(0xFFE3D5AA),
        error = Color(0xFFDC322F),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF4D2D34),
        onErrorContainer = Color(0xFFEBCDBC),
        outline = Color(0xFF93A1A1),
        outlineVariant = Color(0xFF0D4452),
        surface = Color(0xFF001F27),
        onSurface = Color(0xFFEEE8D5),
        onSurfaceVariant = Color(0xFF93A1A1),
        surfaceContainerHighest = Color(0xFF0D4452),
        keyboardSurface = Color(0xFF002B36),
        keyboardContainer = Color(0xFF073642),
        keyboardContainerVariant = Color(0xFF0D4452),
        onKeyboardContainer = Color(0xFFEEE8D5),
        keyboardPress = Color(0xFF1A5566),
        keyboardFade0 = Color(0xFF002B36),
        keyboardFade1 = Color(0xFF002B36),
        primaryTransparent = Color(0xFF2AA198).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFEEE8D5).copy(alpha = 0.1f),
        shadow = Color(0xFF001F27).copy(alpha = 0.7f),
    )
}

// Colors: Rosé Pine (rosepinetheme.com, MIT)
val RosePine = ThemeOption(
    dynamic = false,
    key = "RosePine",
    name = R.string.theme_rose_pine,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFFEBBCBA),
        onPrimary = Color(0xFF12101B),
        primaryContainer = Color(0xFF54454E),
        onPrimaryContainer = Color(0xFFE3D6E6),
        secondary = Color(0xFFC4A7E7),
        onSecondary = Color(0xFF12101B),
        secondaryContainer = Color(0xFF443B55),
        onSecondaryContainer = Color(0xFFDAD3F1),
        tertiary = Color(0xFF9CCFD8),
        onTertiary = Color(0xFF12101B),
        tertiaryContainer = Color(0xFF3A4551),
        onTertiaryContainer = Color(0xFFD2DBEE),
        error = Color(0xFFEB6F92),
        onError = Color(0xFF12101B),
        errorContainer = Color(0xFF62364A),
        onErrorContainer = Color(0xFFE2CDE5),
        outline = Color(0xFF908CAA),
        outlineVariant = Color(0xFF2F2B47),
        surface = Color(0xFF12101B),
        onSurface = Color(0xFFE0DEF4),
        onSurfaceVariant = Color(0xFF908CAA),
        surfaceContainerHighest = Color(0xFF2F2B47),
        keyboardSurface = Color(0xFF191724),
        keyboardContainer = Color(0xFF26233A),
        keyboardContainerVariant = Color(0xFF2F2B47),
        onKeyboardContainer = Color(0xFFE0DEF4),
        keyboardPress = Color(0xFF433F63),
        keyboardFade0 = Color(0xFF191724),
        keyboardFade1 = Color(0xFF191724),
        primaryTransparent = Color(0xFFEBBCBA).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFE0DEF4).copy(alpha = 0.1f),
        shadow = Color(0xFF12101B).copy(alpha = 0.7f),
    )
}

// Colors: Everforest (github.com/sainnhe/everforest, MIT)
val EverforestDark = ThemeOption(
    dynamic = false,
    key = "EverforestDark",
    name = R.string.theme_everforest_dark,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFFA7C080),
        onPrimary = Color(0xFF232A2E),
        primaryContainer = Color(0xFF4F5C4E),
        onPrimaryContainer = Color(0xFFDFDEBC),
        secondary = Color(0xFF83C092),
        onSecondary = Color(0xFF232A2E),
        secondaryContainer = Color(0xFF425851),
        onSecondaryContainer = Color(0xFFDCE0C4),
        tertiary = Color(0xFFDBBC7F),
        onTertiary = Color(0xFF232A2E),
        tertiaryContainer = Color(0xFF58574C),
        onTertiaryContainer = Color(0xFFEDDFC0),
        error = Color(0xFFE67E80),
        onError = Color(0xFF232A2E),
        errorContainer = Color(0xFF6E4F53),
        onErrorContainer = Color(0xFFF0D8C4),
        outline = Color(0xFFBEB49B),
        outlineVariant = Color(0xFF3D484D),
        surface = Color(0xFF232A2E),
        onSurface = Color(0xFFF2E8D0),
        onSurfaceVariant = Color(0xFFBEB49B),
        surfaceContainerHighest = Color(0xFF3D484D),
        keyboardSurface = Color(0xFF2D353B),
        keyboardContainer = Color(0xFF343F44),
        keyboardContainerVariant = Color(0xFF3D484D),
        onKeyboardContainer = Color(0xFFF2E8D0),
        keyboardPress = Color(0xFF475258),
        keyboardFade0 = Color(0xFF2D353B),
        keyboardFade1 = Color(0xFF2D353B),
        primaryTransparent = Color(0xFFA7C080).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFF2E8D0).copy(alpha = 0.1f),
        shadow = Color(0xFF232A2E).copy(alpha = 0.7f),
    )
}

// Colors: Kanagawa (github.com/rebelot/kanagawa.nvim, MIT)
val Kanagawa = ThemeOption(
    dynamic = false,
    key = "Kanagawa",
    name = R.string.theme_kanagawa,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFF7E9CD8),
        onPrimary = Color(0xFF16161D),
        primaryContainer = Color(0xFF3A4259),
        onPrimaryContainer = Color(0xFFC4C8C2),
        secondary = Color(0xFF957FB8),
        onSecondary = Color(0xFF16161D),
        secondaryContainer = Color(0xFF3C374C),
        onSecondaryContainer = Color(0xFFCEC5BA),
        tertiary = Color(0xFFE6C384),
        onTertiary = Color(0xFF16161D),
        tertiaryContainer = Color(0xFF51483F),
        onTertiaryContainer = Color(0xFFDED3AF),
        error = Color(0xFFE82424),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF652127),
        onErrorContainer = Color(0xFFDEBCA4),
        outline = Color(0xFFC8C093),
        outlineVariant = Color(0xFF363646),
        surface = Color(0xFF16161D),
        onSurface = Color(0xFFDCD7BA),
        onSurfaceVariant = Color(0xFFC8C093),
        surfaceContainerHighest = Color(0xFF363646),
        keyboardSurface = Color(0xFF1F1F28),
        keyboardContainer = Color(0xFF2A2A37),
        keyboardContainerVariant = Color(0xFF363646),
        onKeyboardContainer = Color(0xFFDCD7BA),
        keyboardPress = Color(0xFF54546D),
        keyboardFade0 = Color(0xFF1F1F28),
        keyboardFade1 = Color(0xFF1F1F28),
        primaryTransparent = Color(0xFF7E9CD8).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFDCD7BA).copy(alpha = 0.1f),
        shadow = Color(0xFF16161D).copy(alpha = 0.7f),
    )
}

// Colors: Monokai (monokai.nl), classic colors
val Monokai = ThemeOption(
    dynamic = false,
    key = "Monokai",
    name = R.string.theme_monokai,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFFA6E22E),
        onPrimary = Color(0xFF1E1F1C),
        primaryContainer = Color(0xFF4B5C25),
        onPrimaryContainer = Color(0xFFE4F2C1),
        secondary = Color(0xFFF92672),
        onSecondary = Color(0xFF1E1F1C),
        secondaryContainer = Color(0xFF5C2836),
        onSecondaryContainer = Color(0xFFF8CED8),
        tertiary = Color(0xFF66D9EF),
        onTertiary = Color(0xFF1E1F1C),
        tertiaryContainer = Color(0xFF375455),
        onTertiaryContainer = Color(0xFFDBF2F1),
        error = Color(0xFFF92672),
        onError = Color(0xFF1E1F1C),
        errorContainer = Color(0xFF70273E),
        onErrorContainer = Color(0xFFF8D8DF),
        outline = Color(0xFFCFCFC2),
        outlineVariant = Color(0xFF3E3D32),
        surface = Color(0xFF1E1F1C),
        onSurface = Color(0xFFF8F8F2),
        onSurfaceVariant = Color(0xFFCFCFC2),
        surfaceContainerHighest = Color(0xFF3E3D32),
        keyboardSurface = Color(0xFF272822),
        keyboardContainer = Color(0xFF34352D),
        keyboardContainerVariant = Color(0xFF3E3D32),
        onKeyboardContainer = Color(0xFFF8F8F2),
        keyboardPress = Color(0xFF49483E),
        keyboardFade0 = Color(0xFF272822),
        keyboardFade1 = Color(0xFF272822),
        primaryTransparent = Color(0xFFA6E22E).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFF8F8F2).copy(alpha = 0.1f),
        shadow = Color(0xFF1E1F1C).copy(alpha = 0.7f),
    )
}

// Colors: own design, pure black for OLED screens
val AmoledCyan = ThemeOption(
    dynamic = false,
    key = "AmoledCyan",
    name = R.string.theme_amoled_cyan,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFF00E5FF),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF004047),
        onPrimaryContainer = Color(0xFFB6EFF5),
        secondary = Color(0xFF18FFFF),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF064040),
        onSecondaryContainer = Color(0xFFC6F5F5),
        tertiary = Color(0xFF76FF03),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF1E4001),
        onTertiaryContainer = Color(0xFFD9F5C2),
        error = Color(0xFFFF5252),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFF591D1D),
        onErrorContainer = Color(0xFFF4DADA),
        outline = Color(0xFFB8B8B8),
        outlineVariant = Color(0xFF1F1F1F),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFF2F2F2),
        onSurfaceVariant = Color(0xFFB8B8B8),
        surfaceContainerHighest = Color(0xFF1F1F1F),
        keyboardSurface = Color(0xFF000000),
        keyboardContainer = Color(0xFF141414),
        keyboardContainerVariant = Color(0xFF1F1F1F),
        onKeyboardContainer = Color(0xFFF2F2F2),
        keyboardPress = Color(0xFF2E2E2E),
        keyboardFade0 = Color(0xFF000000),
        keyboardFade1 = Color(0xFF000000),
        primaryTransparent = Color(0xFF00E5FF).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFF2F2F2).copy(alpha = 0.1f),
        shadow = Color(0xFF000000).copy(alpha = 0.7f),
    )
}

// Colors: own design, pure black for OLED screens
val AmoledAmber = ThemeOption(
    dynamic = false,
    key = "AmoledAmber",
    name = R.string.theme_amoled_amber,
    available = { true }
) {
    extendedDarkColorScheme(
        primary = Color(0xFFFFB300),
        onPrimary = Color(0xFF000000),
        primaryContainer = Color(0xFF473200),
        onPrimaryContainer = Color(0xFFF5E2B6),
        secondary = Color(0xFFFF8F00),
        onSecondary = Color(0xFF000000),
        secondaryContainer = Color(0xFF402400),
        onSecondaryContainer = Color(0xFFF5DEC2),
        tertiary = Color(0xFFFFD54F),
        onTertiary = Color(0xFF000000),
        tertiaryContainer = Color(0xFF403514),
        onTertiaryContainer = Color(0xFFF5ECD1),
        error = Color(0xFFFF5252),
        onError = Color(0xFF000000),
        errorContainer = Color(0xFF591D1D),
        onErrorContainer = Color(0xFFF4DADA),
        outline = Color(0xFFB8B8B8),
        outlineVariant = Color(0xFF1F1F1F),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFF2F2F2),
        onSurfaceVariant = Color(0xFFB8B8B8),
        surfaceContainerHighest = Color(0xFF1F1F1F),
        keyboardSurface = Color(0xFF000000),
        keyboardContainer = Color(0xFF141414),
        keyboardContainerVariant = Color(0xFF1F1F1F),
        onKeyboardContainer = Color(0xFFF2F2F2),
        keyboardPress = Color(0xFF2E2E2E),
        keyboardFade0 = Color(0xFF000000),
        keyboardFade1 = Color(0xFF000000),
        primaryTransparent = Color(0xFFFFB300).copy(alpha = 0.3f),
        onSurfaceTransparent = Color(0xFFF2F2F2).copy(alpha = 0.1f),
        shadow = Color(0xFF000000).copy(alpha = 0.7f),
    )
}

@Composable
@Preview
private fun PreviewDarkThemeCollection() {
    ThemePreview(Dracula)
}
