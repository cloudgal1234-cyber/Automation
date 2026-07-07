package com.automation.voicegesture.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.automation.voicegesture.data.GesturePoint
import com.automation.voicegesture.data.GestureStroke

/**
 * A drawing surface that records the user's finger movements (position + timing) so the
 * gesture can later be replayed on screen via [android.accessibilityservice.AccessibilityService.dispatchGesture].
 * Supports multiple simultaneous fingers (multi-touch gestures).
 */
class GestureDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint().apply {
        color = Color.parseColor("#6750A4")
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    // One drawing Path + recorded points per active pointer id.
    private val activePaths = mutableMapOf<Int, Path>()
    private val activePoints = mutableMapOf<Int, MutableList<GesturePoint>>()
    private val finishedStrokes = mutableListOf<GestureStroke>()

    private var gestureStartTime = 0L
    private var onGestureChangedListener: (() -> Unit)? = null

    fun setOnGestureChangedListener(listener: () -> Unit) {
        onGestureChangedListener = listener
    }

    fun hasRecordedGesture(): Boolean = finishedStrokes.isNotEmpty()

    fun getRecordedStrokes(): List<GestureStroke> = finishedStrokes.toList()

    fun clear() {
        activePaths.clear()
        activePoints.clear()
        finishedStrokes.clear()
        invalidate()
        onGestureChangedListener?.invoke()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionMasked = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (activePaths.isEmpty() && activePoints.isEmpty() && finishedStrokes.isEmpty()) {
                    gestureStartTime = System.currentTimeMillis()
                }
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                activePaths[pointerId] = Path().apply { moveTo(x, y) }
                activePoints[pointerId] = mutableListOf(
                    GesturePoint(x, y, System.currentTimeMillis() - gestureStartTime)
                )
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val path = activePaths[id] ?: continue
                    val points = activePoints[id] ?: continue
                    val x = event.getX(i)
                    val y = event.getY(i)
                    path.lineTo(x, y)
                    points.add(GesturePoint(x, y, System.currentTimeMillis() - gestureStartTime))
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                activePoints.remove(pointerId)?.let { points ->
                    if (points.size > 1) finishedStrokes.add(GestureStroke(points))
                }
                activePaths.remove(pointerId)
                if (actionMasked == MotionEvent.ACTION_UP) {
                    onGestureChangedListener?.invoke()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                activePaths.clear()
                activePoints.clear()
            }
        }

        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (path in activePaths.values) {
            canvas.drawPath(path, paint)
        }
        // Redraw finished strokes as flattened paths so they remain visible after ACTION_UP.
        for (stroke in finishedStrokes) {
            val path = Path()
            stroke.points.forEachIndexed { index, point ->
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            canvas.drawPath(path, paint)
        }
    }
}
