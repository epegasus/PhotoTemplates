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
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dev.pegasus.template.dataClasses.TemplateModel
import dev.pegasus.template.touchImageView.TouchImageView
import dev.pegasus.template.utils.HelperUtils
import dev.pegasus.template.utils.ImageUtils
import dev.pegasus.template.viewModels.TemplateViewModel

class ComponentTemplateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

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

    // Add a TouchImageView for the background template
    private val templateImageView: TouchImageView by lazy { TouchImageView(context) }

    // Add a TouchImageView for the user image
    private val userImageView: TouchImageView by lazy {
        TouchImageView(context).apply {
            // Customize TouchImageView properties if needed
            // For example, you can set the maximum and minimum zoom levels:
            maxZoom = 4f
            minZoom = 0.1f
            isZoomEnabled = true
            scaleType = ImageView.ScaleType.FIT_XY
            setRotateImageToFitScreen(true)
            setZoom(this)
            isSuperZoomEnabled = true
        }
    }

    /**
     * Set Backgrounds
     */
    override fun setBackgroundResource(@DrawableRes resId: Int) {
        backgroundBitmap = BitmapFactory.decodeResource(resources, resId)
        requestLayout()
        invalidate()
    }

    /**
     * Set Backgrounds from the model received from server
     */
    fun setBackgroundFromModel(model: TemplateModel) {
        // Extract the necessary data from the model and set the background accordingly
        templateModel = model
        backgroundBitmap = BitmapFactory.decodeResource(resources, model.bgImage)
        requestLayout()
        invalidate()
    }

    @Deprecated("Deprecated in Java")
    override fun setBackgroundDrawable(drawable: Drawable?) {
        if (drawable == null) {
            Log.e(HelperUtils.TAG, "TemplateView: setBackgroundDrawable: ", NullPointerException("Drawable is Null"))
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
        // We have to control/set the original width of the image to zero before the updateUserImageRect() function to show the full image in a frame, everytime user change the image
        requestLayout()
        invalidate()
    }

    fun setImageBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            imageBitmap = it
            userImageView.setImageBitmap(it)
            requestLayout()
            invalidate()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        Log.d(HelperUtils.TAG, "onMeasure: is called")

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
        Log.d(HelperUtils.TAG, "onSizeChanged: is called")

        /*if (templateImageView.parent != null) removeView(templateImageView)
        if (userImageView.parent != null) removeView(userImageView)*/

        // Check if the views are already added, if not, add them
        if (templateImageView.parent == null) {
            // Add the background template ImageView
            val layoutParamsBackground = LayoutParams(w, h)
            layoutParamsBackground.gravity = Gravity.CENTER
            templateImageView.layoutParams = layoutParamsBackground
            templateImageView.setImageBitmap(backgroundBitmap)
            addView(templateImageView)
        }

        if (userImageView.parent == null) {
            // Add the user image ImageView
            templateModel?.let {
                val userImageSpaceWidth = w * (it.frameWidth / it.width)
                val userImageSpaceHeight = h * (it.frameHeight / it.height)
                val userImageSpaceX = w * (it.frameX / it.width)
                val userImageSpaceY = h * (it.frameY / it.height)

                // Calculate the coordinates for the user's image
                val userImageRight = userImageSpaceX + userImageSpaceWidth
                val userImageBottom = userImageSpaceY + userImageSpaceHeight

                imageRectFix.set(userImageSpaceX, userImageSpaceY, userImageRight, userImageBottom)
            }

            val userImageWidth = imageRectFix.width().toInt()
            val userImageHeight = imageRectFix.height().toInt()

            val layoutParamsImage = LayoutParams(userImageWidth, userImageHeight)
            layoutParamsImage.leftMargin = imageRectFix.left.toInt()
            layoutParamsImage.topMargin = imageRectFix.top.toInt()

            userImageView.layoutParams = layoutParamsImage
            userImageView.setImageBitmap(imageBitmap)
            addView(userImageView)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(HelperUtils.TAG, "onDraw: is called")
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        userImageView.onTouchEvent(event)
        // Consume the event to indicate that it's been handled.
        return true
    }

}
