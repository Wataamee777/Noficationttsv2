package com.example.notificationttsv2

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * 通知を監視し、対象アプリの通知内容をTTSで読み上げるサービス。
 */
class NotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        super.onDestroy()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.JAPANESE)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                Log.w(TAG, "Japanese locale is not supported by this TTS engine.")
            }
        } else {
            ttsReady = false
            Log.e(TAG, "TextToSpeech initialization failed: $status")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (!ttsReady) return

        val packageName = sbn.packageName
        val prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean("enabled_$packageName", true)
        if (!enabled) return

        val notification = sbn.notification
        val extras = notification.extras

        val appName = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        }.getOrDefault(packageName)

        val title = extras.getCharSequence("android.title")?.toString()?.trim().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()

        // タイトル・本文の両方が空なら読み上げ対象外。
        if (title.isBlank() && text.isBlank()) return

        val speechText = buildString {
            append(appName)
            append("。")
            if (title.isNotBlank()) {
                append(title)
                append("。")
            }
            if (text.isNotBlank()) {
                append(text)
            }
        }

        tts?.speak(
            speechText,
            TextToSpeech.QUEUE_ADD,
            null,
            "notif_${sbn.postTime}"
        )
    }

    companion object {
        private const val TAG = "NotificationService"
    }
}
