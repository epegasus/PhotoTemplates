package dev.pegasus.stickers

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import dev.pegasus.stickers.helper.Sticker
import dev.pegasus.stickers.helper.StickerUtils
import dev.pegasus.stickers.helper.events.DeleteIconEvent
import dev.pegasus.stickers.helper.events.DuplicateIconEvent
import dev.pegasus.stickers.helper.events.ZoomAndRotateIconEvent
import dev.pegasus.stickers.ui.BitmapStickerIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Text
import java.io.File
import java.util.Arrays
import java.util.Collections
import kotlin.math.abs

class StickerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var lifecycleOwner: LifecycleOwner
    fun setLifecycleOwner(owner: LifecycleOwner) {
        lifecycleOwner = owner
    }

    private var showIcons = false
    private var showBorder = false
    private var bringToFrontCurrentSticker = false

    @IntDef(NONE, DRAG, ZOOM_WITH_TWO_FINGER, ICON, CLICK)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ActionMode

    @IntDef(flag = true, value = [FLIP_HORIZONTALLY, FLIP_VERTICALLY])
    @Retention(AnnotationRetention.SOURCE)
    annotation class Flip

    companion object {
        private const val TAG = "StickerView"
        private const val DEFAULT_MIN_CLICK_DELAY_TIME = 200

        const val FLIP_HORIZONTALLY = 1
        const val FLIP_VERTICALLY = 1 shl 1

        const val NONE = 0
        const val DRAG = 1
        const val ZOOM_WITH_TWO_FINGER = 2
        const val ICON = 3
        const val CLICK = 4
    }

    var icons = ArrayList<BitmapStickerIcon>(4)
    private val stickers: MutableList<Sticker?> = ArrayList()
    private val borderPaint = Paint()
    private val stickerRect = RectF()
    private val sizeMatrix = Matrix()
    private val downMatrix = Matrix()
    private val moveMatrix = Matrix()

    // region storing variables
    private val bitmapPoints = FloatArray(8)
    private val bounds = FloatArray(8)
    private val point = FloatArray(2)
    private val currentCenterPoint = PointF()
    private val tmp = FloatArray(2)
    private var midPoint = PointF()

    // endregion
    private val touchSlop: Int
    private var currentIcon: BitmapStickerIcon? = null

    //the first point down position
    private var downX = 0f
    private var downY = 0f
    private var oldDistance = 0f
    private var oldRotation = 0f

    @ActionMode
    private var currentMode = NONE
    var currentSticker: Sticker? = null
        private set
    var isLocked = false
    var isConstrained = false
    var onStickerOperationListener: OnStickerOperationListener? = null
    private var lastClickTime: Long = 0
    var minClickDelayTime = DEFAULT_MIN_CLICK_DELAY_TIME
        private set

    init {
        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        var a: TypedArray? = null
        try {
            a = context.obtainStyledAttributes(attrs, R.styleable.StickerView)
            showIcons = a.getBoolean(R.styleable.StickerView_showIcons, false)
            showBorder = a.getBoolean(R.styleable.StickerView_showBorder, false)
            bringToFrontCurrentSticker = a.getBoolean(R.styleable.StickerView_bringToFrontCurrentSticker, false)
            borderPaint.isAntiAlias = true
            borderPaint.color = a.getColor(R.styleable.StickerView_borderColor, Color.BLACK)
            borderPaint.alpha = a.getInteger(R.styleable.StickerView_borderAlpha, 128)
            borderPaint.strokeWidth = 2.0f
            configDefaultIcons()
        } finally {
            a?.recycle()
        }
    }

    fun showBorder(isBorder: Boolean) {
        showBorder = isBorder
        showIcons = isBorder
        invalidate()
    }

    private fun configDefaultIcons() {
        /*
        Old by Sohaib
        val deleteIcon = BitmapStickerIcon(ContextCompat.getDrawable(context, R.drawable.ic_cross), BitmapStickerIcon.LEFT_TOP)
        deleteIcon.iconEvent = DeleteIconEvent()
        val zoomIcon = BitmapStickerIcon(ContextCompat.getDrawable(context, R.drawable.ic_zoom), BitmapStickerIcon.RIGHT_BOTTOM)
        zoomIcon.iconEvent = ZoomIconEvent()
        val flipIcon = BitmapStickerIcon(ContextCompat.getDrawable(context, R.drawable.ic_flip), BitmapStickerIcon.RIGHT_TOP)
        flipIcon.iconEvent = FlipHorizontallyEvent()
        icons.clear()
        icons.add(deleteIcon)
        icons.add(zoomIcon)
        icons.add(flipIcon)*/


        val deleteIcon = BitmapStickerIcon(ContextCompat.getDrawable(context, R.drawable.ic_cross), BitmapStickerIcon.LEFT_TOP)
        deleteIcon.iconEvent = DeleteIconEvent()
        val duplicateIcon = BitmapStickerIcon(ContextCompat.getDrawable(context, R.drawable.ic_copy), BitmapStickerIcon.RIGHT_TOP)
        //duplicateIcon.iconEvent = DuplicateIconEvent()
        val zoomIcon = BitmapStickerIcon(ContextCompat.getDrawable(context, R.drawable.ic_expand), BitmapStickerIcon.RIGHT_BOTTOM)
        zoomIcon.iconEvent = ZoomAndRotateIconEvent()
        val editIcon = BitmapStickerIcon(ContextCompat.getDrawable(context, R.drawable.ic_edit), BitmapStickerIcon.LEFT_BOTTOM)

        icons.clear()
        icons.add(deleteIcon)
        icons.add(duplicateIcon)
        icons.add(zoomIcon)
        icons.add(editIcon)
    }

    /**
     * Swaps sticker at layer [[oldPos]] with the one at layer [[newPos]].
     * Does nothing if either of the specified layers doesn't exist.
     */
    fun swapLayers(oldPos: Int, newPos: Int) {
        if (stickers.size >= oldPos && stickers.size >= newPos) {
            Collections.swap(stickers, oldPos, newPos)
            invalidate()
        }
    }

    /**
     * Sends sticker from layer [[oldPos]] to layer [[newPos]].
     * Does nothing if either of the specified layers doesn't exist.
     */
    fun sendToLayer(oldPos: Int, newPos: Int) {
        if (stickers.size >= oldPos && stickers.size >= newPos) {
            val s = stickers[oldPos]
            stickers.removeAt(oldPos)
            stickers.add(newPos, s)
            invalidate()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            stickerRect.left = left.toFloat()
            stickerRect.top = top.toFloat()
            stickerRect.right = right.toFloat()
            stickerRect.bottom = bottom.toFloat()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawStickers(canvas)
    }

    protected fun drawStickers(canvas: Canvas) {
        for (i in stickers.indices) {
            val sticker = stickers[i]
            sticker?.draw(canvas)
        }
        if (currentSticker != null && !isLocked && (showBorder || showIcons)) {
            getStickerPoints(currentSticker, bitmapPoints)
            val x1 = bitmapPoints[0]
            val y1 = bitmapPoints[1]
            val x2 = bitmapPoints[2]
            val y2 = bitmapPoints[3]
            val x3 = bitmapPoints[4]
            val y3 = bitmapPoints[5]
            val x4 = bitmapPoints[6]
            val y4 = bitmapPoints[7]
            if (showBorder) {
                canvas.drawLine(x1, y1, x2, y2, borderPaint)
                canvas.drawLine(x1, y1, x3, y3, borderPaint)
                canvas.drawLine(x2, y2, x4, y4, borderPaint)
                canvas.drawLine(x4, y4, x3, y3, borderPaint)
            }

            //draw icons
            if (showIcons) {
                val rotation = calculateRotation(x4, y4, x3, y3)
                for (i in icons.indices) {
                    val icon = icons[i]
                    when (icon.position) {
                        BitmapStickerIcon.LEFT_TOP -> configIconMatrix(icon, x1, y1, rotation)
                        BitmapStickerIcon.RIGHT_TOP -> configIconMatrix(icon, x2, y2, rotation)
                        BitmapStickerIcon.LEFT_BOTTOM -> configIconMatrix(icon, x3, y3, rotation)
                        BitmapStickerIcon.RIGHT_BOTTOM -> configIconMatrix(icon, x4, y4, rotation)
                    }
                    icon.draw(canvas, borderPaint)
                }
            }
        }
    }

    protected fun configIconMatrix(icon: BitmapStickerIcon, x: Float, y: Float, rotation: Float) {
        icon.x = x
        icon.y = y
        icon.matrix.reset()
        icon.matrix.postRotate(rotation, (icon.width / 2).toFloat(), (icon.height / 2).toFloat())
        icon.matrix.postTranslate(x - icon.width / 2, y - icon.height / 2)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isLocked) return super.onInterceptTouchEvent(ev)
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                return findCurrentIconTouched() != null || findHandlingSticker() != null
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) {
            return super.onTouchEvent(event)
        }
        val action = MotionEventCompat.getActionMasked(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> if (!onTouchDown(event)) {
                return false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                /*
                old by Sohaib
                oldDistance = calculateDistance(event)
                oldRotation = calculateRotation(event)
                midPoint = calculateMidPoint(event)
                if (currentSticker != null && isInStickerArea(currentSticker!!, event.getX(1), event.getY(1)) && findCurrentIconTouched() == null) {
                    currentMode = ZOOM_WITH_TWO_FINGER
                }*/
            }

            MotionEvent.ACTION_MOVE -> {
                handleCurrentMode(event)
                invalidate()
            }

            MotionEvent.ACTION_UP -> onTouchUp(event)
            MotionEvent.ACTION_POINTER_UP -> {
                /*
                Old by Sohaib
                if (currentMode == ZOOM_WITH_TWO_FINGER && currentSticker != null) {
                    if (onStickerOperationListener != null) {
                        onStickerOperationListener!!.onStickerZoomFinished(currentSticker!!)
                    }
                }*/
                currentMode = NONE
            }
        }
        return true
    }

    /**
     * @param event MotionEvent received from [)][.onTouchEvent]
     */
    protected fun onTouchDown(event: MotionEvent): Boolean {
        currentMode = DRAG
        downX = event.x
        downY = event.y
        midPoint = calculateMidPoint()
        oldDistance = calculateDistance(midPoint.x, midPoint.y, downX, downY)
        oldRotation = calculateRotation(midPoint.x, midPoint.y, downX, downY)
        currentIcon = findCurrentIconTouched()
        if (currentIcon != null) {
            currentMode = ICON
            currentIcon!!.onActionDown(this, event)
        } else {
            currentSticker = findHandlingSticker()
        }
        if (currentSticker != null) {
            val isUpdate: Boolean = currentIcon?.position == BitmapStickerIcon.LEFT_BOTTOM
            val isDuplicate: Boolean = currentIcon?.position == BitmapStickerIcon.RIGHT_TOP
            onStickerOperationListener!!.onStickerTouchedDown(currentSticker!!, isUpdate, isDuplicate)
            downMatrix.set(currentSticker!!.matrix)
            if (bringToFrontCurrentSticker) {
                stickers.remove(currentSticker)
                stickers.add(currentSticker)
            }
        }
        if (currentIcon == null && currentSticker == null) {
            return false
        }
        invalidate()
        return true
    }

    protected fun onTouchUp(event: MotionEvent) {
        val currentTime = SystemClock.uptimeMillis()
        if (currentMode == ICON && currentIcon != null && currentSticker != null) {
            currentIcon!!.onActionUp(this, event)
        }
        if (currentMode == DRAG && Math.abs(event.x - downX) < touchSlop && abs(event.y - downY) < touchSlop && currentSticker != null) {
            currentMode = CLICK
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerClicked(currentSticker!!)
            }
            if (currentTime - lastClickTime < minClickDelayTime) {
                if (onStickerOperationListener != null) {
                    onStickerOperationListener!!.onStickerDoubleTapped(currentSticker!!)
                }
            }
        }
        if (currentMode == DRAG && currentSticker != null) {
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerDragFinished(currentSticker!!)
            }
        }
        currentMode = NONE
        lastClickTime = currentTime
    }

    protected fun handleCurrentMode(event: MotionEvent) {
        when (currentMode) {
            NONE, CLICK -> {}
            DRAG -> if (currentSticker != null) {
                moveMatrix.set(downMatrix)
                moveMatrix.postTranslate(event.x - downX, event.y - downY)
                currentSticker!!.setMatrix(moveMatrix)
                if (isConstrained) {
                    constrainSticker(currentSticker!!)
                }
            }

            ZOOM_WITH_TWO_FINGER -> if (currentSticker != null) {
                val newDistance = calculateDistance(event)
                val newRotation = calculateRotation(event)
                moveMatrix.set(downMatrix)
                moveMatrix.postScale(newDistance / oldDistance, newDistance / oldDistance, midPoint.x, midPoint.y)
                moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y)
                currentSticker!!.setMatrix(moveMatrix)
            }

            ICON -> if (currentSticker != null && currentIcon != null) {
                currentIcon!!.onActionMove(this, event)
            }
        }
    }

    fun zoomAndRotateCurrentSticker(event: MotionEvent) {
        zoomAndRotateSticker(currentSticker, event, 0)
    }

    fun rotateCurrentSticker(event: MotionEvent) {
        zoomAndRotateSticker(currentSticker, event, 1)
    }

    fun zoomAndRotateSticker(sticker: Sticker?, event: MotionEvent, caseType: Int) {
        /*
        Old by Sohaib
        if (sticker != null) {
            downMatrix.set(currentSticker!!.matrix)
            val newDistance = calculateDistance(midPoint.x, midPoint.y, event.x, event.y)
            val newRotation = calculateRotation(midPoint.x, midPoint.y, event.x, event.y)
            Log.e("HVV1312", "OK ???: " + midPoint.x + " va " + event.x)
            moveMatrix.set(downMatrix)
            if (caseType == 0) moveMatrix.postScale(newDistance / oldDistance, newDistance / oldDistance, midPoint.x, midPoint.y) else moveMatrix.postRotate((newRotation - oldRotation) / 20, midPoint.x, midPoint.y)
            currentSticker!!.setMatrix(moveMatrix)
        }*/

        if (sticker != null) {
            downMatrix.set(currentSticker?.matrix)
            val newDistance = calculateDistance(midPoint.x, midPoint.y, event.x, event.y)
            val newRotation = calculateRotation(midPoint.x, midPoint.y, event.x, event.y)
            // to not drag or zoom when just a finger touch on the same area
            if (newDistance == oldDistance) return
            Log.e("HVV1312", "OK ???: " + midPoint.x + " va " + event.x)
            moveMatrix.set(downMatrix)
            moveMatrix.postScale(newDistance / oldDistance, newDistance / oldDistance, midPoint.x, midPoint.y)
            if (abs(newRotation.toDouble()) > 0.01f) moveMatrix.postRotate(newRotation - oldRotation, midPoint.x, midPoint.y)
            currentSticker?.setMatrix(moveMatrix)
            oldDistance = newDistance
            oldRotation = newRotation
        }
    }

    fun customeZoomAndRotateSticker(sticker: Sticker?, x: Int, y: Int, m: Int, n: Int) {
        if (sticker != null) {
            val oldD = calculateDistance(midPoint.x, midPoint.y, m.toFloat(), n.toFloat())
            Log.e("HVV1312", "mid Point : " + midPoint.x + " va " + midPoint.y + " va va va m: " + m + " va n: " + n)
            val newDistance = calculateDistance(midPoint.x, midPoint.y, x.toFloat(), y.toFloat())
            moveMatrix.set(downMatrix)
            moveMatrix.postScale(newDistance / oldD, newDistance / oldD, midPoint.x, midPoint.y)
            if (currentSticker != null) {
                currentSticker!!.setMatrix(moveMatrix)
            }
            invalidate()
        }
    }

    protected fun constrainSticker(sticker: Sticker) {
        var moveX = 0f
        var moveY = 0f
        val width = width
        val height = height
        sticker.getMappedCenterPoint(currentCenterPoint, point, tmp)
        if (currentCenterPoint.x < 0) {
            moveX = -currentCenterPoint.x
        }
        if (currentCenterPoint.x > width) {
            moveX = width - currentCenterPoint.x
        }
        if (currentCenterPoint.y < 0) {
            moveY = -currentCenterPoint.y
        }
        if (currentCenterPoint.y > height) {
            moveY = height - currentCenterPoint.y
        }
        sticker.matrix.postTranslate(moveX, moveY)
    }

    protected fun findCurrentIconTouched(): BitmapStickerIcon? {
        for (icon in icons) {
            val x = icon.x - downX
            val y = icon.y - downY
            val distance_pow_2 = x * x + y * y
            if (distance_pow_2 <= Math.pow((icon.iconRadius + icon.iconRadius).toDouble(), 2.0)) {
                return icon
            }
        }
        return null
    }

    /**
     * find the touched Sticker
     */
    protected fun findHandlingSticker(): Sticker? {
        for (i in stickers.indices.reversed()) {
            if (isInStickerArea(stickers[i]!!, downX, downY)) {
                return stickers[i]
            }
        }
        return null
    }

    protected fun isInStickerArea(sticker: Sticker, downX: Float, downY: Float): Boolean {
        tmp[0] = downX
        tmp[1] = downY
        return sticker.contains(tmp)
    }

    protected fun calculateMidPoint(event: MotionEvent?): PointF {
        if (event == null || event.pointerCount < 2) {
            midPoint[0f] = 0f
            return midPoint
        }
        val x = (event.getX(0) + event.getX(1)) / 2
        val y = (event.getY(0) + event.getY(1)) / 2
        midPoint[x] = y
        return midPoint
    }

    protected fun calculateMidPoint(): PointF {
        if (currentSticker == null) {
            midPoint[0f] = 0f
            return midPoint
        }
        currentSticker!!.getMappedCenterPoint(midPoint, point, tmp)
        return midPoint
    }

    /**
     * calculate rotation in line with two fingers and x-axis
     */
    protected fun calculateRotation(event: MotionEvent?): Float {
        return if (event == null || event.pointerCount < 2) {
            0f
        } else calculateRotation(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    protected fun calculateRotation(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x1 - x2).toDouble()
        val y = (y1 - y2).toDouble()
        val radians = Math.atan2(y, x)
        return Math.toDegrees(radians).toFloat()
    }

    /**
     * calculate Distance in two fingers
     */
    protected fun calculateDistance(event: MotionEvent?): Float {
        return if (event == null || event.pointerCount < 2) {
            0f
        } else calculateDistance(event.getX(0), event.getY(0), event.getX(1), event.getY(1))
    }

    protected fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x1 - x2).toDouble()
        val y = (y1 - y2).toDouble()
        return Math.sqrt(x * x + y * y).toFloat()
    }

    protected fun calculateDistance2(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = (x1 - x2).toDouble()
        val y = (y1 - y2).toDouble()
        return Math.sqrt(x * x + y * y).toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        for (i in stickers.indices) {
            val sticker = stickers[i]
            sticker?.let { transformSticker(it) }
        }
    }

    /**
     * Sticker's drawable will be too bigger or smaller
     * This method is to transform it to fit
     * step 1：let the center of the sticker image is coincident with the center of the View.
     * step 2：Calculate the zoom and zoom
     */
    protected fun transformSticker(sticker: Sticker?) {
        if (sticker == null) {
            Log.e(TAG, "transformSticker: the bitmapSticker is null or the bitmapSticker bitmap is null")
            return
        }
        sizeMatrix.reset()
        val width = width.toFloat()
        val height = height.toFloat()
        val stickerWidth = sticker.width.toFloat()
        val stickerHeight = sticker.height.toFloat()
        //step 1
        val offsetX = (width - stickerWidth) / 2
        val offsetY = (height - stickerHeight) / 2
        sizeMatrix.postTranslate(offsetX, offsetY)

        //step 2
        val scaleFactor: Float
        scaleFactor = if (width < height) {
            width / stickerWidth
        } else {
            height / stickerHeight
        }
        sizeMatrix.postScale(scaleFactor / 2f, scaleFactor / 2f, width / 2f, height / 2f)
        sticker.matrix.reset()
        sticker.setMatrix(sizeMatrix)
        invalidate()
    }

    fun flipCurrentSticker(direction: Int) {
        flip(currentSticker, direction)
    }

    fun flip(sticker: Sticker?, @Flip direction: Int) {
        if (sticker != null) {
            sticker.getCenterPoint(midPoint)
            if (direction and FLIP_HORIZONTALLY > 0) {
                sticker.matrix.preScale(-1f, 1f, midPoint.x, midPoint.y)
                sticker.isFlippedHorizontally = !sticker.isFlippedHorizontally
            }
            if (direction and FLIP_VERTICALLY > 0) {
                sticker.matrix.preScale(1f, -1f, midPoint.x, midPoint.y)
                sticker.isFlippedVertically = !sticker.isFlippedVertically
            }
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerFlipped(sticker)
            }
            invalidate()
        }
    }

    @JvmOverloads
    fun replace(sticker: Sticker?, needStayState: Boolean = true): Boolean {
        return if (currentSticker != null && sticker != null) {
            val width = width.toFloat()
            val height = height.toFloat()
            if (needStayState) {
                sticker.setMatrix(currentSticker!!.matrix)
                sticker.isFlippedVertically = currentSticker!!.isFlippedVertically
                sticker.isFlippedHorizontally = currentSticker!!.isFlippedHorizontally
            } else {
                currentSticker!!.matrix.reset()
                // reset scale, angle, and put it in center
                val offsetX = (width - currentSticker!!.width) / 2f
                val offsetY = (height - currentSticker!!.height) / 2f
                sticker.matrix.postTranslate(offsetX, offsetY)
                val scaleFactor: Float
                scaleFactor = if (width < height) {
                    width / currentSticker!!.drawable.intrinsicWidth
                } else {
                    height / currentSticker!!.drawable.intrinsicHeight
                }
                sticker.matrix.postScale(scaleFactor / 2f, scaleFactor / 2f, width / 2f, height / 2f)
            }
            val index = stickers.indexOf(currentSticker)
            stickers[index] = sticker
            currentSticker = sticker
            invalidate()
            true
        } else {
            false
        }
    }

    fun remove(sticker: Sticker?): Boolean {
        return if (stickers.contains(sticker)) {
            stickers.remove(sticker)
            if (onStickerOperationListener != null) {
                onStickerOperationListener!!.onStickerDeleted(sticker!!)
            }
            if (currentSticker === sticker) {
                currentSticker = null
            }
            invalidate()
            true
        } else {
            Log.d(TAG, "remove: the sticker is not in this StickerView")
            false
        }
    }

    fun removeCurrentSticker(): Boolean {
        return remove(currentSticker)
    }

    fun removeAllStickers() {
        stickers.clear()
        if (currentSticker != null) {
            currentSticker!!.release()
            currentSticker = null
        }
        invalidate()
    }

    fun addSticker(sticker: Sticker, stickerToBeDuplicated: Sticker?): StickerView {
        return addSticker(sticker, stickerToBeDuplicated, Sticker.Position.CENTER)
    }

    private fun addSticker(sticker: Sticker, stickerToBeDuplicated: Sticker?, @Sticker.Position position: Int): StickerView {
        if (ViewCompat.isLaidOut(this)) {
            addStickerImmediately(sticker, stickerToBeDuplicated, position)
        } else {
            post { addStickerImmediately(sticker, stickerToBeDuplicated, position) }
        }
        return this
    }

    private fun addStickerImmediately(sticker: Sticker, stickerToBeDuplicated: Sticker?, @Sticker.Position position: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            val scaleFactor: Float
            val widthScaleFactor: Float = width.toFloat() / sticker.drawable.intrinsicWidth
            val heightScaleFactor: Float = height.toFloat() / sticker.drawable.intrinsicHeight
            scaleFactor = if (widthScaleFactor > heightScaleFactor) heightScaleFactor else widthScaleFactor
            sticker.matrix.postScale(scaleFactor / 2, scaleFactor / 2, ((width + 100) / 2).toFloat(), (height / 2).toFloat())

            stickerToBeDuplicated?.let {
                sticker.matrix.set(stickerToBeDuplicated.matrix)
                sticker.matrix.postTranslate(0f, 50f)
            } ?: run {
                setStickerPosition(sticker, position)
            }

            currentSticker = sticker
            stickers.add(sticker)

            withContext(Dispatchers.Main) {
                if (onStickerOperationListener != null) {
                    onStickerOperationListener!!.onStickerAdded(sticker)
                }
                invalidate()
            }
        }
    }

    private fun setStickerPosition(sticker: Sticker, @Sticker.Position position: Int) {
        val width = width.toFloat()
        val height = height.toFloat()
        var offsetX = width - sticker.width
        var offsetY = height - sticker.height
        if (position and Sticker.Position.TOP > 0) {
            offsetY /= 4f
        } else if (position and Sticker.Position.BOTTOM > 0) {
            offsetY *= 3f / 4f
        } else {
            offsetY /= 2f
        }
        if (position and Sticker.Position.LEFT > 0) {
            offsetX /= 4f
        } else if (position and Sticker.Position.RIGHT > 0) {
            offsetX *= 3f / 4f
        } else {
            offsetX /= 2f
        }
        // Reset the matrix before applying new transformations
        sticker.matrix.reset()
        // Apply the translation to the sticker's matrix
        sticker.matrix.postTranslate(offsetX, offsetY)
    }

    fun getStickerPoints(sticker: Sticker?): FloatArray {
        val points = FloatArray(8)
        getStickerPoints(sticker, points)
        return points
    }

    private fun getStickerPoints(sticker: Sticker?, dst: FloatArray) {
        if (sticker == null) {
            Arrays.fill(dst, 0f)
            return
        }
        sticker.getBoundPoints(bounds)
        sticker.getMappedPoints(dst, bounds)
    }

    fun save(file: File) {
        try {
            StickerUtils.saveImageToGallery(file, createBitmap())
            StickerUtils.notifySystemGallery(context, file)
        } catch (ignored: IllegalArgumentException) {
            //
        } catch (ignored: IllegalStateException) {
        }
    }

    @Throws(OutOfMemoryError::class)
    fun createBitmap(): Bitmap {
        currentSticker = null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    val stickerCount: Int
        get() = stickers.size
    val isNoneSticker: Boolean
        get() = stickerCount == 0

    fun setLocked(locked: Boolean): StickerView {
        isLocked = locked
        invalidate()
        return this
    }

    fun setMinClickDelayTime(minClickDelayTime: Int): StickerView {
        this.minClickDelayTime = minClickDelayTime
        return this
    }

    fun setConstrained(constrained: Boolean): StickerView {
        isConstrained = constrained
        postInvalidate()
        return this
    }

    fun setOnStickerOperationListener(onStickerOperationListener: OnStickerOperationListener?): StickerView {
        this.onStickerOperationListener = onStickerOperationListener
        return this
    }

    fun getIcons(): List<BitmapStickerIcon> {
        return icons
    }

    fun setIcons(icons: List<BitmapStickerIcon>) {
        this.icons.clear()
        this.icons.addAll(icons)
        invalidate()
    }

    interface OnStickerOperationListener {
        fun onStickerAdded(sticker: Sticker)
        fun onStickerClicked(sticker: Sticker)
        fun onStickerDeleted(sticker: Sticker)
        fun onStickerDragFinished(sticker: Sticker)
        fun onStickerTouchedDown(sticker: Sticker, isUpdate: Boolean, isDuplicate: Boolean)
        fun onStickerZoomFinished(sticker: Sticker)
        fun onStickerFlipped(sticker: Sticker)
        fun onStickerDoubleTapped(sticker: Sticker)
        fun onStickerUpdated(sticker: Sticker)
    }
}