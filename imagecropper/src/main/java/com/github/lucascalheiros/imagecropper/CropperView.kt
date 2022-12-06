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
import kotlin.math.*


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

    private var mPhotoBitmap: Bitmap? = null
    var photoBitmap: Bitmap?
        get() = mPhotoBitmap
        set(value) {
            mPhotoBitmap = value
            invalidatePhotoAndMeasurements()
        }

    private var mPhotoProportion = 0f

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

    val cropX: Int
        get() = mCropAreaRectangle.centerX() - cropSize / 2

    val cropY: Int
        get() = mCropAreaRectangle.centerY() - cropSize / 2

    val cropSize: Int
        get() = mCropAreaRectangle.width()

    private val photoMatrix = Matrix()

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener())
    private val mDragDetector = DragGestureDetector(dragListener())
    private val mRotationDetector = RotationGestureDetector(rotationListener())

    private fun invalidatePhotoAndMeasurements() {
        try {
            val bitmap = mPhotoBitmap ?: return
            mPhotoProportion = bitmap.height / bitmap.width.toFloat()
            if (width == 0)
                return
            val height = (width * mPhotoProportion).toInt()
            mPhotoBitmap = bitmap.scale(width, height, true).also {
                val posX = it.width / 2f
                val posY = it.height / 2f
                updateCropAreaPosition(posX, posY)
                val size = min(it.width, it.height)
                resizeCropArea(size)
            }

            requestLayout()
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        return when (specMode) {
            MeasureSpec.EXACTLY -> {
                specSize
            }
            MeasureSpec.AT_MOST -> {
                min(desiredSize, specSize)
            }
            else -> {
                desiredSize
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d(TAG, "onMeasure")
        Log.v(TAG, MeasureSpec.toString(widthMeasureSpec))
        Log.v(TAG, MeasureSpec.toString(heightMeasureSpec))
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = widthSize * mPhotoProportion + paddingTop + paddingBottom
        setMeasuredDimension(
            measureDimension(desiredWidth, widthMeasureSpec),
            measureDimension(desiredHeight.toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mPhotoBitmap?.let {
            canvas.drawBitmap(it, photoMatrix, null)
            canvas.drawRect(mCropAreaRectangle, paint)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        mScaleDetector.onTouchEvent(ev)
        mDragDetector.onTouchEvent(ev)
        mRotationDetector.onTouchEvent(ev)
        invalidate()
        return true
    }


    suspend fun cropToBitmap(): Bitmap = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting to crop bitmap")
        Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888).also { bitmap ->
            val targetBmp: Bitmap = mPhotoBitmap!!.copy(Bitmap.Config.ARGB_8888, false)

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
//            val size = (cropSize * detector.scaleFactor).coerceIn(
//                min(width.toFloat(), height.toFloat()) * 0.5f,
//                min(width.toFloat(), height.toFloat())
//            ).toInt()
//
//            resizeCropArea(size)

            photoMatrix.postScale(
                detector.scaleFactor,
                detector.scaleFactor,
                detector.focusX,
                detector.focusY
            )
            Log.d(TAG, photoMatrix.toString())

            return true
        }
    }

    private fun dragListener() = object : DragGestureDetector.OnDragGestureListener {
        override fun onDragged(walkX: Float, walkY: Float) {
//            val x = (cropX + walkX).coerceIn(0f, width.toFloat() - cropSize)
//            val y = (cropY + walkY).coerceIn(0f, height.toFloat() - cropSize)
//            updateCropAreaPosition(x, y)
            photoMatrix.postTranslate(walkX, walkY)
            Log.d(TAG, photoMatrix.toString())
        }
    }

    private fun rotationListener() = object : RotationGestureDetector.OnRotationGestureListener {
        override fun onRotation(angle: Float, pivotPoint: Point) {
//            Log.d(TAG, "angle: $angle, pivotPoint: $pivotPoint")
            photoMatrix.postRotate(angle, pivotPoint.x, pivotPoint.y)
            Log.d(TAG, photoMatrix.toString())
        }
    }

}
