package com.github.lucascalheiros.imagecropper

import android.view.MotionEvent

class DragGestureDetector(private val listener: OnDragGestureListener) {
    companion object {
        private const val TAG = "DragGestureDetector"
    }

    interface OnDragGestureListener {
        fun onDragged(walkX: Float, walkY: Float)
    }

    // The ‘active pointer’ is the one currently moving our object.
    private var mActivePointerId = MotionEvent.INVALID_POINTER_ID

    private var mLastTouchX = 0f
    private var mLastTouchY = 0f

    private fun registerActivePointerInitialPosition(ev: MotionEvent) {
        mActivePointerId = ev.getPointerId(0)
        val (x: Float, y: Float) = activePointerPosition(ev) ?: return
        mLastTouchX = x
        mLastTouchY = y
    }

    private fun invalidateActivePointer() {
        mActivePointerId = MotionEvent.INVALID_POINTER_ID
    }

    private fun activePointerPosition(ev: MotionEvent): Pair<Float, Float>? {
        if (mActivePointerId == MotionEvent.INVALID_POINTER_ID) {
            registerActivePointerInitialPosition(ev)
        }
        return try {
            val activePointerId = mActivePointerId
            if (activePointerId == MotionEvent.INVALID_POINTER_ID) {
                null
            } else {
                ev.findPointerIndex(activePointerId).let { pointerIndex ->
                    ev.getX(pointerIndex) to ev.getY(pointerIndex)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun onTouchEvent(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                registerActivePointerInitialPosition(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                if (ev.pointerCount > 1) {
                    invalidateActivePointer()
                    return
                }

                val (x: Float, y: Float) = activePointerPosition(ev) ?: return

                listener.onDragged(x - mLastTouchX, y - mLastTouchY)

                mLastTouchX = x
                mLastTouchY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                invalidateActivePointer()
            }
        }
    }
}

