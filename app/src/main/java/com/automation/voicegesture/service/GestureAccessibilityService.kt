package com.automation.voicegesture.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.automation.voicegesture.data.GestureStroke
import java.lang.ref.WeakReference

/**
 * Replays a user-recorded gesture on screen using the Accessibility APIs.
 *
 * The user must enable this service once from Settings > Accessibility, Android does not allow
 * apps to turn on accessibility services programmatically.
 *
 * Note: for security reasons the platform restricts gesture dispatch while the keyguard
 * (lock screen) is showing and secured, so gestures reliably replay once the device is unlocked.
 * The voice trigger itself is still recognized while locked; see VoiceListenerService.
 */
class GestureAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
        Log.i(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No content inspection needed; this service is used purely for gesture dispatch.
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        if (instanceRef?.get() == this) instanceRef = null
    }

    private fun replay(strokes: List<GestureStroke>): Boolean {
        if (strokes.isEmpty()) return false

        val builder = GestureDescription.Builder()
        var strokeCount = 0
        for (stroke in strokes) {
            if (stroke.points.size < 2) continue
            val path = Path()
            stroke.points.forEachIndexed { index, point ->
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            val startTime = stroke.points.first().timeOffsetMs.coerceAtLeast(0)
            val duration = (stroke.points.last().timeOffsetMs - stroke.points.first().timeOffsetMs)
                .coerceAtLeast(MIN_STROKE_DURATION_MS)
            builder.addStroke(GestureDescription.StrokeDescription(path, startTime, duration))
            strokeCount++
        }
        if (strokeCount == 0) return false

        return dispatchGesture(builder.build(), null, null)
    }

    companion object {
        private const val TAG = "GestureA11yService"
        private const val MIN_STROKE_DURATION_MS = 16L

        @Volatile
        private var instanceRef: WeakReference<GestureAccessibilityService>? = null

        val isEnabled: Boolean
            get() = instanceRef?.get() != null

        /** Replays [strokes] on screen. Returns false if the accessibility service isn't enabled. */
        fun replayGesture(strokes: List<GestureStroke>): Boolean {
            val service = instanceRef?.get() ?: run {
                Log.w(TAG, "Cannot replay gesture: accessibility service not enabled")
                return false
            }
            return service.replay(strokes)
        }
    }
}
