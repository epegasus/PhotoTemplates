package dev.pegasus.phototemplates.customView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView.ScaleType
import dev.pegasus.phototemplates.R
import kotlin.math.abs

class TemplateView2 : View {

    // Background Template
    private var templateBitmap: Bitmap? = BitmapFactory.decodeResource(resources, R.drawable.birthday_frame_one)

    private var mScaleType: ScaleType? = null
    private val mAdjustViewBounds = false
    private var mMatrix: Matrix? = null
    private var mMaxWidth = Int.MAX_VALUE
    private var mMaxHeight = Int.MAX_VALUE

    private val mDrawable: Drawable = BitmapDrawable(resources, templateBitmap)
    private val mDrawableWidth = mDrawable.intrinsicWidth
    private val mDrawableHeight = mDrawable.intrinsicHeight
    private val mDrawMatrix: Matrix? = null

    // Rectangles to define the boundaries of the template and selected image.
    private val mTempSrc = RectF()
    private val mTempDst = RectF()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        mMatrix = Matrix()
        mScaleType = ScaleType.FIT_CENTER
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

        w = mDrawableWidth
        h = mDrawableHeight
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mDrawableWidth == 0 || mDrawableHeight == 0) {
            return // nothing to draw (empty bounds)
        }
        if (mDrawMatrix == null) {
            mDrawable.draw(canvas)
        } else {
            val saveCount = canvas.saveCount
            canvas.save()
            canvas.concat(mDrawMatrix)
            mDrawable.draw(canvas)
            canvas.restoreToCount(saveCount)
        }
    }
}
