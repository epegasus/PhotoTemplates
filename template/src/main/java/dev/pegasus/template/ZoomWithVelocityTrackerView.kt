package dev.pegasus.template

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import dev.pegasus.template.utils.ImageUtils
import kotlin.math.hypot

class ZoomWithVelocityTrackerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val imageUtils by lazy { ImageUtils(context) }

    private val matrix = Matrix()
    private val velocityTracker = VelocityTracker.obtain()
    private var scaleFactor = 1f
    private var animator: ValueAnimator? = null
    private var initialDistance = 0f

    private var imageBitmap: Bitmap? = null

    fun setImageResource(@DrawableRes imageId: Int) {
        val imageDrawable = ContextCompat.getDrawable(context, imageId)
        this.imageBitmap = imageDrawable?.let { imageUtils.createBitmapFromDrawable(it) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        imageBitmap?.let { canvas.drawBitmap(it, matrix, null) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Initialize velocity tracker when touch starts
                velocityTracker.clear()
                velocityTracker?.addMovement(event)
                // Set initial distance for pinch gesture
                initialDistance = calculateFingerDistance(event)
            }
            MotionEvent.ACTION_MOVE -> {
                // Update velocity tracker during movement
                velocityTracker?.addMovement(event)

                // Calculate scale factor based on pinch gesture
                val newScaleFactor = calculateScaleFactor(event)
                setScale(newScaleFactor)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Release velocity tracker when touch ends
                velocityTracker?.recycle()
                velocityTracker.clear()

                // Start animation to smoothly zoom back to 1.0f
                startScaleAnimation()
            }
        }
        return true
    }

    private fun calculateFingerDistance(event: MotionEvent): Float {
        return try {
            val deltaX = event.getX(0) - event.getX(1)
            val deltaY = event.getY(0) - event.getY(1)
            hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()
        }
        catch (ex: Exception){
            ex.printStackTrace()
            return 0f
        }
    }

    private fun calculateScaleFactor(event: MotionEvent): Float {
        val pointerCount = event.pointerCount
        if (pointerCount < 2) return scaleFactor

        var distance = 0f

        try {
            // Calculate distance between two pointers
            val deltaX = event.getX(0) - event.getX(1)
            val deltaY = event.getY(0) - event.getY(1)
            distance = hypot(deltaX.toDouble(), deltaY.toDouble()).toFloat()
            // Calculate scale factor based on distance
            val newScaleFactor = distance / initialDistance
            return newScaleFactor.coerceIn(MIN_SCALE, MAX_SCALE)
        }
        catch (ex: Exception){
            ex.printStackTrace()
            return 0f
        }
    }

    private fun setScale(scale: Float) {
        this.scaleFactor = scale
        scaleX = scaleFactor
        scaleY = scaleFactor
    }

    private fun startScaleAnimation() {
        animator?.cancel()

        // Use ValueAnimator to smoothly animate scale back to 1.0f
        animator = ValueAnimator.ofFloat(scaleFactor, 1.0f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                setScale(animatedValue)
            }
            start()
        }
    }

    companion object {
        private const val MIN_SCALE = 1.0f
        private const val MAX_SCALE = 3.0f
        private const val ANIMATION_DURATION = 300L
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        velocityTracker?.recycle()
        animator?.cancel()
    }



    /*private inner class PinchGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minScaleFactor, maxScaleFactor)

            // Implement scaling using a matrix
            matrix.setScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)

            invalidate()
            return true
        }
    }*/

    /*private fun startFlingAnimation(velocityX: Float, velocityY: Float) {
        val animatorX = ValueAnimator.ofFloat(0f, velocityX / 2)
        val animatorY = ValueAnimator.ofFloat(0f, velocityY / 2)

        animatorX.addUpdateListener { animation ->
            val translationX = animation.animatedValue as Float
            matrix.postTranslate(translationX, 0f)
            invalidate()
        }

        animatorY.addUpdateListener { animation ->
            val translationY = animation.animatedValue as Float
            matrix.postTranslate(0f, translationY)
            invalidate()
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animatorX, animatorY)
        animatorSet.duration = 500 // Adjust the duration as needed
        animatorSet.start()
    }*/

}