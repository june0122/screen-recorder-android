package com.june0122.overlay_sample.service.helper


import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import androidx.core.view.GravityCompat
import com.june0122.overlay_sample.R


class OverlayViewManager constructor(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val params = WindowManager.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        },
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP
    }

    private var themedContext: Context? = null
    private var root: View? = null

    fun create() {
        themedContext = OverlayViewContext(context)
        val inflater = LayoutInflater.from(themedContext)
        root = View.inflate(context,    R.layout.fragment_overlay_button, null)
        windowManager.addView(root, params)
    }

    fun changeConfiguration() {
        if (root == null) return
        windowManager.updateViewLayout(root, params)
    }

    fun destroy() {
        if (root == null) return
        windowManager.removeViewImmediate(root)
        themedContext = null
    }

    /* package */
    internal class OverlayViewContext(base: Context?) :
        ContextThemeWrapper(base, R.style.AppTheme) {
        private var inflater: LayoutInflater? = null
//        override fun attachBaseContext(base: Context?) {
//            super.attachBaseContext(CalligraphyContextWrapper.wrap(base))
//        }

        override fun getSystemService(name: String): Any? {
            if (LAYOUT_INFLATER_SERVICE == name) {
                if (inflater == null) {
                    inflater = LayoutInflater.from(baseContext).cloneInContext(this)
                }
                return inflater
            }
            return super.getSystemService(name)
        }
    }
}