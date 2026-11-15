package org.futo.inputmethod.latin.uix.settings.pages

import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.content.getSystemService
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.ANIMATE_BUBBLE
import org.futo.inputmethod.latin.uix.AUDIO_FOCUS
import org.futo.inputmethod.latin.uix.CAN_EXPAND_SPACE
import org.futo.inputmethod.latin.uix.DISALLOW_SYMBOLS
import org.futo.inputmethod.latin.uix.ENABLE_SOUND
import org.futo.inputmethod.latin.uix.PREFER_BLUETOOTH
import org.futo.inputmethod.latin.uix.SYSTEM_VOICE_INPUT_PACKAGE
import org.futo.inputmethod.latin.uix.SYSTEM_VOICE_INPUT_POPUP
import org.futo.inputmethod.latin.uix.USE_PERSONAL_DICT
import org.futo.inputmethod.latin.uix.USE_SYSTEM_VOICE_INPUT
import org.futo.inputmethod.latin.uix.USE_VAD_AUTOSTOP
import org.futo.inputmethod.latin.uix.actions.LEGACY_POPUP_VOICE_PACKAGE
import org.futo.inputmethod.latin.uix.actions.findRecognizeSpeechActivities
import org.futo.inputmethod.latin.uix.settings.DropDownPickerSettingItem
import org.futo.inputmethod.latin.uix.settings.NavigationItemStyle
import org.futo.inputmethod.latin.uix.settings.Tip
import org.futo.inputmethod.latin.uix.settings.UserSetting
import org.futo.inputmethod.latin.uix.settings.UserSettingsMenu
import org.futo.inputmethod.latin.uix.settings.useDataStore
import org.futo.inputmethod.latin.uix.settings.useDataStoreValue
import org.futo.inputmethod.latin.uix.settings.userSettingNavigationItem
import org.futo.inputmethod.latin.uix.settings.userSettingToggleDataStore

private val visibilityCheckNotSystemVoiceInput = @Composable {
    useDataStoreValue(USE_SYSTEM_VOICE_INPUT) == false
}

private data class VoiceIMEInfo(
    val builtin: Boolean,
    val name: String,
    val packageName: String,
    // Futo Keyboard M: App mit RECOGNIZE_SPEECH-Popup statt Sprach-Tastatur (IME)
    val popup: Boolean = false,
)

@Composable
fun usePackageReadableName(pkg: String): String? {
    val context = LocalContext.current
    return remember(pkg) {
        try {
            context.packageManager.getPackageInfo(pkg, 0)
        } catch(e: Exception) {
            null
        }?.applicationInfo?.let {
            context.packageManager.getApplicationLabel(it).toString()
        }
    }
}

