package com.github.lucascalheiros.imagecropper

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.scale
import com.github.lucascalheiros.imagecropper.utils.*
import com.github.lucascalheiros.imagecropper.utils.Point
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.min


class CropperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(
    context,
    attrs,
    defStyle
) {
    companion object {
        private const val TAG = "CropperView"
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener())

    private val mDragDetector = DragGestureDetector(dragListener())

    private val mRotationDetector = RotationGestureDetector(rotationListener())

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

    private val mPhotoProportion: Float?
        get() {
            val bitmap = photoBitmap ?: return null
            return bitmap.width.toFloat() / bitmap.height.toFloat()
        }

    private var mScaledPhoto: Bitmap? = null

    private val cropX: Int
        get() = mCropAreaRectangle.left

    private val cropY: Int
        get() = mCropAreaRectangle.top

    private val cropWidth: Int
        get() = mCropAreaRectangle.width()

    private val cropHeight: Int
        get() = mCropAreaRectangle.height()

    private val initialYPositionShift: Float
        get() = (height - (mScaledPhoto?.height ?: 0)) / 2f

    private val initialXPositionShift: Float
        get() = (width - (mScaledPhoto?.width ?: 0)) / 2f

    private val viewProportion: Float?
        get() = if (height > 0 && width > 0) width / height.toFloat() else null

    var cropProportion = 1f
        set(value) {
            if (field != value) {
                field = value
                initializeCropAreaPosition()
                invalidate()
            }
        }

    var maxCropWidth: Float = Float.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                initializeCropAreaPosition()
                invalidate()
            }
        }

    var maxCropHeight: Float = Float.MAX_VALUE
        set(value) {
            if (field != value) {
                field = value
                initializeCropAreaPosition()
                invalidate()
            }
        }

    var horizontalBorder: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                initializeCropAreaPosition()
                invalidate()
            }
        }

    var verticalBorder: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                initializeCropAreaPosition()
                invalidate()
            }
        }

    var photoBitmap: Bitmap? = null
        set(value) {
            if (field != value) {
                field = value
                invalidatePhotoAndMeasurements()
            }
        }

    val croppingMatrix = Matrix()

    private fun initializePhotoPosition() {
        croppingMatrix.setTranslate(initialXPositionShift, initialYPositionShift)
    }

    private fun initializeCropAreaPosition() {
        val viewProportion = viewProportion ?: return

        val posX = (width - cropWidth) / 2f
        val posY = (height - cropHeight) / 2f
        updateCropAreaPosition(posX, posY)

        val boundSize = if (viewProportion <= cropProportion) {
            (width - horizontalBorder).let { it to (it / cropProportion) }
        } else {
            (height - verticalBorder).let { (it * cropProportion) to it }
        }.toPoint()
        val resizeFactor = min(boundSize.x, maxCropWidth) / boundSize.x * min(boundSize.y, maxCropHeight) / boundSize.y

        val resizedBoundSize = boundSize * resizeFactor
        resizeCropArea(resizedBoundSize.x.toInt(), resizedBoundSize.y.toInt())
    }

    private fun invalidatePhotoAndMeasurements() {
        try {
            val bitmap = photoBitmap ?: return
            val photoProportion = mPhotoProportion ?: return
            val viewProportion = viewProportion ?: return

            val (scaledWidth, scaledHeight) = if (viewProportion <= photoProportion)
                width to (width / photoProportion).toInt()
            else
                (height * photoProportion).toInt() to height
            mScaledPhoto = bitmap.scale(scaledWidth, scaledHeight)
            initializePhotoPosition()
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "onSizeChanged")
        initializePhotoPosition()
        initializeCropAreaPosition()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mScaledPhoto?.let {
            canvas.drawBitmap(it, croppingMatrix, null)
        }

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
        mScaleDetector.onTouchEvent(ev)
        mDragDetector.onTouchEvent(ev)
        mRotationDetector.onTouchEvent(ev)
        invalidate()
        Log.d(TAG, croppingMatrix.toString())
        return true
    }

    suspend fun cropToBitmap(): Bitmap = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting to crop bitmap")
        val scaledBitmap = mScaledPhoto!!
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).let { bitmap ->
            val targetBmp: Bitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(targetBmp, croppingMatrix, null)
            Log.d(TAG, "Crop successful")
            Bitmap.createBitmap(
                bitmap,
                cropX,
                cropY,
                cropWidth,
                cropHeight
            )
        }
    }

    private fun updateCropAreaPosition(x: Float, y: Float) {
        mCropAreaRectangle.offsetTo(
            x.toInt(),
            y.toInt()
        )
    }

    // Resize keeping same center
    private fun resizeCropArea(newWidth: Int, newHeight: Int) {
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
            croppingMatrix.postScale(
                detector.scaleFactor,
                detector.scaleFactor,
                detector.focusX,
                detector.focusY
            )
            return true
        }
    }

    private fun dragListener() = object : DragGestureDetector.OnDragGestureListener {
        override fun onDragged(walkX: Float, walkY: Float) {
            croppingMatrix.postTranslate(walkX, walkY)
        }
    }

    private fun rotationListener() = object : RotationGestureDetector.OnRotationGestureListener {
        override fun onRotation(angle: Float, pivotPoint: Point) {
            croppingMatrix.postRotate(angle, pivotPoint.x, pivotPoint.y)
        }
    }

}
