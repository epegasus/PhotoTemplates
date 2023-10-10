package dev.pegasus.phototemplates.customView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView.ScaleType
import dev.pegasus.phototemplates.R

class TemplateView4 @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    // The selected image to be positioned within the template.
    private var selectedImageBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.birthday_frame_one)

    private var scaleMatrix: Matrix? = null
    private var drawable: Drawable? = BitmapDrawable(resources, selectedImageBitmap)
    private var aspectRatio: Float = 0.5f // Default aspect ratio

    init {
        val scaleType = ScaleType.MATRIX
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        // Calculate the desired width based on the aspect ratio
        val desiredWidth: Int = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            (heightSize * aspectRatio).toInt()
        }

        // Calculate the desired height based on the aspect ratio
        val desiredHeight: Int = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            (widthSize / aspectRatio).toInt()
        }

        // Set the measured dimensions
        setMeasuredDimension(desiredWidth, desiredHeight)
    }

    override fun onDraw(canvas: Canvas) {
        if (drawable != null) {
            if (scaleMatrix == null) {
                // Initialize the scale matrix based on the aspect ratio
                scaleMatrix = computeScaleMatrix(drawable!!)
            }
            canvas.drawBitmap(imageBitmap, scaleMatrix!!, null)
        } else super.onDraw(canvas)
    }

    private val imageBitmap: Bitmap
        get() = drawable?.let { dr ->
            val bitmap = Bitmap.createBitmap(
                dr.intrinsicWidth,
                dr.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            dr.setBounds(0, 0, canvas.width, canvas.height)
            dr.draw(canvas)
            bitmap
        } ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    private fun computeScaleMatrix(drawable: Drawable): Matrix {
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()

        val matrix = Matrix()
        val scale: Float
        val dx: Float
        val dy: Float

        if (drawableWidth * viewHeight > viewWidth * drawableHeight) {
            scale = viewHeight / drawableHeight
            dx = (viewWidth - drawableWidth * scale) * 0.5f
            dy = 0f
        } else {
            scale = viewWidth / drawableWidth
            dx = 0f
            dy = (viewHeight - drawableHeight * scale) * 0.5f
        }

        matrix.setScale(scale, scale)
        matrix.postTranslate(dx, dy)
        return matrix
    }
}