val VoiceInputMenu = UserSettingsMenu(
    title = R.string.voice_input_settings_title,
    navPath = "voiceInput", registerNavPath = true,
    settings = listOf(
        UserSetting(
            name = R.string.voice_input_settings_backend_system,
            searchTagList = listOf(
                R.string.voice_input_settings_disable_builtin_voice_input,
                R.string.voice_input_settings_disable_builtin_voice_input_subtitle
            )
        ) {
            val useExternal = useDataStore(USE_SYSTEM_VOICE_INPUT)
            val externalPkg = useDataStore(SYSTEM_VOICE_INPUT_PACKAGE)
            val externalPopup = useDataStore(SYSTEM_VOICE_INPUT_POPUP)

            val context = LocalContext.current
            val res = LocalResources.current
            val options = remember(externalPkg.value) {
                val imm = context.getSystemService<InputMethodManager>()!!
                buildList {
                    add(VoiceIMEInfo(true, "", ""))
                    addAll(imm.enabledInputMethodList.filter { im ->
                        im.packageName == externalPkg.value ||
                            (0 until im.subtypeCount).map { im.getSubtypeAt(it) }
                                .any { it.mode.lowercase() == "voice" }
                    }.map {
                        VoiceIMEInfo(false, it.loadLabel(context.packageManager)?.toString() ?: it.packageName, it.packageName)
                    })

                    // Futo Keyboard M: Apps, die nur einen Spracheingabe-Popup anbieten (keine Tastatur)
                    val imePackages = map { it.packageName }.toSet()
                    addAll(findRecognizeSpeechActivities(context).map {
                        val label = it.applicationInfo.loadLabel(context.packageManager).toString()
                        VoiceIMEInfo(
                            false,
                            if(it.packageName in imePackages) "$label (Popup)" else label,
                            it.packageName,
                            popup = true
                        )
                    })
                }
            }

            // Altinstallationen: externe Eingabe an, aber kein Paket gewaehlt -> frueher fest verdrahtete Popup-App
            val effectivePkg = if(useExternal.value && externalPkg.value == "") LEGACY_POPUP_VOICE_PACKAGE else externalPkg.value
            val effectivePopup = externalPopup.value || (useExternal.value && externalPkg.value == "")
            val currOption = remember(options, useExternal.value, effectivePkg, effectivePopup) {
                if(!useExternal.value) options[0] else
                options.find { !it.builtin && it.packageName == effectivePkg && it.popup == effectivePopup }
            }


            DropDownPickerSettingItem(
                stringResource(R.string.voice_input_settings_backend_system),
                options,
                currOption,
                {
                    useExternal.setValue(!it.builtin)
                    externalPkg.setValue(it.packageName ?: "")
                    externalPopup.setValue(it.popup)
                },
                {
                    if(it.builtin) res.getString(R.string.voice_input_settings_backend_system_internal)
                    else it.name
                }
            )

            if(useExternal.value && effectivePkg.isNotEmpty()) {
                val privacyWhitelist = listOf(
                    "org.futo.voiceinput",
                    "org.futo.voiceinput.dev",
                    "dev.notune.transcribe",
                    "dev.soupslurpr.transcribro"
                )

                if(!privacyWhitelist.contains(effectivePkg))
                    Tip(stringResource(R.string.voice_input_settings_backend_system_external_warning,
                        currOption?.name ?: effectivePkg))
            }
        },

        //if(!systemVoiceInput.value) {
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_indication_sounds,
            subtitle = R.string.voice_input_settings_indication_sounds_subtitle,
            setting = ENABLE_SOUND
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        /*
        userSettingToggleDataStore(
            title = R.string.voice_input_settings_verbose_progress,
            subtitle = R.string.voice_input_settings_verbose_progress_subtitle,
            setting = VERBOSE_PROGRESS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),
         */

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_use_personal_dict,
            subtitle = R.string.voice_input_settings_use_personal_dict_subtitle,
            setting = USE_PERSONAL_DICT
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_use_bluetooth_mic,
            subtitle = R.string.voice_input_settings_use_bluetooth_mic_subtitle,
            setting = PREFER_BLUETOOTH
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_audio_focus,
            subtitle = R.string.voice_input_settings_audio_focus_subtitle,
            setting = AUDIO_FOCUS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_suppress_symbols,
            setting = DISALLOW_SYMBOLS
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_long_form,
            subtitle = R.string.voice_input_settings_long_form_subtitle,
            setting = CAN_EXPAND_SPACE
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_autostop_vad,
            subtitle = R.string.voice_input_settings_autostop_vad_subtitle,
            setting = USE_VAD_AUTOSTOP
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingToggleDataStore(
            title = R.string.voice_input_settings_animate_bubble,
            subtitle = R.string.voice_input_settings_animate_bubble_subtitle,
            setting = ANIMATE_BUBBLE
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),

        userSettingNavigationItem(
            title = R.string.voice_input_settings_change_models,
            subtitle = R.string.voice_input_settings_change_models_subtitle,
            style = NavigationItemStyle.Misc,
            navigateTo = "languages"
        ).copy(visibilityCheck = visibilityCheckNotSystemVoiceInput),
        //}
    )
)