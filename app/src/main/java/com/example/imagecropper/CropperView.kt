package com.example.imagecropper

import android.content.Context
import android.graphics.*
import android.graphics.ImageDecoder.createSource
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.scale
import androidx.core.view.MotionEventCompat
import kotlin.math.max
import kotlin.math.min


class CropperView : View {

    private var _photoUri: String? = null
    var photoUri: String?
        get() = _photoUri
        set(value) {
            _photoUri = value
            invalidatePhotoAndMeasurements()
        }

    private var _photoBitmap: Bitmap? = null

    // The ‘active pointer’ is the one currently moving our object.
    private var mActivePointerId = INVALID_POINTER_ID

    private var _viewHeight = 0
    private var _viewWidth = 0
    private var _viewDesiredHeight = 0
    private var _photoProportion = 0f

    private var mScaleFactor = 1f

    // Set up the paint with which to draw.
    private val paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.black, null)
        // Smooths out edges of what is drawn without affecting shape.
        isAntiAlias = true
        // Dithering affects how colors with higher-precision than the device are down-sampled.
        isDither = true
        style = Paint.Style.STROKE // default: FILL
        strokeJoin = Paint.Join.ROUND // default: MITER
        strokeCap = Paint.Cap.ROUND // default: BUTT
        strokeWidth = 12f // default: Hairline-width (really thin)
    }

    private val rect = Rect(
        0,
        0,
        200,
        200
    )

    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            mScaleFactor *= detector.scaleFactor

            // Don't let the object get too small or too large.
            mScaleFactor = max(0.1f, min(mScaleFactor, 5.0f))

            Log.d(TAG, "Scale change: $mScaleFactor")

            invalidate()
            return true
        }
    }

    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    private var mPosX = 0f
    private var mLastTouchX = 0f
    private var mPosY = 0f
    private var mLastTouchY = 0f

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        try {
            val uri = Uri.parse(photoUri)
            val source = createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)
            _photoProportion = bitmap.height / bitmap.width.toFloat()
            _viewDesiredHeight = (bitmap.height * (_viewWidth / bitmap.width.toFloat())).toInt()
            _photoBitmap = bitmap.scale(_viewWidth, _viewDesiredHeight, true)
        } catch (t: Throwable) {

        }
    }

    override fun onSizeChanged(xNew: Int, yNew: Int, xOld: Int, yOld: Int) {
        super.onSizeChanged(xNew, yNew, xOld, yOld)
        invalidatePhotoAndMeasurements()
    }

    private fun invalidatePhotoAndMeasurements() {
        try {
            val uri = Uri.parse(photoUri)
            val source = createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source)
            _viewDesiredHeight = (bitmap.height * (width / bitmap.width.toFloat())).toInt()
            _photoBitmap = bitmap.scale(width, _viewDesiredHeight, true)
            invalidate()
        } catch (t: Throwable) {

        }
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = desiredSize
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        if (result < desiredSize) {
            Log.e("ChartView", "The view is too small, the content might get cut")
        }
        return result
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.v("Chart onMeasure w", MeasureSpec.toString(widthMeasureSpec))
        Log.v("Chart onMeasure h", MeasureSpec.toString(heightMeasureSpec))
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = widthSize * _photoProportion + paddingTop + paddingBottom
        setMeasuredDimension(
            measureDimension(desiredWidth, widthMeasureSpec),
            measureDimension(desiredHeight.toInt(), heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        rect.offsetTo(
            mPosX.toInt(),
            mPosY.toInt()
        )

        _photoBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        canvas.drawRect(rect, paint)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev)

        val action = MotionEventCompat.getActionMasked(ev)

        Log.d(TAG, action.toString())

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                MotionEventCompat.getActionIndex(ev).also { pointerIndex ->
                    // Remember where we started (for dragging)
                    mLastTouchX = MotionEventCompat.getX(ev, pointerIndex)
                    mLastTouchY = MotionEventCompat.getY(ev, pointerIndex)
                }

                // Save the ID of this pointer (for dragging)
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
            }

            MotionEvent.ACTION_MOVE -> {
                // Find the index of the active pointer and fetch its position
                val (x: Float, y: Float) =
                    MotionEventCompat.findPointerIndex(ev, mActivePointerId).let { pointerIndex ->
                        // Calculate the distance moved
                        MotionEventCompat.getX(ev, pointerIndex) to
                                MotionEventCompat.getY(ev, pointerIndex)
                    }

                mPosX += x - mLastTouchX
                mPosY += y - mLastTouchY

                invalidate()

                // Remember this touch position for the next move event
                mLastTouchX = x
                mLastTouchY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
            }
            MotionEvent.ACTION_POINTER_UP -> {

                MotionEventCompat.getActionIndex(ev).also { pointerIndex ->
                    MotionEventCompat.getPointerId(ev, pointerIndex)
                        .takeIf { it == mActivePointerId }
                        ?.run {
                            // This was our active pointer going up. Choose a new
                            // active pointer and adjust accordingly.
                            val newPointerIndex = if (pointerIndex == 0) 1 else 0
                            mLastTouchX = MotionEventCompat.getX(ev, newPointerIndex)
                            mLastTouchY = MotionEventCompat.getY(ev, newPointerIndex)
                            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex)
                        }
                }
            }
        }
        invalidate()
        return true
    }

    companion object {
        private const val TAG = "[CROPPER]"
    }
}