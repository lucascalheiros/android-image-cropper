package com.github.lucascalheiros.imagecropper.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.scale
import com.github.lucascalheiros.imagecropper.data.ImageCropperSnapshot
import com.github.lucascalheiros.imagecropper.utils.DragGestureDetector
import com.github.lucascalheiros.imagecropper.utils.Point
import com.github.lucascalheiros.imagecropper.utils.RotationGestureDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ImageCropperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(
    context,
    attrs,
    defStyle
) {
    companion object {
        private const val TAG = "ImageCropperView"
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener())

    private val mDragDetector = DragGestureDetector(dragListener())

    private val mRotationDetector = RotationGestureDetector(rotationListener())

    private val mPhotoProportion: Float?
        get() {
            val bitmap = photoBitmap ?: return null
            return bitmap.width.toFloat() / bitmap.height.toFloat()
        }

    private var mScaledPhoto: Bitmap? = null

    private val viewProportion: Float?
        get() = if (height > 0 && width > 0) width / height.toFloat() else null

    private var mIsFromSnapshot = false
    var defaultPhotoWidth: Int? = null
    var defaultPhotoHeight: Int? = null

    var photoBitmap: Bitmap? = null
        set(value) {
            if (field != value) {
                field = value
                invalidatePhoto()
            }
        }

    val croppingMatrix = Matrix()

    var allowTouch = true

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
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mScaledPhoto?.let {
            canvas.drawBitmap(it, croppingMatrix, null)
        }
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!allowTouch) return false
        mScaleDetector.onTouchEvent(ev)
        mDragDetector.onTouchEvent(ev)
        mRotationDetector.onTouchEvent(ev)
        invalidate()
        Log.d(TAG, croppingMatrix.toString())
        return true
    }

    suspend fun cropToBitmap(x: Int, y: Int, cropWidth: Int, cropHeight: Int): Bitmap = withContext(Dispatchers.Default) {
        Log.d(TAG, "Starting to crop bitmap")
        val scaledBitmap = mScaledPhoto!!
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).let { bitmap ->
            val targetBmp: Bitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, false)
            val canvas = Canvas(bitmap)
            canvas.drawBitmap(targetBmp, croppingMatrix, null)
            Log.d(TAG, "Crop successful")
            Bitmap.createBitmap(
                bitmap,
                x,
                y,
                cropWidth,
                cropHeight
            )
        }
    }

    fun resetCropDefaults() {
        defaultPhotoHeight = null
        defaultPhotoWidth = null
        croppingMatrix.set(Matrix())
        mIsFromSnapshot = false
        invalidatePhoto()
    }

    fun applyCropSnapshot(snapshot: ImageCropperSnapshot) {
        defaultPhotoHeight = snapshot.photoHeight
        defaultPhotoWidth = snapshot.photoWidth
        croppingMatrix.set(snapshot.matrix)
        mIsFromSnapshot = true
        invalidatePhoto()
    }

    fun takeCropSnapshot(): ImageCropperSnapshot {
        val scaledPhoto = mScaledPhoto!!
        return ImageCropperSnapshot(
            scaledPhoto.width,
            scaledPhoto.height,
            Matrix(croppingMatrix)
        )
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

