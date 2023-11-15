package dev.pegasus.template

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.toRect
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dev.pegasus.template.dataClasses.TemplateModel
import dev.pegasus.template.state.CustomViewState
import dev.pegasus.template.utils.HelperUtils.TAG
import dev.pegasus.template.utils.ImageUtils
import dev.pegasus.template.utils.RotationGestureDetector
import dev.pegasus.template.viewModels.TemplateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TemplateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(context, attrs, defStyleAttr) {

    private val imageUtils by lazy { ImageUtils(context) }

    /**
     * @property backgroundBitmap: Bitmap of background of the template
     * @property imageDrawable: Drawable of the image provided by user (we need this ultimately)
     * @property imageBitmap: Holds the image the user select
     * @property imageMatrix: use to handle the zooming and rotation functionality of the user image
     */

    private var backgroundBitmap: Bitmap? = null
    private var imageBitmap: Bitmap? = null
    private var imageDrawable: Drawable? = null
    private var imageMatrix = Matrix()

    // create a coroutine scope
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

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
     * @property templateModel: Complete specifications for a template
     */
    private var templateModel: TemplateModel? = null

    /**
     * Variables to track touch events
     * @property lastTouchX: Save x-axis of a touch inside a view
     * @property lastTouchY: Save y-axis of a touch inside a view
     * @property isDragging: Check if user's image can be drag-able (depends on touch events)
     * @property dragValueX: Save the overall x-axis drag value, so to keep the user image in-place after screen configuration
     * @property dragValueY: Save the overall y-axis drag value, so to keep the user image inplace after screen configuration
     * @property isConfigurationTrigger: this value is a flag to indicate that whether configuration happened or not. It also used as an indicator for view size changed event
     */
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var dragValueX = 0f
    private var dragValueY = 0f
    private var isConfigurationTrigger = false

    /**
     * @property matrix: This matrix object is used to scale the background template according to the device screen and maintain the aspect ratio
     * @property matrixValues: It holds the matrix values, we need to get the location (width, height) of our background template
     */
    private val matrix = Matrix()
    private val matrixValues = FloatArray(9)

    /**
     * @property deviceScreenHeight: It saves the device screen height value, and we use it in onMeasure function, for properly locating our custom view
     * @property deviceScreenWidth: It saves the device screen width value, and we use it in onMeasure function to properly locate our custom view on the device screen
     * @property templateAspectRatio: to hold the aspect ratio of the background template
     * @property imageAspectRatio: to hold the aspect ratio of the user selected image
     */
    private val deviceScreenWidth = resources.displayMetrics.widthPixels
    private val deviceScreenHeight = resources.displayMetrics.heightPixels
    private var templateAspectRatio: Float = 1.0f
    private var imageAspectRatio: Float = 1.0f

    /**
     * @property transformedWidth: It holds the width value of the background template after scaling by the matrix
     * @property transformedHeight: It holds the height value of the background template after scaling by the matrix
     */
    // Calculate the transformed dimensions of imageRect
    private var transformedWidth = 0f
    private var transformedHeight = 0f

    /**
     * @property transformedLeft: This holds the left value of the frame (where we want to show the user image) inside a background template
     * @property transformedTop: This holds the top value of the frame (where we want to show the user image) inside a background template
     * @property transformedRight: This holds the right value of the frame (where we want to show the user image) inside a background template
     * @property transformedBottom: This holds the bottom value of the frame (where we want to show the user image) inside a background template
     */
    // Calculate the left, top, right, and bottom values of the transformed imageRect
    private var transformedLeft = 0f
    private var transformedTop = 0f
    private var transformedRight = 0f
    private var transformedBottom = 0f

    /**
     * @property isZooming: this works as a flag for the image zooming. when it becomes true, we calculate the zoom ratio in onDraw function
     * @property zoomScaleFactor: holds the zoom scale ratio.
     * @property zoomCenterX: holds the zoom center x value for equal zooming on all sides
     * @property zoomCenterY: holds the zoom center y value for equal zooming on all sides
     * @property focusShiftX: holds the zoom x-axis value
     * @property focusShiftY: holds the zoom y-axis value
     * @property cumulativeScaleFactor: holds the sum of the zoom scale factor
     * @property rotationAngleDelta: holds the value of the rotation
     * @property isRotating: indicator for a rotating feature of the image
     */
    private var isZooming = false
    private var zoomScaleFactor = 0f
    private var zoomCenterX = 0f
    private var zoomCenterY = 0f
    private var focusShiftX = 0f
    private var focusShiftY = 0f
    private var cumulativeScaleFactor = 1f
    private var rotationAngleDelta = 0f
    private var isRotating = false

    /**
     * @property originalFrameWidth: holds the frame width before the size of the view is changed
     * @property originalUserImageWidth: holds the user image width before the size of the view is changed
     * @property newFrameWidth: holds the width of the frame after the size of the view is changed
     * @property newUserImageWidth: holds the width of the user image after the size of the view is changed.
     */
    private var originalFrameWidth = 0f
    private var originalUserImageWidth = 0f

    // Method to adjust drag values when the view is resized or visibility changes
    private var newFrameWidth = 0f
    private var newUserImageWidth = 0f

    /**
     * @property viewModel: holds the user selected image during configuration changes
     */
    private val viewModel: TemplateViewModel by lazy {
        ViewModelProvider(context as ViewModelStoreOwner)[TemplateViewModel::class.java]
    }

    /**
     * Set Backgrounds
     */
    override fun setBackgroundResource(@DrawableRes resId: Int) {
        backgroundBitmap = BitmapFactory.decodeResource(resources, resId)
        updateBackgroundRect()
        invalidate()
    }

    /**
     * Set Backgrounds from the model received from server
     */
    fun setBackgroundFromModel(model: TemplateModel) {
        // Extract the necessary data from the model and set the background accordingly
        templateModel = model
        backgroundBitmap = BitmapFactory.decodeResource(resources, model.bgImage)
        updateBackgroundRect()
        invalidate()
    }

    fun setBackgroundBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            Log.e(TAG, "TemplateView: setBackgroundBitmap: ", NullPointerException("Bitmap is Null"))
            return
        }
        backgroundBitmap = bitmap
        updateBackgroundRect()
        invalidate()
    }

    @Deprecated("Deprecated in Java")
    override fun setBackgroundDrawable(drawable: Drawable?) {
        if (drawable == null) {
            Log.e(TAG, "TemplateView: setBackgroundDrawable: ", NullPointerException("Drawable is Null"))
            return
        }
        backgroundBitmap = imageUtils.createBitmapFromDrawable(drawable)
        updateBackgroundRect()
        invalidate()
    }

    /**
     * Set User Images
     */

    fun setImageResource(@DrawableRes imageId: Int) {
        imageDrawable = ContextCompat.getDrawable(context, imageId)

        // We have to control/set the original width of the image to zero before the updateUserImageRect() function to show the full image in a frame, everytime user change the image
        originalFrameWidth = 0f
        originalUserImageWidth = 0f

        coroutineScope.launch {
            updateUserImageRect()
            dragValueX = 0f
            dragValueY = 0f
            invalidate()
        }
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            imageBitmap = it
            imageAspectRatio = it.width.toFloat() / it.height.toFloat()
            imageDrawable = imageUtils.createDrawableFromBitmap(it)

            // We have to control/set the original width of the image to zero before the updateUserImageRect() function to show the full image in a frame, everytime user change the image
            originalFrameWidth = 0f
            originalUserImageWidth = 0f

            coroutineScope.launch {
                updateUserImageRect()
                dragValueX = 0f
                dragValueY = 0f
                invalidate()
            }
        }
    }

    fun setImageDrawable(drawable: Drawable?) {
        if (drawable == null) {
            Log.e(TAG, "TemplateView: setImageDrawable: ", NullPointerException("Drawable is Null"))
            return
        }
        imageDrawable = drawable

        // We have to control/set the original width of the image to zero before the updateUserImageRect() function to show the full image in a frame, everytime user change the image
        originalFrameWidth = 0f
        originalUserImageWidth = 0f

        coroutineScope.launch {
            updateUserImageRect()
            dragValueX = 0f
            dragValueY = 0f
            invalidate()
        }
    }

    private fun updateBackgroundRect() {
        backgroundBitmap?.let { backgroundRect.set(0f, 0f, it.width.toFloat(), it.height.toFloat()) }
        matrix.setRectToRect(backgroundRect, viewRect, Matrix.ScaleToFit.CENTER)
    }

    private suspend fun updateUserImageRect() {
        if (imageRectFix.isEmpty) return
        Log.d(TAG, "updateUserImageRect: is called")

        coroutineScope.launch {
            Log.d(TAG, "updateUserImageRect: coroutine is launched")
            // Calculate the available width and height while maintaining aspect ratio
            var availableHeight = imageRectFix.height().toInt()
            var availableWidth = (availableHeight * imageAspectRatio).toInt()

            if (availableWidth < imageRectFix.width()) {
                availableWidth = imageRectFix.width().toInt()
                availableHeight = (availableWidth / imageAspectRatio).toInt()
            }

            // Calculate the position to center the imageRect inside imageRectFix
            val left = imageRectFix.centerX() - (availableWidth / 2f)
            val top = imageRectFix.centerY() - (availableHeight / 2f)
            val right = left + availableWidth
            val bottom = top + availableHeight

            // Set the calculated values to imageRect
            imageRect.set(left, top, right, bottom)

            // The below code is to ensure the drag position of the user selected image inside a frame.
            originalFrameWidth = newFrameWidth
            originalUserImageWidth = newUserImageWidth
            newFrameWidth = imageRectFix.width()
            newUserImageWidth = imageRect.width()

        }.join()

        Log.d(TAG, "updateUserImageRect: coroutine is finished")
    }

    private suspend fun setImageFixRectangle() {
        coroutineScope.launch {
            Log.d(TAG, "setImageFixRectangle: coroutine is launched")
            // Get the matrix values
            matrix.getValues(matrixValues)

            // Extract the scaling factors
            val scaleX = matrixValues[Matrix.MSCALE_X]
            val scaleY = matrixValues[Matrix.MSCALE_Y]

            // Calculate the transformed dimensions of imageRect
            transformedWidth = backgroundRect.width() * scaleX
            transformedHeight = backgroundRect.height() * scaleY

            // Calculate the coordinates for the user's image space based on the device's screen size
            templateModel?.let {
                val userImageSpaceWidth = transformedWidth * (it.frameWidth / it.width)
                val userImageSpaceHeight = transformedHeight * (it.frameHeight / it.height)
                val userImageSpaceX = transformedWidth * (it.frameX / it.width)
                val userImageSpaceY = transformedHeight * (it.frameY / it.height)

                // Calculate the coordinates for the user's image
                val userImageRight = userImageSpaceX + userImageSpaceWidth
                val userImageBottom = userImageSpaceY + userImageSpaceHeight

                imageRectFix.set(userImageSpaceX, userImageSpaceY, userImageRight, userImageBottom)
            }

        }.join()
        Log.d(TAG, "setImageFixRectangle: is finished")
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        Log.d(TAG, "onMeasure: is called")

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        val viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (backgroundBitmap!!.width > 0) templateAspectRatio = backgroundBitmap!!.height.toFloat() / backgroundBitmap!!.width.toFloat()

        val measuredWidth: Int
        val measuredHeight: Int

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            // If both width and height are fixed (e.g., match_parent or specific dimensions)
            if (viewWidth == deviceScreenWidth && viewHeight == deviceScreenHeight) {
                measuredWidth = deviceScreenWidth
                measuredHeight = (measuredWidth * templateAspectRatio).toInt()
            } else {
                measuredWidth = viewWidth
                measuredHeight = viewHeight
            }
        } else if (widthMode == MeasureSpec.EXACTLY) {
            // If width is fixed (e.g., match_parent or specific dimension)
            measuredWidth = viewWidth
            measuredHeight = (measuredWidth * templateAspectRatio).toInt()
        } else if (heightMode == MeasureSpec.EXACTLY) {
            // If height is fixed (e.g., match_parent or specific dimension)
            measuredHeight = viewHeight
            measuredWidth = (measuredHeight / templateAspectRatio).toInt()
        } else {
            // If both width and height are wrap_content
            measuredWidth = if (suggestedMinimumWidth != 0) suggestedMinimumWidth else {
                if (deviceScreenWidth > backgroundBitmap!!.width) backgroundBitmap!!.width else deviceScreenWidth
            }
            measuredHeight = (measuredWidth * templateAspectRatio).toInt()
        }
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged: is called")

        imageRect.setEmpty()
        viewRect.set(0f, 0f, width.toFloat(), height.toFloat())
        matrix.setRectToRect(backgroundRect, viewRect, Matrix.ScaleToFit.CENTER)

        // Here we make isConfigurationTrigger to true, bcz we want to perform operations everytime the size changed
        isConfigurationTrigger = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw: is called")

        // Draw the background template image.
        backgroundBitmap?.let { canvas.drawBitmap(it, matrix, null) }

        if (imageRect.isEmpty) {
            coroutineScope.launch {
                Log.d(TAG, "onDraw: second coroutine is launched")
                setImageFixRectangle()
                updateUserImageRect()
                Log.d(TAG, "onDraw: second coroutine is finished")
                invalidate()
            }
        }
        /**
         * If, due to any reason, the view size has changed. and therefore,
         * we have to make sure the dragX and dragY value according to size or ratio.
         */
        if (isConfigurationTrigger) {
            if (originalFrameWidth != 0f && originalUserImageWidth != 0f) {
                dragValueX *= newFrameWidth / originalFrameWidth
                dragValueY *= newUserImageWidth / originalUserImageWidth
            }
            if (dragValueX != 0f || dragValueY != 0f) imageRect.offset(dragValueX, dragValueY)
            isConfigurationTrigger = false
        }

        // Save the current state of the canvas
        canvas.save()

        // Clip the drawing of the selected image to the template bounds.
        canvas.clipRect(imageRectFix)

        // Apply the matrix for zooming, rotation, and translation
        canvas.concat(imageMatrix)

        // Set the bounds for the selected image drawable.
        imageDrawable?.bounds = imageRect.toRect()

        // Draw the image
        imageDrawable?.draw(canvas)

        // Restore the canvas to its original state
        canvas.restore()
    }

    override fun onSaveInstanceState(): Parcelable {
        Log.d(TAG, "onSaveInstanceState: is called")
        // Save the user selected image in a view model.
        viewModel.updateImage(imageBitmap)

        return CustomViewState(super.onSaveInstanceState()).apply {
            imageAspectRatio = this@TemplateView.imageAspectRatio
            scaleFactor = this@TemplateView.zoomScaleFactor
            zoomCenterX = this@TemplateView.zoomCenterX
            zoomCenterY = this@TemplateView.zoomCenterY
            dx = dragValueX
            dy = dragValueY
            rotationAngle = rotationAngleDelta

            Log.d(TAG, "onSaveInstanceState: zoomCenterX: $zoomCenterX")
            Log.d(TAG, "onSaveInstanceState: zoomCenterY: $zoomCenterY")
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        Log.d(TAG, "onRestoreInstanceState: is called")
        if (state is CustomViewState) {
            super.onRestoreInstanceState(state.superState)
            // Get the image from the view-model
            imageBitmap = viewModel.getImage()
            imageBitmap?.let { imageDrawable = imageUtils.createDrawableFromBitmap(it) }
            imageAspectRatio = state.imageAspectRatio
            zoomScaleFactor = state.scaleFactor
            zoomCenterX = state.zoomCenterX
            zoomCenterY = state.zoomCenterY
            dragValueX = state.dx
            dragValueY = state.dy
            rotationAngleDelta = state.rotationAngle
            isConfigurationTrigger = true
        } else super.onRestoreInstanceState(state)
    }

    private fun updateMatrix() {
        Log.d(TAG, "updateMatrix: is called")
            // Assuming you have variables for scale, rotation, and translation
            val matrix = Matrix()
            // Apply scaling
            if (zoomCenterX != 0f || zoomCenterY != 0f) matrix.postScale(zoomScaleFactor, zoomScaleFactor, zoomCenterX, zoomCenterY)
            // Apply rotation
            if (rotationAngleDelta != 0f && (zoomCenterX != 0f || zoomCenterY != 0f)) matrix.postRotate(rotationAngleDelta, zoomCenterX, zoomCenterY)
            else if (rotationAngleDelta != 0f) matrix.postRotate(rotationAngleDelta, imageRectFix.width() / 2f, imageRectFix.height() / 2f)

            // Apply translation (dragging)
            if (isDragging) matrix.postTranslate(dragValueX, dragValueY)

            // Set the matrix for your custom view
            imageMatrix = matrix

            // Trigger a redraw
            invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {

        event?.let {
            if (it.pointerCount > 1) {
                // Handle the two-finger zoom gesture
                if (imageRectFix.contains(event.x, event.y)) {
                    scaleGestureDetector.onTouchEvent(it)
                    // Handle the two-finger rotation gesture
                    rotationGestureDetector.onTouchEvent(it)
                }
                // return from the below one finger conditions when two fingers are on the screen
                return true
            }
        }

        // Attach the double tap gesture detector
        event?.let { doubleTapGestureDetector.onTouchEvent(it) }

        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if the touch event is within the selected image bounds.
                if (imageRectFix.contains(event.x, event.y)) {
                    Log.d(TAG, "onTouchEvent: ACTION_DOWN is called")
                    isDragging = true
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isRotating = false
                isZooming = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging && !isZooming) {
                    Log.d(TAG, "onTouchEvent: ACTION_MOVE is called")
                    // Calculate the distance moved.
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    dragValueX += dx
                    dragValueY += dy

                    // Update the last touch position.
                    lastTouchX = event.x
                    lastTouchY = event.y

                    // Invalidate the view to trigger a redraw.
                    updateMatrix()
                }
            }
        }
        // Consume the event to indicate that it's been handled.
        return true
    }

    // Initialize a scale gesture detector for pinch-to-zoom
    private val scaleGestureDetector: ScaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            zoomCenterX = detector.focusX
            zoomCenterY = detector.focusY
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            Log.d(TAG, "onScale: cumulativeScaleFactor: $cumulativeScaleFactor")
            cumulativeScaleFactor *= detector.scaleFactor
            cumulativeScaleFactor = cumulativeScaleFactor.coerceIn(0.1f, 10f) // Adjust the limits as needed
            zoomScaleFactor = cumulativeScaleFactor
            isZooming = true
            updateMatrix()
            return true
        }
    })

    // Initialize a gesture detector for double-tap
    private val doubleTapGestureDetector: GestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d(TAG, "onDoubleTap: is called")
            if (imageRectFix.contains(e.x, e.y)) {
                dragValueX = 0f
                dragValueY = 0f
                zoomScaleFactor = 0f
                rotationAngleDelta = 0f
                zoomCenterX = 0f
                zoomCenterY = 0f
                cumulativeScaleFactor = 1f
                imageMatrix.reset()
                invalidate()
            }
            return true
        }
    })

    // Initialize a rotation detector
    private val rotationGestureDetector = RotationGestureDetector(object : RotationGestureDetector.OnRotationGestureListener {
        override fun onRotation(rotationAngle: Float) {
            // Handle the rotation gesture here.
            rotationAngleDelta = rotationAngle
            isRotating = true
            updateMatrix()
        }
    })

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope.cancel()
    }

}