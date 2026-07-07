package com.automation.voicegesture.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.automation.voicegesture.service.VoiceListenerService
import com.automation.voicegesture.util.Prefs

/** Restarts the voice listener service after a reboot if the user had it enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isListeningEnabled(context)) {
            VoiceListenerService.start(context)
        }
    }
}
