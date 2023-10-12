package dev.pegasus.template

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import dev.pegasus.template.utils.HelperUtils.TAG
import dev.pegasus.template.utils.ImageUtils

class TemplateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val imageUtils by lazy { ImageUtils(context) }

    /**
     * @property backgroundBitmap: Bitmap of background of the template
     * @property imageDrawable: Drawable of the image provided by user (we need this ultimately)
     */

    private var backgroundBitmap: Bitmap? = null
    private var imageDrawable: Drawable? = null

    /**
     * @property viewRect: It's the main view named as "TemplateView"
     * @property backgroundRect: Coordinates of the background of current template
     * @property imageRect: Coordinates of the user's image (mutable)
     * @property imageRectFix: Coordinates of the user's image (immutable)
     */

    private val viewRect = RectF()
    private val backgroundRect = RectF()
    private val imageRect = RectF()
    private var imageRectFix = RectF()

    /**
     * Variables to track touch events
     * @property lastTouchX: Save x-axis of a touch inside a view
     * @property lastTouchY: Save y-axis of a touch inside a view
     * @property isDragging: Check if user's image can be drag-able (depends on touch events)
     */

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    /**
     * @property viewRect: "
     */
    // Using this matrix we will show the bg template according to the aspect ratio
    private val matrix = Matrix()

    /**
     * @property viewRect: "
     */
    private val deviceScreenWidth = resources.displayMetrics.widthPixels
    private var aspectRatio: Float = 1.0f

    /**
     * @property viewRect: "
     */
    // Calculate the transformed dimensions of imageRect
    private var transformedWidth = 0f
    private var transformedHeight = 0f

    /**
     * @property viewRect: "
     */
    // Calculate the left, top, right, and bottom values of the transformed imageRect
    private var transformedLeft = 0f
    private var transformedTop = 0f
    private var transformedRight = 0f
    private var transformedBottom = 0f

    /**
     * @property viewRect: "
     */
    // Initialize a float array to hold the matrix values
    private val matrixValues = FloatArray(9)

    /**
     * @property viewRect: "
     */
    private var isFirstTime = true
    private var isZooming = false
    private var scaleFactor = 1.0f

    /**
     * Set Backgrounds
     */
    override fun setBackgroundResource(@DrawableRes resId: Int) {
        backgroundBitmap = BitmapFactory.decodeResource(resources, resId)
        requestLayout()
        invalidate()
    }

    fun setBackgroundBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            Log.e(TAG, "TemplateView: setBackgroundBitmap: ", NullPointerException("Bitmap is Null"))
            return
        }
        backgroundBitmap = bitmap
        requestLayout()
        invalidate()
    }

    @Deprecated("Deprecated in Java")
    override fun setBackgroundDrawable(drawable: Drawable?) {
        if (drawable == null) {
            Log.e(TAG, "TemplateView: setBackgroundDrawable: ", NullPointerException("Drawable is Null"))
            return
        }
        backgroundBitmap = imageUtils.createBitmapFromDrawable(drawable)
        requestLayout()
        invalidate()
    }

    /**
     * Set User Images
     */

    fun setImageResource(@DrawableRes imageId: Int) {
        imageDrawable = ContextCompat.getDrawable(context, imageId)
        requestLayout()
        invalidate()
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            Log.e(TAG, "TemplateView: setImageBitmap: ", NullPointerException("Bitmap is Null"))
            return
        }
        imageDrawable = imageUtils.createDrawableFromBitmap(bitmap)
        requestLayout()
        invalidate()
    }

    fun setImageDrawable(drawable: Drawable?) {
        if (drawable == null) {
            Log.e(TAG, "TemplateView: setImageDrawable: ", NullPointerException("Drawable is Null"))
            return
        }
        imageDrawable = drawable
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (backgroundBitmap!!.width > 0) aspectRatio = backgroundBitmap!!.height.toFloat() / backgroundBitmap!!.width.toFloat()

        val measuredWidth: Int
        val measuredHeight: Int

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            // If both width and height are fixed (e.g., match_parent or specific dimensions)
            measuredWidth = viewWidth
            measuredHeight = viewHeight
        } else if (widthMode == MeasureSpec.EXACTLY) {
            // If width is fixed (e.g., match_parent or specific dimension)
            measuredWidth = viewWidth
            measuredHeight = (measuredWidth * aspectRatio).toInt()
        } else if (heightMode == MeasureSpec.EXACTLY) {
            // If height is fixed (e.g., match_parent or specific dimension)
            measuredHeight = viewHeight
            measuredWidth = (measuredHeight / aspectRatio).toInt()
        } else {
            // If both width and height are wrap_content
            measuredWidth = if (suggestedMinimumWidth != 0) suggestedMinimumWidth else {
                if (deviceScreenWidth > backgroundBitmap!!.width) backgroundBitmap!!.width else deviceScreenWidth
            }
            measuredHeight = (measuredWidth * aspectRatio).toInt()
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background template image.
        backgroundBitmap?.let {
            viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
            backgroundRect.set(0f, 0f, it.width.toFloat(), it.height.toFloat())
            matrix.setRectToRect(backgroundRect, viewRect, Matrix.ScaleToFit.CENTER)
            canvas.drawBitmap(it, matrix, null)
        }

        if (isFirstTime) {
            // Get the matrix values
            matrix.getValues(matrixValues)

            // Extract the scaling factors
            val scaleX = matrixValues[Matrix.MSCALE_X]
            val scaleY = matrixValues[Matrix.MSCALE_Y]

            // Calculate the transformed dimensions of imageRect
            transformedWidth = backgroundRect.width() * scaleX
            transformedHeight = backgroundRect.height() * scaleY

            // Calculate the left, top, right, and bottom values of the transformed imageRect
            transformedLeft = (backgroundRect.left * scaleX) + 450
            transformedTop = (backgroundRect.top * scaleY) + 300
            transformedRight = (transformedLeft + transformedWidth) - 500
            transformedBottom = (transformedTop + transformedHeight) - 600

            imageRect.set(transformedLeft, transformedTop, transformedRight, transformedBottom)
            imageRectFix.set(imageRect)

            isFirstTime = false
        }

        if (isZooming) {
            // Apply the scaleFactor uniformly to all variables
            val scaledWidth = transformedWidth * scaleFactor
            val scaledHeight = transformedHeight * scaleFactor
            transformedLeft = (transformedLeft * scaleFactor)
            transformedTop = (transformedTop * scaleFactor)
            transformedRight = (transformedLeft + scaledWidth)
            transformedBottom = (transformedTop + scaledHeight)

            isZooming = false

            // Ensure the image remains within the view's bounds
            val viewLeft = 0f
            val viewTop = 0f
            val viewRight = width.toFloat()
            val viewBottom = height.toFloat()

            if (transformedLeft < viewLeft) {
                transformedRight += viewLeft - transformedLeft
                transformedLeft = viewLeft
            }
            if (transformedTop < viewTop) {
                transformedBottom += viewTop - transformedTop
                transformedTop = viewTop
            }
            if (transformedRight > viewRight) {
                transformedLeft -= transformedRight - viewRight
                transformedRight = viewRight
            }
            if (transformedBottom > viewBottom) {
                transformedTop -= transformedBottom - viewBottom
                transformedBottom = viewBottom
            }

            imageRect.set(transformedLeft, transformedTop, transformedRight, transformedBottom)
        }

        // Set the bounds for the selected image drawable.
        imageDrawable!!.bounds = imageRect.toRect()

        // Clip the drawing of the selected image to the template bounds.
        canvas.clipRect(imageRectFix)

        imageDrawable!!.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        event.let { scaleGestureDetector.onTouchEvent(it) }
        // Check if a zoom gesture occurred and don't handle other touch events
        if (scaleGestureDetector.isInProgress) return true

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if the touch event is within the selected image bounds.
                if (imageRect.contains(event.x, event.y)) {
                    isDragging = true
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    // Calculate the distance moved.
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    // Ensure the selected image stays within the template bounds.
                    imageRect.offset(dx, dy)

                    // Update the last touch position.
                    lastTouchX = event.x
                    lastTouchY = event.y

                    // Invalidate the view to trigger a redraw.
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isDragging = false
        }

        // Consume the event to indicate that it's been handled.
        return true
    }

    // Initialize a scale gesture detector for pinch-to-zoom
    private val scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = 1.0f.coerceAtLeast(scaleFactor.coerceAtMost(4f))
            isZooming = true
            invalidate()
            return true
        }
    })
}
