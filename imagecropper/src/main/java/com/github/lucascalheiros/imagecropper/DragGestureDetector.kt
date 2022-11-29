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

    fun onTouchEvent(ev: MotionEvent) {

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                ev.actionIndex.also { pointerIndex ->
                    mLastTouchX = ev.getX(pointerIndex)
                    mLastTouchY = ev.getY(pointerIndex)
                }

                mActivePointerId = ev.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> {
                if (ev.pointerCount > 1) {
                    mActivePointerId = MotionEvent.INVALID_POINTER_ID
                    return
                }
                val (x: Float, y: Float) = try {
                    mActivePointerId.let { pointerId ->
                        if (pointerId == MotionEvent.INVALID_POINTER_ID) {
                            return
                        } else {
                            ev.findPointerIndex(pointerId).let { pointerIndex ->
                                ev.getX(pointerIndex) to ev.getY(pointerIndex)
                            }
                        }
                    }
                } catch (e: Exception) {
                    return
                }

                listener.onDragged( x - mLastTouchX,  y - mLastTouchY)

                mLastTouchX = x
                mLastTouchY = y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
    }
}

