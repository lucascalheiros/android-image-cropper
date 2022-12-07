package com.github.lucascalheiros.imagecropper.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.github.lucascalheiros.imagecropper.R
import com.github.lucascalheiros.imagecropper.data.CropAreaSnapshot
import com.github.lucascalheiros.imagecropper.utils.DragGestureDetector
import com.github.lucascalheiros.imagecropper.utils.Point
import com.github.lucascalheiros.imagecropper.utils.toPoint
import kotlin.math.absoluteValue


class CropAreaView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(
    context,
    attrs,
    defStyle
) {
    companion object {
        private const val TAG = "CropAreaView"
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener())

    private val mDragDetector = DragGestureDetector(dragListener())

    private val cropAreaBackground: Paint = Paint().apply {
        color = resources.getColor(R.color.black_semitransparent, context.theme)
        style = Paint.Style.FILL
    }
    private val cropAreaPath = Path()

    private val mCropAreaRectangle = Rect(
        0,
        0,
        0,
        0
    )
    private val cropX: Int
        get() = mCropAreaRectangle.left

    private val cropY: Int
        get() = mCropAreaRectangle.top

    private val cropWidth: Int
        get() = mCropAreaRectangle.width()

    private val cropHeight: Int
        get() = mCropAreaRectangle.height()

    private val viewProportion: Float?
        get() = if (height > 0 && width > 0) width / height.toFloat() else null

    private var mCropProportion = 1f
    var cropProportion: Float
        get() = mCropProportion
        set(value) {
            mCropProportion = value
            initializeCropAreaPosition()
            invalidate()
        }

    private val maxWidth: Int?
        get() {
            val viewProportion = viewProportion ?: return null
            return if (viewProportion <= cropProportion) {
                width
            } else {
                (height * cropProportion).toInt()
            }
        }

    private val maxHeight: Int?
        get() {
            val viewProportion = viewProportion ?: return null
            return if (viewProportion <= cropProportion) {
                (width / cropProportion).toInt()
            } else {
                height
            }
        }

    var minScale: Float = 0.5f

    private var mHorizontalCropBorder = 0f
    var horizontalCropBorder: Float
        get() = mHorizontalCropBorder
        set(value) {
            mHorizontalCropBorder = value
            initializeCropAreaPosition()
            invalidate()
        }

    private var mVerticalCropBorder = 0f
    var verticalCropBorder: Float
        get() = mVerticalCropBorder
        set(value) {
            mVerticalCropBorder = value
            initializeCropAreaPosition()
            invalidate()
        }

    private var defaultPos: Point? = null
    private var defaultSize: Point? = null

    var allowTouch = true

    private fun initializeCropAreaPosition() {
        val (cropWidth, cropHeight) = defaultCropSize() ?: return
        resizeCropArea(cropWidth, cropHeight)
        val (posX, posY) = defaultCropPosition()
        updateCropAreaPosition(posX, posY)
    }

    private fun defaultCropSize(): Pair<Int, Int>? {
        val viewProportion = viewProportion ?: return null
        defaultSize?.let {
            return it.x.toInt() to it.y.toInt()
        }

        val widthLimit = width - horizontalCropBorder
        val heightLimit = height - verticalCropBorder
        val (newWidth, newHeight) = if (viewProportion <= cropProportion) {
            widthLimit to (widthLimit / cropProportion)
        } else {
            (heightLimit * cropProportion) to heightLimit
        }
        val resizeFactor = if (newWidth > widthLimit) {
            newWidth / widthLimit
        } else if (newHeight > heightLimit) {
            newHeight / heightLimit
        } else 1f

        return (newWidth * resizeFactor).toInt() to (newHeight * resizeFactor).toInt()
    }

    private fun defaultCropPosition(): Pair<Float, Float> {
        return defaultPos?.let {
            it.x to it.y
        } ?: ((width - cropWidth) / 2f to (height - cropHeight) / 2f)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged")
        initializeCropAreaPosition()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCropArea(canvas)
    }

    private fun drawCropArea(canvas: Canvas) {
        cropAreaPath.reset()

        cropAreaPath.apply {
            moveTo(mCropAreaRectangle.left.toFloat(), mCropAreaRectangle.top.toFloat())
            lineTo(mCropAreaRectangle.left.toFloat(), mCropAreaRectangle.bottom.toFloat())
            lineTo(mCropAreaRectangle.right.toFloat(), mCropAreaRectangle.bottom.toFloat())
            lineTo(mCropAreaRectangle.right.toFloat(), mCropAreaRectangle.top.toFloat())
            fillType = Path.FillType.INVERSE_EVEN_ODD
        }

        canvas.drawPath(cropAreaPath, cropAreaBackground)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!allowTouch) return false
        mScaleDetector.onTouchEvent(ev)
        mDragDetector.onTouchEvent(ev)
        invalidate()
        return true
    }

    fun resetDefaults() {
        mVerticalCropBorder = 0f
        mHorizontalCropBorder = 0f
        mCropProportion = 1f
        defaultPos = null
        defaultSize = null
        initializeCropAreaPosition()
        invalidate()
    }

    fun applyCropSnapshot(snapshot: CropAreaSnapshot) {
        mCropProportion = snapshot.cropWidth.toFloat() / snapshot.cropHeight.toFloat()
        defaultPos = (snapshot.cropX.toFloat() to snapshot.cropY.toFloat()).toPoint()
        defaultSize = (snapshot.cropWidth.toFloat() to snapshot.cropHeight.toFloat()).toPoint()
        initializeCropAreaPosition()
        invalidate()
    }

    fun takeCropSnapshot(): CropAreaSnapshot {
        return CropAreaSnapshot(
            cropX,
            cropY,
            cropWidth,
            cropHeight
        )
    }

    private fun updateCropAreaPosition(x: Float, y: Float) {
        mCropAreaRectangle.offsetTo(
            x.coerceIn(0f, width.toFloat() - cropWidth).toInt(),
            y.coerceIn(0f, height.toFloat() - cropHeight).toInt()
        )
    }

    // Resize keeping same center
    private fun resizeCropArea(newW: Int, newH: Int) {
        val maxWidth = maxWidth ?: return
        val maxHeight = maxHeight ?: return
        val newWidth = newW.coerceIn((maxWidth * minScale).toInt(), maxWidth)
        val newHeight = newH.coerceIn((maxHeight * minScale).toInt(), maxHeight)
        val x = (mCropAreaRectangle.exactCenterX() - newWidth / 2).absoluteValue.coerceIn(
            0f,
            (width.toFloat() - newWidth).absoluteValue
        )
        val y = (mCropAreaRectangle.exactCenterY() - newHeight / 2).absoluteValue.coerceIn(
            0f,
            (height.toFloat() - newHeight).absoluteValue
        )

        mCropAreaRectangle.left = 0
        mCropAreaRectangle.top = 0
        mCropAreaRectangle.right = 0 + newWidth
        mCropAreaRectangle.bottom = 0 + newHeight
        updateCropAreaPosition(x, y)
    }

    private fun scaleListener() = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            resizeCropArea(
                (cropWidth * detector.scaleFactor).toInt(),
                (cropHeight * detector.scaleFactor).toInt()
            )
            return true
        }
    }

    private fun dragListener() = object : DragGestureDetector.OnDragGestureListener {
        override fun onDragged(walkX: Float, walkY: Float) {
            val x = (cropX + walkX).coerceIn(0f, width.toFloat() - cropWidth)
            val y = (cropY + walkY).coerceIn(0f, height.toFloat() - cropHeight)
            updateCropAreaPosition(x, y)
        }
    }

}

