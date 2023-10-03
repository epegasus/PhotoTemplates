package dev.pegasus.phototemplates.customView

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toRect
import dev.pegasus.phototemplates.R

class TemplateView : View {

    // The background template image (1080x1080 in your example).
    private var templateBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_bg)

    // The selected image to be positioned within the template.
    private var selectedImageBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_pic)
    private val selectedImageDrawable by lazy { BitmapDrawable(resources, selectedImageBitmap) }

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
        imageRect.set(300F, 450F, 800F, 750F)
        imageRectFix.set(300F, 450F, 800F, 750F)

        // Set the view to be clickable, so it can receive touch events.
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Measure the view based on the template's size.
        val width = MeasureSpec.makeMeasureSpec(templateRect.width().toInt(), MeasureSpec.EXACTLY)
        val height = MeasureSpec.makeMeasureSpec(templateRect.height().toInt(), MeasureSpec.EXACTLY)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        templateRect.set(0F, 0F, w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background template image.
        canvas.drawBitmap(templateBitmap, null, templateRect, null)

        // Set the bounds for the selected image drawable.
        selectedImageDrawable.bounds = imageRect.toRect()

        // Clip the drawing of the selected image to the template bounds.
        canvas.clipRect(imageRectFix)

        selectedImageDrawable.draw(canvas)
    }

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

                    // If the image is going out of bounds, restrict it to the template.
                    if (!templateRect.contains(imageRect)) {
                        imageRect.offset(-dx, -dy)
                    }

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