package dev.pegasus.phototemplates.customView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView.ScaleType
import androidx.core.graphics.toRect
import dev.pegasus.phototemplates.R
import kotlin.math.abs

class TemplateView : View {

    // Background Template
    private var templateBitmap: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.birthday_frame_one)

    // The selected image to be positioned within the template.
    private var selectedImageBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_pic)

    // We will change our resources to drawables, so that bounds work correctly on it.
    private val templateDrawable by lazy { BitmapDrawable(resources, templateBitmap) }
    private val selectedImageDrawable by lazy { BitmapDrawable(resources, selectedImageBitmap) }

    private var bgImageHeight: Int = templateDrawable.intrinsicHeight
    private var bgImageWidth: Int = templateDrawable.intrinsicWidth

    private val mScaleType: ScaleType? = null
    private val mAdjustViewBounds = false
    private var mMaxWidth = Int.MAX_VALUE
    private var mMaxHeight = Int.MAX_VALUE

    // Taking Device Screen sizes to scale background template, if greater
    // private val screenWidth = resources.displayMetrics.widthPixels
    // private val screenHeight = resources.displayMetrics.heightPixels
    // private val scaleX = screenWidth.toFloat() / templateBitmap.width
    // private val scaleY = screenHeight.toFloat() / templateBitmap.height
    // private val scale = minOf(scaleX, scaleY)

    // Our scaled bitmap (according to the device screen)
    // private val scaledBitmap = Bitmap.createScaledBitmap(templateBitmap, (templateBitmap.width * scale).toInt(), (templateBitmap.height * scale).toInt(), true)

    // To keep the background image aspect ratio, so that we always maintain the ratio
    // private val bgImageAspectRatio: Float = (scaledBitmap.height / scaledBitmap.width).toFloat()

    // Rectangles to define the boundaries of the template and selected image.
    private val templateRect = RectF()
    private val imageRect = RectF()
    private val imageRectFix = RectF()

    // Variables to track touch events.
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        // Set up initial positions and sizes for the template and selected image.
        imageRect.set(495F, 268F, 1000F, 1100F)
        imageRectFix.set(495F, 268F, 1000F, 1100F)

        // Set the view to be clickable, so it can receive touch events.
        isClickable = true
        isFocusable = true
    }

    private fun getMaxWidth() = mMaxWidth
    private fun setMaxWidth(maxWidth: Int) {
        mMaxWidth = maxWidth
    }
    private fun getMaxHeight() = mMaxHeight
    private fun setMaxHeight(maxHeight: Int) {
        mMaxHeight = maxHeight
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        var w = 0
        var h = 0

        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)

        // We are allowed to change the view's width
        val resizeWidth = widthSpecMode != MeasureSpec.EXACTLY

        // We are allowed to change the view's height
        val resizeHeight = heightSpecMode != MeasureSpec.EXACTLY

        w = bgImageWidth
        h = bgImageHeight
        if (w <= 0) w = 1
        if (h <= 0) h = 1

        // Desired aspect ratio of the view's contents (not including padding)
        val desiredAspect = (w / h).toFloat()

        var widthSize: Int
        var heightSize: Int

        if (resizeHeight || resizeWidth) {
            /* If we get here, it means we want to resize to match the
                drawables aspect ratio, and we have the freedom to change at
                least one dimension.
            */

            // Get the max possible width given our constraints
            widthSize = resolveAdjustedSize(w, mMaxWidth, widthMeasureSpec)

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h, mMaxHeight, heightMeasureSpec)

            if (desiredAspect != 0.0f) {
                // See what our actual aspect ratio is
                val actualAspect = (widthSize / heightSize).toFloat()
                if (abs(actualAspect - desiredAspect) > 0.0000001) {
                    var done = false
                    // Try adjusting width to be proportional to height
                    if (resizeWidth) {
                        val newWidth = (desiredAspect * heightSize).toInt()
                        // Allow the width to outgrow its original estimate if height is fixed.
                        if (!resizeHeight /*&& !sCompatAdjustViewBounds*/) widthSize = resolveAdjustedSize(newWidth, mMaxWidth, widthMeasureSpec)
                        if (newWidth <= widthSize) {
                            widthSize = newWidth
                            done = true
                        }
                    }
                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        val newHeight = (widthSize / desiredAspect).toInt()
                        // Allow the height to outgrow its original estimate if width is fixed
                        if (!resizeWidth /*&& !sCompatAdjustViewBounds*/) heightSize = resolveAdjustedSize(newHeight, mMaxHeight, heightMeasureSpec)
                        if (newHeight <= heightSize) heightSize = newHeight
                    }
                }
            }
        } else {
            /* We are either don't want to preserve the drawables aspect ratio,
               or we are not allowed to change view dimensions. Just measure in
               the normal way.
            */

            w = w.coerceAtLeast(suggestedMinimumWidth)
            h = h.coerceAtLeast(suggestedMinimumHeight)

            widthSize = resolveSizeAndState(w, widthMeasureSpec, 0)
            heightSize = resolveSizeAndState(h, heightMeasureSpec, 0)
        }

        /*val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        var desiredHeight: Int = 0
        var desiredWidth: Int = 0

        when (widthMode) {
            MeasureSpec.EXACTLY -> {
                desiredWidth = viewWidth
                desiredHeight = (viewWidth * bgImageAspectRatio).toInt()
            }
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> {
                desiredWidth = MeasureSpec.makeMeasureSpec(scaledBitmap.width, widthMode)
            }
        }

        when (heightMode) {
            MeasureSpec.EXACTLY -> {
                desiredHeight = viewHeight
                desiredWidth = (viewHeight / bgImageAspectRatio).toInt()
            }
            MeasureSpec.AT_MOST -> {
                if (widthMode != MeasureSpec.EXACTLY) desiredHeight = MeasureSpec.makeMeasureSpec(scaledBitmap.height, heightMode)
            }
            MeasureSpec.UNSPECIFIED -> if (widthMode != MeasureSpec.EXACTLY) desiredHeight = MeasureSpec.makeMeasureSpec((scaledBitmap.height / bgImageAspectRatio).toInt(), heightMode)
        }

        when (widthMode) {
            MeasureSpec.AT_MOST -> desiredWidth = MeasureSpec.makeMeasureSpec(scaledBitmap.width, MeasureSpec.AT_MOST)
            MeasureSpec.UNSPECIFIED -> desiredWidth = MeasureSpec.makeMeasureSpec(scaledBitmap.width, MeasureSpec.UNSPECIFIED)
        }

        when (heightMode) {
            MeasureSpec.AT_MOST -> desiredHeight = MeasureSpec.makeMeasureSpec(scaledBitmap.height, MeasureSpec.AT_MOST)
            MeasureSpec.UNSPECIFIED -> desiredHeight = MeasureSpec.makeMeasureSpec(scaledBitmap.height, MeasureSpec.UNSPECIFIED)
        }

        when {
            widthMode == MeasureSpec.EXACTLY -> {
                desiredWidth = viewWidth
                desiredHeight = (viewWidth * bgImageAspectRatio).toInt()
            }
            heightMode == MeasureSpec.EXACTLY -> {
                desiredHeight = viewHeight
                desiredWidth = (viewHeight / bgImageAspectRatio).toInt()
            }
        }*/

        setMeasuredDimension(widthSize, heightSize)
    }

    private fun resolveAdjustedSize(desiredSize: Int, maxSize: Int, measureSpec: Int): Int {
        var result = desiredSize
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        when (specMode) {
            MeasureSpec.UNSPECIFIED -> {
                /* Parent says we can be as big as we want. Just don't be larger
                   than max size imposed on ourselves.
                */
                result = desiredSize.coerceAtMost(maxSize)
            }

            MeasureSpec.AT_MOST -> {
                // Parent says we can be as big as we want, up to specSize.
                // Don't be larger than specSize, and don't be larger than
                // the max size imposed on ourselves.
                result = desiredSize.coerceAtMost(specSize).coerceAtMost(maxSize)
            }
            MeasureSpec.EXACTLY -> {
                // No choice. Do what we are told.
                result = specSize
            }
        }
        return result
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        templateRect.set(0F, 0F, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background template image.
        canvas.drawBitmap(templateBitmap!!, null, templateRect, null)

        // Set the bounds for the selected image drawable.
        selectedImageDrawable.bounds = imageRect.toRect()

        // Clip the drawing of the selected image to the template bounds.
        canvas.clipRect(imageRectFix)

        selectedImageDrawable.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
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

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }

        // Consume the event to indicate that it's been handled.
        return true
    }
}
