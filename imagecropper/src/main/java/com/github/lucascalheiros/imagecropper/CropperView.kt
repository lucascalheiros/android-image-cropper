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

    private val viewProportion: Float?
        get() = if (height > 0 && width > 0) width / height.toFloat() else null

    private var mIsFromSnapshot = false
    var defaultPhotoWidth: Int? = null
    var defaultPhotoHeight: Int? = null

    private var mCropProportion = 1f
    var cropProportion: Float
        get() = mCropProportion
        set(value) {
            mCropProportion = value
            initializeCropAreaPosition()
            invalidate()
        }

    private var mMaxCropWidth = Float.MAX_VALUE
    var maxCropWidth: Float
        get() = mMaxCropWidth
        set(value) {
            mMaxCropWidth = value
            initializeCropAreaPosition()
            invalidate()
        }

    private var mMaxCropHeight = Float.MAX_VALUE
    var maxCropHeight: Float
        get() = mMaxCropHeight
        set(value) {
            mMaxCropHeight = value
            initializeCropAreaPosition()
            invalidate()
        }

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

    var photoBitmap: Bitmap? = null
        set(value) {
            if (field != value) {
                field = value
                invalidatePhoto()
            }
        }

    val croppingMatrix = Matrix()


    private fun initializeCropAreaPosition() {
        val (cropWidth, cropHeight) = defaultCropSize() ?: return
        resizeCropArea(cropWidth, cropHeight)
        val (posX, posY) = defaultCropPosition()
        updateCropAreaPosition(posX, posY)
    }

    private fun defaultCropSize(): Pair<Int, Int>? {
        val viewProportion = viewProportion ?: return null

        val widthLimited = min(width - horizontalCropBorder, maxCropWidth)
        val heightLimited = min(height - verticalCropBorder, maxCropHeight)
        val boundSize = if (viewProportion <= cropProportion) {
            widthLimited to (widthLimited / cropProportion)
        } else {
            (heightLimited * cropProportion) to heightLimited
        }.toPoint()
        val resizeFactor = if (boundSize.x - widthLimited < 0) {
            boundSize.x / widthLimited
        } else if (boundSize.y - heightLimited < 0) {
            boundSize.y / heightLimited
        } else 1f

        val resizedBoundSize = boundSize * resizeFactor

        return resizedBoundSize.x.toInt() to resizedBoundSize.y.toInt()
    }

    private fun defaultCropPosition(): Pair<Float, Float> {
        return (width - cropWidth) / 2f to (height - cropHeight) / 2f
    }

    private fun initializePhotoPosition() {
        if (mIsFromSnapshot) return
        val (initialX, initialY) = defaultPhotoPosition() ?: return
        croppingMatrix.setTranslate(initialX, initialY)
    }

    private fun defaultPhotoSize(): Pair<Int, Int>? {
        val photoProportion = mPhotoProportion ?: return null
        val viewProportion = viewProportion ?: return null
        val defaultPhotoHeight = defaultPhotoHeight
        val defaultPhotoWidth = defaultPhotoWidth
        return if (defaultPhotoHeight != null && defaultPhotoWidth != null)
            defaultPhotoWidth to defaultPhotoHeight
        else if (viewProportion <= photoProportion)
            width to (width / photoProportion).toInt()
        else
            (height * photoProportion).toInt() to height
    }

    private fun defaultPhotoPosition(): Pair<Float, Float>? {
        val scaledPhoto = mScaledPhoto ?: return null
        return (width - scaledPhoto.width) / 2f to (height - scaledPhoto.height) / 2f
    }

    private fun invalidatePhoto() {
        try {
            val (scaledWidth, scaledHeight) = defaultPhotoSize() ?: return
            mScaledPhoto = photoBitmap?.scale(scaledWidth, scaledHeight) ?: return
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

    fun resetCropDefaults() {
        mVerticalCropBorder = 0f
        mHorizontalCropBorder = 0f
        mMaxCropHeight = Float.MAX_VALUE
        mMaxCropWidth = Float.MAX_VALUE
        mCropProportion = 1f
        defaultPhotoHeight = null
        defaultPhotoWidth = null
        croppingMatrix.set(Matrix())
        mIsFromSnapshot = false
        initializeCropAreaPosition()
        invalidatePhoto()
    }

    fun applyCropSnapshot(snapshot: CropSnapshot) {
        mMaxCropHeight = snapshot.cropHeight.toFloat()
        mMaxCropWidth = snapshot.cropWidth.toFloat()
        mCropProportion = snapshot.cropWidth.toFloat() / snapshot.cropHeight.toFloat()
        defaultPhotoHeight = snapshot.photoHeight
        defaultPhotoWidth = snapshot.photoWidth
        croppingMatrix.set(snapshot.matrix)
        mIsFromSnapshot = true
        initializeCropAreaPosition()
        invalidatePhoto()
    }

    fun takeCropSnapshot(): CropSnapshot {
        val scaledPhoto = mScaledPhoto!!
        return CropSnapshot(
            cropWidth,
            cropHeight,
            scaledPhoto.width,
            scaledPhoto.height,
            Matrix(croppingMatrix)
        )
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


data class CropSnapshot(
    val cropWidth: Int,
    val cropHeight: Int,
    val photoWidth: Int,
    val photoHeight: Int,
    val matrix: Matrix
)