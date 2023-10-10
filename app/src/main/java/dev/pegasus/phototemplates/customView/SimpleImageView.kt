package dev.pegasus.phototemplates.customView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import dev.pegasus.phototemplates.R

class SimpleImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): View(context, attrs, defStyleAttr) {

    private var imageBitmap: Bitmap? = null
    private val matrix = Matrix()
    private val viewRect = RectF()
    private val imageRect = RectF()
    private val deviceScreenWidth = resources.displayMetrics.widthPixels

    init {
        setImageResource(R.drawable.dummy)
    }

    fun setImageResource(resourceId: Int) {
        imageBitmap = BitmapFactory.decodeResource(resources, resourceId)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val aspectRatio = if (imageBitmap != null && imageBitmap!!.width > 0) {
            imageBitmap!!.height.toFloat() / imageBitmap!!.width.toFloat()
        } else {
            1.0f
        }

        val measuredWidth: Int
        val measuredHeight: Int

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            // If both width and height are fixed (e.g., match_parent or specific dimensions)
            measuredWidth = widthSize
            measuredHeight = heightSize
        } else if (widthMode == MeasureSpec.EXACTLY) {
            // If width is fixed (e.g., match_parent or specific dimension)
            measuredWidth = widthSize
            measuredHeight = (measuredWidth * aspectRatio).toInt()
        } else if (heightMode == MeasureSpec.EXACTLY) {
            // If height is fixed (e.g., match_parent or specific dimension)
            measuredHeight = heightSize
            measuredWidth = (measuredHeight / aspectRatio).toInt()
        } else {
            // If both width and height are wrap_content
            measuredWidth = if (suggestedMinimumWidth != 0) suggestedMinimumWidth else {
                if (deviceScreenWidth > imageBitmap!!.width) imageBitmap!!.width else deviceScreenWidth
            }
            measuredHeight = (measuredWidth * aspectRatio).toInt()
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        imageBitmap?.let {
            viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
            imageRect.set(0f, 0f, it.width.toFloat(), it.height.toFloat())
            matrix.setRectToRect(imageRect, viewRect, Matrix.ScaleToFit.CENTER)
            canvas.drawBitmap(it, matrix, null)
        }
    }

}