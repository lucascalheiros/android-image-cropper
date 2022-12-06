package com.github.lucascalheiros.imagecropper

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.scale
import com.github.lucascalheiros.imagecropper.utils.DragGestureDetector
import com.github.lucascalheiros.imagecropper.utils.Point
import com.github.lucascalheiros.imagecropper.utils.RotationGestureDetector
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

    private val photoMatrix = Matrix()

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener())

    private val mDragDetector = DragGestureDetector(dragListener())

    private val mRotationDetector = RotationGestureDetector(rotationListener())

    private val paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.black, null)
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
    }

    private val mCropAreaRectangle = Rect(
        0,
        0,
        0,
        0
    )

    private val mPhotoProportion: Float?
        get() {
            val bitmap = photoBitmap ?: return null
            return bitmap.height.toFloat() / bitmap.width.toFloat()
        }

    private var mScaledPhoto: Bitmap? = null

    var photoBitmap: Bitmap? = null
        set(value) {
            if (field != value) {
                field = value
                invalidatePhotoAndMeasurements()
            }
        }

    val cropX: Int
        get() = mCropAreaRectangle.centerX() - cropSize / 2

    val cropY: Int
        get() = mCropAreaRectangle.centerY() - cropSize / 2

    val cropSize: Int
        get() = mCropAreaRectangle.width()


    private fun initializePhotoPosition() {
        val posY = (height - (mScaledPhoto?.height ?: 0)) / 2f
        photoMatrix.setTranslate(0f, posY)
    }

    private fun initializeCropAreaPosition() {
        val posX = width / 2f
        val posY = height / 2f
        updateCropAreaPosition(posX, posY)
        val size = min(width, height)
        resizeCropArea(size)
    }

    private fun invalidatePhotoAndMeasurements() {
        try {
            val bitmap = photoBitmap ?: return
            val photoProportion = mPhotoProportion ?: return
            val viewProportion = if (width > 0) height / width else return

            val (scaledWidth, scaledHeight) = if (viewProportion <= photoProportion)
                width to (width * photoProportion).toInt()
            else
                (height / photoProportion).toInt() to height
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
            canvas.drawBitmap(it, photoMatrix, null)
            canvas.drawRect(mCropAreaRectangle, paint)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(ev)
        mDragDetector.onTouchEvent(ev)
        mRotationDetector.onTouchEvent(ev)
        invalidate()
        Log.d(TAG, photoMatrix.toString())
        return true
    }


    suspend fun cropToBitmap(): Bitmap = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting to crop bitmap")
        Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888).also { bitmap ->
            val targetBmp: Bitmap = mScaledPhoto!!.copy(Bitmap.Config.ARGB_8888, false)

            val canvas = Canvas(bitmap)

            photoMatrix.preTranslate(-cropX.toFloat(), -cropY.toFloat())
            canvas.drawBitmap(targetBmp, photoMatrix, null)

            Log.d(TAG, "Crop successful")
        }
    }

    private fun updateCropAreaPosition(x: Float, y: Float) {
        mCropAreaRectangle.offsetTo(
            x.toInt(),
            y.toInt()
        )
    }

    private fun resizeCropArea(size: Int) {
        val x = (mCropAreaRectangle.exactCenterX() - size / 2).absoluteValue.coerceIn(
            0f,
            (width.toFloat() - size).absoluteValue
        )
        val y = (mCropAreaRectangle.exactCenterY() - size / 2).absoluteValue.coerceIn(
            0f,
            (height.toFloat() - size).absoluteValue
        )

        mCropAreaRectangle.left = 0
        mCropAreaRectangle.top = 0
        mCropAreaRectangle.right = 0 + size
        mCropAreaRectangle.bottom = 0 + size
        updateCropAreaPosition(x, y)
    }

    private fun scaleListener() = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            photoMatrix.postScale(
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
            photoMatrix.postTranslate(walkX, walkY)
        }
    }

    private fun rotationListener() = object : RotationGestureDetector.OnRotationGestureListener {
        override fun onRotation(angle: Float, pivotPoint: Point) {
            photoMatrix.postRotate(angle, pivotPoint.x, pivotPoint.y)
        }
    }

}
