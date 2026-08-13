package org.futo.inputmethod.latin.uix.actions

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log

/**
 * Transparente Proxy-Activity fuer den RECOGNIZE_SPEECH-Popup-Weg (B1-Patch).
 *
 * Startet die RecognizeActivity der Offline-Voice-App per startActivityForResult
 * und reicht das Ergebnis (EXTRA_RESULTS) ueber [VoiceResultBridge] an den
 * Mic-Key-Action weiter, der es ins aktive Textfeld committed.
 *
 * Wichtig: eigene taskAffinity (leerer String) — sonst laendet die Activity in
 * der Settings-Task und wird nie resumed, wenn das Popup endet, und
 * onActivityResult feuert nie.
 */
class VoiceResultProxyActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "onCreate")
        val target = ComponentName(
            "dev.notune.transcribe",
            "dev.notune.transcribe.RecognizeActivity"
        )
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .setComponent(target)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        try {
            startActivityForResult(intent, REQ_RECOGNIZE)
        } catch (e: Exception) {
            Log.e(TAG, "startActivityForResult failed", e)
            VoiceResultBridge.onText = null
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(TAG, "onActivityResult req=$requestCode res=$resultCode data=${data != null}")
        var textToCommit: String? = null
        if (requestCode == REQ_RECOGNIZE && resultCode == RESULT_OK && data != null) {
            val text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull().orEmpty()
            Log.i(TAG, "text='$text'")
            if (text.isNotBlank()) {
                textToCommit = text
            }
        }
        // Erst schliessen, damit der Fokus zurueck ins Textfeld wandert und die
        // InputConnection wieder aktiv ist — dann committen (sonst stille No-Op).
        finish()
        val text = textToCommit
        if (text != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    VoiceResultBridge.onText?.invoke(text)
                    Log.i(TAG, "bridge invoked")
                } catch (e: Exception) {
                    Log.e(TAG, "bridge failed", e)
                }
            }, COMMIT_DELAY_MS)
        } else {
            Log.i(TAG, "no result -> clear bridge")
            VoiceResultBridge.onText = null
        }
    }

    companion object {
        private const val TAG = "FutoShizukuVoice"
        private const val REQ_RECOGNIZE = 1001
        private const val COMMIT_DELAY_MS = 500L
    }
}
