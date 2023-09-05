package com.github.lucascalheiros.imagecropper.utils

import android.view.MotionEvent
import kotlin.math.atan2

class RotationGestureDetector(private val listener: OnRotationGestureListener) {
    interface OnRotationGestureListener {
        fun onRotation(angle: Float, pivotPoint: Point)
    }

    private var mPrimaryPointerId = MotionEvent.INVALID_POINTER_ID
    private var mSecondaryPointerId = MotionEvent.INVALID_POINTER_ID

    private var mLastPrimaryPointerPosition: Point? = null
    private var mLastSecondaryPointerPosition: Point? = null

    private val middlePoint: Point?
        get() {
            val lastPrimaryPointerPosition = mLastPrimaryPointerPosition ?: return  null
            val lastSecondaryPointerPosition = mLastSecondaryPointerPosition ?: return  null
            return (lastPrimaryPointerPosition + lastSecondaryPointerPosition) / 2f
        }

    private val pointersValid: Boolean
        get() = mPrimaryPointerId != MotionEvent.INVALID_POINTER_ID &&
                mSecondaryPointerId != MotionEvent.INVALID_POINTER_ID

    private fun invalidatePrimaryPointer() {
        mPrimaryPointerId = MotionEvent.INVALID_POINTER_ID
    }

    private fun invalidateSecondaryPointer() {
        mSecondaryPointerId = MotionEvent.INVALID_POINTER_ID
    }

    private fun MotionEvent.getPoint(pointerIndex: Int): Point {
        return findPointerIndex(pointerIndex).let {
            getX(it) to getY(it)
        }.toPoint()
    }

    private fun angleBetweenLines(
        lastPrimaryPoint: Point,
        lastSecondaryPoint: Point,
        newPrimaryPoint: Point,
        newSecondaryPoint: Point
    ): Float {
        val angle1 = (lastPrimaryPoint - lastSecondaryPoint).let { atan2(it.y, it.x) }
        val angle2 = (newPrimaryPoint - newSecondaryPoint).let { atan2(it.y, it.x) }
        return Math.toDegrees(angle2.toDouble() - angle1.toDouble()).toFloat()
    }

    fun onTouchEvent(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mPrimaryPointerId = event.getPointerId(0)
                mLastPrimaryPointerPosition = event.getPoint(mPrimaryPointerId)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mSecondaryPointerId = event.getPointerId(event.actionIndex)
                mLastSecondaryPointerPosition = event.getPoint(mSecondaryPointerId)
            }
            MotionEvent.ACTION_MOVE -> {
                if (pointersValid) {
                    val newPrimaryPoint = event.getPoint(mPrimaryPointerId)
                    val newSecondaryPoint = event.getPoint(mSecondaryPointerId)
                    val lastPrimaryPointerPosition = mLastPrimaryPointerPosition
                    val lastSecondaryPointerPosition = mLastSecondaryPointerPosition
                    if (lastPrimaryPointerPosition == null || lastSecondaryPointerPosition == null) {
                        mLastPrimaryPointerPosition = newPrimaryPoint
                        mLastSecondaryPointerPosition = newSecondaryPoint
                    } else {
                        val angle = angleBetweenLines(
                            lastPrimaryPointerPosition,
                            lastSecondaryPointerPosition,
                            newPrimaryPoint,
                            newSecondaryPoint
                        )
                        middlePoint?.let {
                            listener.onRotation(angle, it)
                        }
                    }
                }
                registerLastPositionsAfterMove(event)
            }
            MotionEvent.ACTION_UP -> invalidatePrimaryPointer()
            MotionEvent.ACTION_POINTER_UP -> invalidateSecondaryPointer()
            MotionEvent.ACTION_CANCEL -> {
                invalidatePrimaryPointer()
                invalidateSecondaryPointer()
            }
        }
    }

    private fun registerLastPositionsAfterMove(event: MotionEvent) {
        mLastPrimaryPointerPosition = if (mPrimaryPointerId != MotionEvent.INVALID_POINTER_ID) {
            try { event.getPoint(mPrimaryPointerId) } catch (ignore: Exception) { null }
        } else {
            null
        }
        mLastSecondaryPointerPosition = if (mSecondaryPointerId != MotionEvent.INVALID_POINTER_ID) {
            try { event.getPoint(mSecondaryPointerId) } catch (ignore: Exception) { null }
        } else {
            null
        }
    }
}
