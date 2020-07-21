package com.june0122.overlay_sample.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageButton


class OverlayImageButton: AppCompatImageButton {
    private var xCoordinate: Float = 0f
    private var yCoordinate: Float = 0f
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                xCoordinate = rootView.x - event.rawX
                yCoordinate = rootView.y - event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                rootView.animate()
                    .x(event.rawX + xCoordinate)
                    .y(event.rawY + yCoordinate)
                    .setDuration(0)
                    .start()
            }
            MotionEvent.ACTION_UP -> {
                performClick()
                return true
            }
        }
        return false
    }

    // Because we call this from onTouchEvent, this code will be executed for both
    // normal touch events and for when the system calls this using Accessibility
    override fun performClick(): Boolean {
        super.performClick()
        doSomething()
        return true
    }

    private fun doSomething() {
//        Toast.makeText(context, "did something", Toast.LENGTH_SHORT).show()
    }
}