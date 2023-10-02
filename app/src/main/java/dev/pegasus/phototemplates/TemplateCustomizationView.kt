package dev.pegasus.phototemplates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toRect

class TemplateCustomizationView : View {

    // The background template image (1080x1080 in your example).
    private var templateBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_bg)

    // The selected image to be positioned within the template.
    private var selectedImageBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.img_pic)

    // Matrix to handle image transformations (e.g., translation).
    private val matrix = Matrix()

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
        // Initialize your templateBitmap and selectedImageBitmap here.
        // Example: templateBitmap = BitmapFactory.decodeResource(resources, R.drawable.template_image)
        // Example: selectedImageBitmap = BitmapFactory.decodeResource(resources, R.drawable.selected_image)

        // Set up initial positions and sizes for the template and selected image.
        templateRect.set(0F, 0F, 900F, 1200F)
        imageRect.set(300F, 450F, 600F, 750F)
        imageRectFix.set(300F, 450F, 600F, 750F)

        // Set the view to be clickable, so it can receive touch events.
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Measure the view based on the template's size.
        val width = MeasureSpec.makeMeasureSpec(templateRect.width().toInt(), MeasureSpec.EXACTLY)
        val height = MeasureSpec.makeMeasureSpec(templateRect.height().toInt(), MeasureSpec.EXACTLY)
        setMeasuredDimension(width, height)
    }

    private val selectedImageDrawable by lazy { BitmapDrawable(resources, selectedImageBitmap) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the background template image.
        canvas.drawBitmap(templateBitmap, null, templateRect, null)
        //canvas.drawBitmap(selectedImageBitmap, null, imageRect, null)

        // Calculate the bounds for the selected image within the templateRect.
        val imageLeft = (templateRect.left + imageRect.left).coerceAtLeast(templateRect.left)
        val imageTop = (templateRect.top + imageRect.top).coerceAtLeast(templateRect.top)
        val imageRight = (templateRect.left + imageRect.right).coerceAtMost(templateRect.right)
        val imageBottom = (templateRect.top + imageRect.bottom).coerceAtMost(templateRect.bottom)

        // Set the bounds for the selected image drawable.
        selectedImageDrawable.bounds = RectF(imageLeft, imageTop, imageRight, imageBottom).toRect()

        // Clip the drawing of the selected image to the template bounds.
        canvas.clipRect(imageRectFix)

        canvas.concat(matrix)
        selectedImageDrawable.draw(canvas)
        canvas.concat(matrix)



        // Apply the matrix transformation to the selected image and draw it.
        //canvas.drawBitmap(selectedImageBitmap, matrix, null)
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

                    // Apply translation to the matrix to move the selected image.
                    matrix.postTranslate(dx, dy)

                    // Ensure the selected image stays within the template bounds.
                    //matrix.mapRect(imageRect)
                    imageRect.offset(dx, dy)

                    if (!templateRect.contains(imageRect)) {
                        // If the image is going out of bounds, restrict it to the template.
                        matrix.postTranslate(-dx, -dy)
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