package dev.pegasus.phototemplates.customView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toRect
import dev.pegasus.phototemplates.R

class TemplateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private var TAG: String = "TemplateView"

    // Background Template
    private var templateBitmap: Bitmap? = null
    private var selectedImageBitmap: Bitmap? = null
    private var mDrawable: Drawable? = null

    // Rectangles to define the boundaries of the template and selected image.
    private val viewRect = RectF()
    private val templateRect = RectF()
    private val imageRect = RectF()
    private var imageRectFix = RectF()

    // Variables to track touch events.
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // Using this matrix we will show the bg template according to the aspect ratio
    private val matrix = Matrix()

    private val deviceScreenWidth = resources.displayMetrics.widthPixels
    private var aspectRatio: Float = 1.0f
    private var isFirstTime = true

    // Initialize a float array to hold the matrix values
    private val matrixValues = FloatArray(9)

    init {
        setImageResource(R.drawable.img_bg, R.drawable.img_pic)

        isClickable = true
        isFocusable = true
    }

    fun setImageResource(templateId: Int, imageId: Int) {
        templateBitmap = BitmapFactory.decodeResource(resources, templateId)
        selectedImageBitmap = BitmapFactory.decodeResource(resources, imageId)
        mDrawable = BitmapDrawable(resources, selectedImageBitmap)
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (templateBitmap!!.width > 0) aspectRatio = templateBitmap!!.height.toFloat() / templateBitmap!!.width.toFloat()

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
                if (deviceScreenWidth > templateBitmap!!.width) templateBitmap!!.width else deviceScreenWidth
            }
            measuredHeight = (measuredWidth * aspectRatio).toInt()
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    /*override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background template image.
        templateBitmap?.let {
            viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
            templateRect.set(0f, 0f, it.width.toFloat(), it.height.toFloat())
            matrix.setRectToRect(templateRect, viewRect, Matrix.ScaleToFit.CENTER)
            canvas.drawBitmap(it, matrix, null)
        }

        // Get the matrix values
        val matrixValues = matrix.values()

        // Calculate the transformed dimensions of the template bitmap
        val transformedWidth = templateRect.width() * matrixValues[Matrix.MSCALE_X]
        val transformedHeight = templateRect.height() * matrixValues[Matrix.MSCALE_Y]

        // Calculate the center position of the transformed bitmap
        val centerX = (width.toFloat() - transformedWidth) / 2f
        val centerY = (height.toFloat() - transformedHeight) / 2f

        // Calculate the size and position of imageRect based on the transformation
        val imageRectLeft = centerX - (selectedImageBitmap!!.width / 2)
        val imageRectTop = centerY - (selectedImageBitmap!!.height / 2)
        val imageRectRight = centerX + (selectedImageBitmap!!.width / 2)
        val imageRectBottom = centerY + (selectedImageBitmap!!.height / 2)

        // Update imageRect with the calculated values
        imageRect.set(imageRectLeft, imageRectTop, imageRectRight, imageRectBottom)
        imageRectFix.set(imageRect)

        // Set the bounds for the selected image drawable.
        mDrawable!!.bounds = imageRect.toRect()

        // Clip the drawing of the selected image to the template bounds.
        canvas.clipRect(imageRectFix)

        // Draw the selected image
        mDrawable!!.draw(canvas)
    }*/


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background template image.
        templateBitmap?.let {
            viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
            templateRect.set(0f, 0f, it.width.toFloat(), it.height.toFloat())
            matrix.setRectToRect(templateRect, viewRect, Matrix.ScaleToFit.CENTER)
            canvas.drawBitmap(it, matrix, null)
        }

        if (isFirstTime){
            // Get the matrix values
            matrix.getValues(matrixValues)

            // Extract the scaling factors
            val scaleX = matrixValues[Matrix.MSCALE_X]
            val scaleY = matrixValues[Matrix.MSCALE_Y]

            // Calculate the transformed dimensions of imageRect
            val transformedWidth = templateRect.width() * scaleX
            val transformedHeight = templateRect.height() * scaleY

            // Calculate the left, top, right, and bottom values of the transformed imageRect
            val transformedLeft = templateRect.left * scaleX
            val transformedTop = templateRect.top * scaleY
            val transformedRight = transformedLeft + transformedWidth
            val transformedBottom = transformedTop + transformedHeight

            imageRect.set(transformedLeft, transformedTop, transformedRight, transformedBottom)
            imageRectFix.set(imageRect)

            isFirstTime = false
        }

        // Set the bounds for the selected image drawable.
        mDrawable!!.bounds = imageRect.toRect()

        // Clip the drawing of the selected image to the template bounds.
        canvas.clipRect(imageRectFix)

        mDrawable!!.draw(canvas)
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
