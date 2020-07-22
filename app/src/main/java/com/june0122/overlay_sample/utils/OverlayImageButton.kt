package com.june0122.overlay_sample.utils

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageButton
import java.util.*


class OverlayImageButton: AppCompatImageButton {

    companion object {
        const val MAX_CLICK_DURATION = 200
    }

    private var startClickTime : Long = 0
    private var xCoordinate: Float = 0f
    private var yCoordinate: Float = 0f
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startClickTime = Calendar.getInstance().timeInMillis

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
               val clickDuration = Calendar.getInstance().timeInMillis - startClickTime
                if(clickDuration < MAX_CLICK_DURATION) {
                    performClick()
                }
            }
        }
        return false
    }

    // Because we call this from onTouchEvent, this code will be executed for both
    // normal touch events and for when the system calls this using Accessibility
    override fun performClick(): Boolean {
        super.performClick()
        Log.d("debug", "move")
        doSomething()
        return true
    }

    private fun doSomething() {
//        Toast.makeText(context, "did something", Toast.LENGTH_SHORT).show()
    }
}