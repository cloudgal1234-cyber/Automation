package com.automation.voicegesture.data

/**
 * A single sampled point of a recorded gesture.
 * [timeOffsetMs] is the time since the gesture started, used to reproduce speed/rhythm on replay.
 */
data class GesturePoint(
    val x: Float,
    val y: Float,
    val timeOffsetMs: Long
)

/** One continuous finger stroke, i.e. from ACTION_DOWN to ACTION_UP for a single pointer. */
data class GestureStroke(
    val points: List<GesturePoint>
)
