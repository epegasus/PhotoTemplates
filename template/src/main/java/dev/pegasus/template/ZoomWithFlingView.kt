package dev.pegasus.template

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import dev.pegasus.template.utils.ImageUtils

class ZoomWithFlingView  @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val imageUtils by lazy { ImageUtils(context) }

    private var bitmap: Bitmap? = null
    private val matrix = Matrix()

    // Constants for controlling the zoom level
    private val minZoom = 1.0f
    private val maxZoom = 3.0f

    // Variables for pinch-to-zoom
    private val scaleGestureDetector: ScaleGestureDetector
    private var scaleFactor = 1.0f
    private val focalPoint = PointF()

    // Variables for fling animation
    private var flingAnimatorX: ValueAnimator? = null
    private var flingAnimatorY: ValueAnimator? = null

    init {
        scaleGestureDetector = ScaleGestureDetector(context, PinchToZoomListener())
    }

    fun setImageBitmap(@DrawableRes imageId: Int) {
        val imageDrawable = ContextCompat.getDrawable(context, imageId)
        this.bitmap = imageDrawable?.let { imageUtils.createBitmapFromDrawable(it) }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let {
            canvas.drawBitmap(it, matrix, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)

        // Handle fling after pinch-to-zoom
        handleFling(event)

        invalidate()
        return true
    }

    private fun handleFling(event: MotionEvent) {
        // Implement fling using VelocityTracker or simply use MotionEvent history
        // For simplicity, this example uses MotionEvent history
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                // Start fling animation when the user releases the touch
                startFlingAnimation()
            }
        }
    }

    private fun startFlingAnimation() {
        // Fling animation for X-axis
        flingAnimatorX?.cancel()
        flingAnimatorX = ValueAnimator.ofFloat(matrixValues[Matrix.MTRANS_X], 0f)
        flingAnimatorX?.duration = 500
        flingAnimatorX?.interpolator = DecelerateInterpolator()
        flingAnimatorX?.addUpdateListener {
            matrix.postTranslate(it.animatedValue as Float, 0f)
            invalidate()
        }
        flingAnimatorX?.start()

        // Fling animation for Y-axis
        flingAnimatorY?.cancel()
        flingAnimatorY = ValueAnimator.ofFloat(matrixValues[Matrix.MTRANS_Y], 0f)
        flingAnimatorY?.duration = 500
        flingAnimatorY?.interpolator = DecelerateInterpolator()
        flingAnimatorY?.addUpdateListener {
            matrix.postTranslate(0f, it.animatedValue as Float)
            invalidate()
        }
        flingAnimatorY?.start()
    }

    private inner class PinchToZoomListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(minZoom, maxZoom)

            focalPoint.set(detector.focusX, detector.focusY)

            matrix.setScale(scaleFactor, scaleFactor, focalPoint.x, focalPoint.y)

            invalidate()
            return true
        }
    }

    companion object {
        private val matrixValues = FloatArray(9)
    }
}