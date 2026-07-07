package com.automation.voicegesture.util

import android.content.Context

/** Small wrapper around SharedPreferences for the one setting the app needs to persist. */
object Prefs {
    private const val FILE = "voice_gesture_prefs"
    private const val KEY_LISTENING_ENABLED = "listening_enabled"

    fun setListeningEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LISTENING_ENABLED, enabled)
            .apply()
    }

    fun isListeningEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_LISTENING_ENABLED, false)
}
