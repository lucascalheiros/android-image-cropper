package com.github.lucascalheiros.imagecropper.utils

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

    private var mLastTouch = (0f to 0f).toPoint()

    private fun registerActivePointerInitialPosition(ev: MotionEvent) {
        mActivePointerId = ev.getPointerId(0)
        mLastTouch = activePointerPosition(ev) ?: return
    }

    private fun invalidateActivePointer() {
        mActivePointerId = MotionEvent.INVALID_POINTER_ID
    }

    private fun activePointerPosition(ev: MotionEvent): Point? {
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
                }.toPoint()
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

                val currentTouchPosition = activePointerPosition(ev) ?: return

                (currentTouchPosition - mLastTouch).let { moveWalk ->
                    listener.onDragged(moveWalk.x, moveWalk.y)
                }

                mLastTouch = currentTouchPosition
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                invalidateActivePointer()
            }
        }
    }
}

