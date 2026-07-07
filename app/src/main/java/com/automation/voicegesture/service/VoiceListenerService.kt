package com.automation.voicegesture.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.automation.voicegesture.MainActivity
import com.automation.voicegesture.R
import com.automation.voicegesture.data.AppDatabase
import com.automation.voicegesture.data.Automation
import com.automation.voicegesture.data.GestureSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the microphone listening for the user's trigger words,
 * including while the screen is locked. Android allows a foreground service declared with
 * foregroundServiceType="microphone" to keep recording as long as its notification is showing,
 * regardless of screen state, which is what makes lock-screen recognition possible here.
 */
class VoiceListenerService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isListening = false
    private var isStopping = false

    private val lastTriggerAtByWord = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        startListeningLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isStopping = true
        mainHandler.removeCallbacksAndMessages(null)
        speechRecognizer?.destroy()
        speechRecognizer = null
        wakeLock?.let { if (it.isHeld) it.release() }
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VoiceGesture:listening"
        ).apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun refreshWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
            it.acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun startListeningLoop() {
        if (isListening || isStopping) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available on this device")
            stopSelf()
            return
        }

        refreshWakeLock()

        val recognizer = speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(this).also {
            it.setRecognitionListener(recognitionListener)
            speechRecognizer = it
        }

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
        }

        isListening = true
        recognizer.startListening(recognizerIntent)
    }

    private fun restartListeningAfterDelay(delayMs: Long) {
        isListening = false
        if (isStopping) return
        mainHandler.postDelayed({ startListeningLoop() }, delayMs)
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            // A final result or error will follow; the loop restarts from there.
        }

        override fun onError(error: Int) {
            val delay = when (error) {
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> RETRY_DELAY_BUSY_MS
                else -> RETRY_DELAY_DEFAULT_MS
            }
            restartListeningAfterDelay(delay)
        }

        override fun onResults(results: Bundle?) {
            handleRecognizedText(results)
            restartListeningAfterDelay(RETRY_DELAY_DEFAULT_MS)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            handleRecognizedText(partialResults)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleRecognizedText(bundle: Bundle?) {
        val candidates = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        if (candidates.isEmpty()) return
        val recognizedText = candidates[0].lowercase().trim()
        if (recognizedText.isEmpty()) return

        serviceScope.launch {
            val automations = AppDatabase.getInstance(this@VoiceListenerService)
                .automationDao()
                .getAll()
            matchAndTrigger(recognizedText, automations)
        }
    }

    private fun matchAndTrigger(recognizedText: String, automations: List<Automation>) {
        val now = System.currentTimeMillis()
        for (automation in automations) {
            val word = automation.triggerWord.lowercase().trim()
            if (word.isEmpty() || !recognizedText.contains(word)) continue

            val lastTriggered = lastTriggerAtByWord[word] ?: 0L
            if (now - lastTriggered < TRIGGER_DEBOUNCE_MS) continue
            lastTriggerAtByWord[word] = now

            val strokes = GestureSerializer.fromJson(automation.strokesJson)
            val replayed = GestureAccessibilityService.replayGesture(strokes)
            Log.i(TAG, "Trigger word '${automation.triggerWord}' matched, gesture replayed=$replayed")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "VoiceListenerService"
        private const val CHANNEL_ID = "voice_listener_channel"
        private const val NOTIFICATION_ID = 42

        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes, renewed each restart cycle
        private const val RETRY_DELAY_DEFAULT_MS = 250L
        private const val RETRY_DELAY_BUSY_MS = 1000L
        private const val TRIGGER_DEBOUNCE_MS = 2000L

        fun start(context: Context) {
            val intent = Intent(context, VoiceListenerService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceListenerService::class.java))
        }
    }
}
