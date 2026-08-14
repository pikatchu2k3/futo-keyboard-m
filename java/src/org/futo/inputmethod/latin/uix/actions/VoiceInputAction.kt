package org.futo.inputmethod.latin.uix.actions

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.futo.inputmethod.latin.LatinIME
import org.futo.inputmethod.latin.MicPermissionActivity
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.ANIMATE_BUBBLE
import org.futo.inputmethod.latin.uix.AUDIO_FOCUS
import org.futo.inputmethod.latin.uix.Action
import org.futo.inputmethod.latin.uix.ActionWindow
import org.futo.inputmethod.latin.uix.CAN_EXPAND_SPACE
import org.futo.inputmethod.latin.uix.CloseResult
import org.futo.inputmethod.latin.uix.DISALLOW_SYMBOLS
import org.futo.inputmethod.latin.uix.ENABLE_SOUND
import org.futo.inputmethod.latin.uix.KeyboardManagerForAction
import org.futo.inputmethod.latin.uix.PREFER_BLUETOOTH
import org.futo.inputmethod.latin.uix.PersistentActionState
import org.futo.inputmethod.latin.uix.ResourceHelper
import org.futo.inputmethod.latin.uix.USE_PERSONAL_DICT
import org.futo.inputmethod.latin.uix.USE_VAD_AUTOSTOP
import org.futo.inputmethod.latin.uix.VERBOSE_PROGRESS
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.setSetting
import org.futo.inputmethod.latin.uix.settings.SettingsActivity
import org.futo.inputmethod.latin.uix.utils.ModelOutputSanitizer
import org.futo.inputmethod.latin.uix.utils.TextContext
import org.futo.inputmethod.latin.xlm.UserDictionaryObserver
import org.futo.inputmethod.updates.openURI
import org.futo.voiceinput.shared.ModelDoesNotExistException
import org.futo.voiceinput.shared.RecognizerView
import org.futo.voiceinput.shared.RecognizerViewListener
import org.futo.voiceinput.shared.RecognizerViewSettings
import org.futo.voiceinput.shared.RecordingSettings
import org.futo.voiceinput.shared.SoundPlayer
import org.futo.voiceinput.shared.types.Language
import org.futo.voiceinput.shared.types.ModelLoader
import org.futo.voiceinput.shared.types.getLanguageFromWhisperString
import org.futo.voiceinput.shared.ui.MicrophoneDeviceState
import org.futo.voiceinput.shared.whisper.DecodingConfiguration
import org.futo.voiceinput.shared.whisper.ModelManager
import org.futo.voiceinput.shared.whisper.MultiModelRunConfiguration
import java.util.Locale

/**
 * B1-Patch (Futo Shizuku): Spracherkennung ueber den RECOGNIZE_SPEECH-Popup-Weg.
 *
 * Statt auf die System-Voice-IME umzuschalten (switchToShortcutIme) oeffnet der
 * Mic-Key direkt das Popup der Offline-Voice-App (RecognizeActivity). Die App
 * nimmt mit IHRER eigenen Mikrofon-Berechtigung auf — Futo Shizuku braucht KEIN
 * RECORD_AUDIO, kein System-Default-Setting, kein Shizuku/ADB. Das Ergebnis wird
 * ueber die Proxy-Activity zurueckgegeben und ins aktive Textfeld committed.
 * (Android-15-Hintergrund: ein IME ist nie "foreground", SpeechRecognizer-Start
 * aus der Tastatur wird vom Framework abgelehnt — der Popup-Weg umgeht das.)
 */
private const val SYSTEM_VOICE_TAG = "FutoShizukuVoice"

/** Bruecke zwischen der Proxy-Activity und dem Mic-Key-Action. */
object VoiceResultBridge {
    var onText: ((String) -> Unit)? = null
}

