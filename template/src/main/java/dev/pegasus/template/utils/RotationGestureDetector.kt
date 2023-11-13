package dev.pegasus.template.utils

import android.view.MotionEvent
import kotlin.math.atan2

class RotationGestureDetector(private val listener: OnRotationGestureListener) {
    interface OnRotationGestureListener {
        fun onRotation(rotationAngle: Float)
    }

    private var angleDelta = 0f
    private var lastAngle = 0f
    private var cumulativeAngle = 0f

    fun onTouchEvent(event: MotionEvent?) {
        when (event?.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                lastAngle = getAngle(event)
            }
            MotionEvent.ACTION_MOVE -> {
                val newAngle = getAngle(event)
                angleDelta = newAngle - lastAngle
                lastAngle = newAngle
                cumulativeAngle += angleDelta
                listener.onRotation(cumulativeAngle)
            }
        }
    }

    private fun getAngle(event: MotionEvent?): Float {
        return try {
            event?.let {
                val dx = it.getX(0) - it.getX(1)
                val dy = it.getY(0) - it.getY(1)
                Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            } ?: run {
                lastAngle
            }
        }
        catch (ex: Exception){
            ex.printStackTrace()
            return lastAngle
        }
    }

}