val SystemVoiceInputAction = Action(
    icon = R.drawable.mic_fill,
    name = R.string.action_system_voice_input_title,
    simplePressImpl = { manager, _ ->
        val context = manager.getContext()

        val target = ComponentName(
            "dev.notune.transcribe",
            "dev.notune.transcribe.RecognizeActivity"
        )
        val available = context.packageManager.resolveActivity(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).setComponent(target),
            0
        ) != null
        if (!available) {
            manager.announce("Offline Voice Input ist nicht installiert")
            return@Action
        }

        VoiceResultBridge.onText = { text ->
            try {
                // Robuster Commit mit Retry: nach dem Popup muss der Fokus erst zurueck
                // ins Feld wandern, bevor die InputConnection wieder aktiv ist.
                commitVoiceResultWithRetry(manager, text)
            } catch (e: Exception) {
                Log.w(SYSTEM_VOICE_TAG, "commit failed", e)
            } finally {
                VoiceResultBridge.onText = null
            }
        }

        try {
            // Die transparente Proxy-Activity uebernimmt startActivityForResult auf die
            // RecognizeActivity (singleTask!) und reicht das Ergebnis ueber die Bridge zurueck.
            context.startActivity(
                Intent(context, VoiceResultProxyActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            Log.e(SYSTEM_VOICE_TAG, "proxy start failed", e)
            VoiceResultBridge.onText = null
            manager.announce("Spracherkennung konnte nicht gestartet werden")
        }
    },
    persistentState = null,
    windowImpl = null,
    shownInEditor = false
)

/**
 * Committet das Spracherkennungs-Ergebnis robust ins aktive Textfeld.
 *
 * Hintergrund: Nach dem RECOGNIZE_SPEECH-Popup muss der Fokus erst zurueck ins Feld
 * wandern, bevor die InputConnection wieder aktiv ist. In nativen Apps (Signal) reichen
 * dafuer ~500 ms, in WebView-Feldern (Browser, Amazon-Suche) dauert das deutlich laenger —
 * ein zu frueher commitText geht an eine tote/stale Connection und wird STILL verworfen
 * (kein Fehler, kein Text). Deshalb: Retry-Schleife mit Verifikation ueber getTextBeforeCursor.
 * Abbruchkriterium Doppel-Commit: Vor jedem Commit pruefen, ob der Text schon im Feld steht.
 */
private fun commitVoiceResultWithRetry(manager: KeyboardManagerForAction, rawText: String) {
    val handler = Handler(Looper.getMainLooper())
    val maxAttempts = 10
    val maxCommits = 5
    val delayFirstMs = 150L
    val delayRetryMs = 250L
    val verifyDelayMs = 120L
    var attempt = 0
    var commitCalls = 0
    var lastCommitIC: InputConnection? = null

    fun rawIC(): InputConnection? =
        (manager.getContext() as? LatinIME)?.currentInputConnection

    lateinit var attemptOnce: () -> Unit

    fun scheduleNext(delayMs: Long) {
        handler.postDelayed({ attemptOnce() }, delayMs)
    }

    fun scheduleVerify(sanitized: String, tag: String) {
        handler.postDelayed({
            val check = try { rawIC()?.getTextBeforeCursor(512, 0)?.toString().orEmpty() }
                catch (e: Exception) { "" }
            if (check.contains(sanitized)) {
                Log.i(SYSTEM_VOICE_TAG, "verified ($tag)")
                manager.announce(rawText)
            } else {
                Log.w(SYSTEM_VOICE_TAG, "not verified ($tag), retrying")
                scheduleNext(delayRetryMs)
            }
        }, verifyDelayMs)
    }

    attemptOnce = lambda@{
        attempt++
        if (attempt > maxAttempts) {
            Log.w(SYSTEM_VOICE_TAG, "commit failed after $maxAttempts attempts (ic=${rawIC() != null})")
            manager.announce("Spracherkennung: Text konnte nicht eingefügt werden")
            return@lambda
        }

        // KEIN createInputTransaction() hier! Das wirft im Unstable-Build (throwIfDebug +
        // FLAVOR=unstable) eine IllegalStateException, wenn der IME nach dem Popup noch
        // nicht wieder im Input-Zustand ist (inInput=false) — genau in der Retry-Phase.
        // Der textContext (nur fuer Sanitize) wird deshalb direkt aus der rohen Connection
        // gebaut; der Commit laeuft ebenfalls direkt auf ihr (Boolean verwertbar, kein Wrapper).
        val ic = rawIC()
        val before = try { ic?.getTextBeforeCursor(512, 0)?.toString() } catch (e: Exception) { null }
        val after = try { ic?.getTextAfterCursor(512, 0)?.toString() } catch (e: Exception) { null }
        val sanitized = ModelOutputSanitizer.sanitize(rawText, TextContext(before, after))

        if (sanitized.isBlank()) return@lambda

        if (ic == null) {
            Log.w(SYSTEM_VOICE_TAG, "no input connection yet (attempt $attempt)")
            scheduleNext(delayRetryMs)
            return@lambda
        }

        // Text evtl. schon von einem frueheren Versuch angekommen? (Schutz vor Doppel-Commit)
        val existing = before ?: ""
        if (existing.contains(sanitized)) {
            Log.i(SYSTEM_VOICE_TAG, "text already present, commit not needed")
            manager.announce(rawText)
            return@lambda
        }

        // Commit-Cap (Sicherheitsnetz, falls die Instanz-Heuristik unten versagt).
        if (commitCalls >= maxCommits) {
            Log.w(SYSTEM_VOICE_TAG, "commit cap reached, only verifying now")
            scheduleVerify(sanitized, "cap")
            return@lambda
        }

        // Instanz-Heuristik: Nur auf einer NEUEN InputConnection committen. WebViews
        // (Chromium) erzeugen nach dem Resume eine NEUE Connection-Instanz — die alte
        // war tot, der Commit wurde verworfen, ein neuer Versuch ist gerechtfertigt.
        // Gleiche Instanz wie beim letzten Commit = Text ist entweder schon drin
        // (blinde Verifikation) oder die Connection ist unheilbar tot — in beiden
        // Faellen wuerde ein erneuter Commit nur schaden (Doppel-Text).
        if (commitCalls > 0 && ic === lastCommitIC) {
            Log.w(SYSTEM_VOICE_TAG, "same connection as last commit, verifying only")
            scheduleVerify(sanitized, "same-ic")
            return@lambda
        }

        commitCalls++
        lastCommitIC = ic
        val ok = try {
            ic.beginBatchEdit()
            val r = ic.commitText(sanitized, 1)
            ic.endBatchEdit()
            r
        } catch (e: Exception) {
            Log.w(SYSTEM_VOICE_TAG, "commit attempt $attempt failed", e)
            false
        }

        if (!ok) {
            Log.w(SYSTEM_VOICE_TAG, "commitText returned false (attempt $attempt), retrying")
            scheduleNext(delayRetryMs)
            return@lambda
        }

        // Verifikation: kurz warten, dann pruefen ob der Text wirklich im Feld steht.
        scheduleVerify(sanitized, "attempt $attempt")
    }

    scheduleNext(delayFirstMs)
}



@Composable
fun NoModelInstalled(locale: Locale) {
    val context = LocalContext.current
    Box(modifier = Modifier
        .fillMaxSize()
        .clickable(
            enabled = true,
            onClickLabel = null,
            onClick = {
                context.openURI("https://keyboard.futo.tech/voice-input-models", true)
            },
            role = null,
            indication = null,
            interactionSource = remember { MutableInteractionSource() })) {
        Text(
            stringResource(
                R.string.action_voice_input_no_model_for_language_x_installed,
                locale.getDisplayName(locale)
            ), modifier = Modifier
                .align(Alignment.Center)
                .padding(8.dp), textAlign = TextAlign.Center)
    }
}

class VoiceInputPersistentState(val manager: KeyboardManagerForAction) : PersistentActionState {
    val modelManager = ModelManager(manager.getContext())
    val soundPlayer = SoundPlayer(manager.getContext())
    val userDictionaryObserver = UserDictionaryObserver(manager.getContext())

    override suspend fun cleanUp() {
        modelManager.cleanUp()
    }

    override fun close() {
        runBlocking { modelManager.cleanUp() }
        userDictionaryObserver.unregister()
    }
}

private class VoiceInputActionWindow(
    val manager: KeyboardManagerForAction, val state: VoiceInputPersistentState,
    val model: ModelLoader, val locales: List<Locale>
) : ActionWindow(), RecognizerViewListener {
    val context = manager.getContext()

    private var shouldPlaySounds: Boolean = false
    private fun loadSettings(): RecognizerViewSettings {
        val enableSound = context.getSetting(ENABLE_SOUND)
        val verboseFeedback = false//context.getSetting(VERBOSE_PROGRESS)
        val disallowSymbols = context.getSetting(DISALLOW_SYMBOLS)
        val useBluetoothAudio = context.getSetting(PREFER_BLUETOOTH)
        val requestAudioFocus = context.getSetting(AUDIO_FOCUS)
        val canExpandSpace = context.getSetting(CAN_EXPAND_SPACE)
        val useVAD = context.getSetting(USE_VAD_AUTOSTOP)
        val usePersonalDict = context.getSetting(USE_PERSONAL_DICT)
        val animateBubble = context.getSetting(ANIMATE_BUBBLE)

        val primaryModel = model
        val languageSpecificModels = mutableMapOf<Language, ModelLoader>()
        val allowedLanguages = locales.mapNotNull { getLanguageFromWhisperString(it.language) }.toSet()
        val glossary = if(usePersonalDict) {
            state.userDictionaryObserver.getWords(locales).filter { it.shortcut.isNullOrEmpty() }.map { it.word }
        } else {
            emptyList()
        }

        shouldPlaySounds = enableSound

        return RecognizerViewSettings(
            shouldShowInlinePartialResult = false,
            shouldShowVerboseFeedback = verboseFeedback,
            shouldAnimateBubble = animateBubble,
            modelRunConfiguration = MultiModelRunConfiguration(
                primaryModel = primaryModel,
                languageSpecificModels = languageSpecificModels
            ),
            decodingConfiguration = DecodingConfiguration(
                glossary = glossary,
                languages = allowedLanguages,
                suppressSymbols = disallowSymbols
            ),
            recordingConfiguration = RecordingSettings(
                preferBluetoothMic = useBluetoothAudio,
                requestAudioFocus = requestAudioFocus,
                canExpandSpace = canExpandSpace,
                useVADAutoStop = useVAD
            )
        )
    }

    private var recognizerView: MutableState<RecognizerView?> = mutableStateOf(null)
    private var modelException: MutableState<ModelDoesNotExistException?> = mutableStateOf(null)

    private val initJob = manager.getLifecycleScope().launch(Dispatchers.Default) {
        yield()
        val settings = loadSettings()

        yield()
        val recognizerView = try {
            RecognizerView(
                context = manager.getContext(),
                listener = this@VoiceInputActionWindow,
                settings = settings,
                lifecycleScope = manager.getLifecycleScope(),
                modelManager = state.modelManager
            )
        } catch(e: ModelDoesNotExistException) {
            modelException.value = e
            return@launch
        }

        this@VoiceInputActionWindow.recognizerView.value = recognizerView

        //yield()
        recognizerView.reset()

        //yield()
        recognizerView.start()
    }

    private var inputTransaction = manager.createInputTransaction()

    @Composable
    private fun ModelDownloader(modelException: ModelDoesNotExistException) {
        NoModelInstalled(locales.firstOrNull() ?: Locale.ROOT)
    }

    @Composable
    override fun windowName(): String {
        return stringResource(R.string.action_voice_input_title)
    }

    @Composable
    override fun WindowContents(keyboardShown: Boolean) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = true,
                onClickLabel = null,
                onClick = { recognizerView.value?.finish() },
                role = null,
                indication = null,
                interactionSource = remember { MutableInteractionSource() })
            .semantics(mergeDescendants = true) {
                traversalIndex = -1.0f
            }) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                when {
                    modelException.value != null -> ModelDownloader(modelException.value!!)
                    recognizerView.value != null -> recognizerView.value!!.Content()
                }
            }
        }
    }

    override fun close(): CloseResult {
        inputTransaction.cancel()
        runBlocking { initJob.cancelAndJoin() }
        recognizerView.value?.cancel()
        state.modelManager.cancelAll()
        return CloseResult.Default
    }

    private var wasFinished = false
    private var cancelPlayed = false
    override fun cancelled() {
        if (!wasFinished) {
            if (shouldPlaySounds && !cancelPlayed) {
                state.soundPlayer.playCancelSound()
                cancelPlayed = true
            }
            inputTransaction.cancel()
        }
    }

    override fun recordingStarted(device: MicrophoneDeviceState) {
        if (shouldPlaySounds) {
            state.soundPlayer.playStartSound()
        }

        // Only set the setting if bluetooth is available, else it would reset the setting
        // every time it's used without a bluetooth device connected.
        if(device.bluetoothAvailable) {
            manager.getLifecycleScope().launch {
                context.setSetting(PREFER_BLUETOOTH, device.bluetoothActive)
            }
        }
    }

    override fun finished(result: String) {
        wasFinished = true

        manager.getLifecycleScope().launch(Dispatchers.Main) {
            val sanitized = ModelOutputSanitizer.sanitize(result, inputTransaction.textContext)
            inputTransaction.commit(sanitized)
            manager.announce(result)
            manager.closeActionWindow()
        }
    }

    override fun partialResult(result: String) {
        manager.getLifecycleScope().launch(Dispatchers.Main) {
            val sanitized = ModelOutputSanitizer.sanitize(result, inputTransaction.textContext)
            inputTransaction.updatePartial(sanitized)
        }
    }

    override fun requestPermission(onGranted: () -> Unit, onRejected: () -> Unit): Boolean {
        return false
    }

    override fun openSettings() {
        SettingsActivity.openToNavDest(context, "languages")
    }
}

private class VoiceInputNoModelWindow(val locale: Locale) : ActionWindow() {
    @Composable
    override fun windowName(): String {
        return stringResource(R.string.action_voice_input_title)
    }

    @Composable
    override fun WindowContents(keyboardShown: Boolean) {
        NoModelInstalled(locale)
    }
}

val VoiceInputAction = Action(icon = R.drawable.mic_fill,
    name = R.string.action_voice_input_title,
    simplePressImpl = null,
    keepScreenAwake = true,
    persistentState = { VoiceInputPersistentState(it) },
    windowImpl = { manager, persistentState ->
        val locales = manager.getActiveLocales()

        val model = ResourceHelper.tryFindingVoiceInputModelForLocale(manager.getContext(), locales.firstOrNull() ?: Locale.ROOT)

        if(model == null) {
            VoiceInputNoModelWindow(locales.firstOrNull() ?: Locale.ROOT)
        } else {
            VoiceInputActionWindow(
                manager = manager, state = persistentState as VoiceInputPersistentState,
                locales = locales, model = model
            )
        }
    }
)