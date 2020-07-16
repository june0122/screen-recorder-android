package com.june0122.overlay_sample.ui.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.ui.activity.MainActivity


class ForegroundService : Service() {
    private var xCoordinate: Float = 0f
    private var yCoordinate: Float = 0f
    private val serviceId = "Foreground Service Example"
    private var mView: View? = null
    private var wm: WindowManager? = null

    companion object {
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, ForegroundService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //do heavy work on a background thread
        val input = intent?.getStringExtra("inputExtra")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
        val ezrealImage: Drawable = resources.getDrawable(R.drawable.wildrift_logo, null)
        val largeIconBitmap: Bitmap = (ezrealImage as BitmapDrawable).bitmap

        val notification = NotificationCompat.Builder(this, serviceId)
                .setContentTitle("AHAM INTERNSHIP EXAMPLE")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_lol_logo)
                .setLargeIcon(largeIconBitmap)
                .setContentIntent(pendingIntent)
                .build()

        createNotificationChannel()
        startForeground(1, notification)
        //stopSelf();

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(serviceId, "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        val inflate: LayoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= 26) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    // TYPE_SYSTEM_OVERLAYはロック画面にもViewを表示できますが、タッチイベントを取得できません
                },
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP
        mView = inflate.inflate(R.layout.fragment_overlay_button, null)


        val screenshotButton: ImageView? = mView?.rootView?.findViewById(R.id.screenshotButton)

        screenshotButton?.setOnClickListener {
            Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
        }

//        screenshotButton?.setOnTouchListener(object : View.OnTouchListener {
//            override fun onTouch(v: View, event: MotionEvent?): Boolean {
//                when (event?.actionMasked) {
//                    MotionEvent.ACTION_DOWN -> {
//                        xCoordinate = v.x - event.rawX
//                        yCoordinate = v.y - event.rawY
//                        Log.d("XXX", "Down")
//                        Log.d("XXX", "${params.x} | ${params.y}")
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        v.animate()
//                            .x(event.rawX + xCoordinate)
//                            .y(event.rawY + yCoordinate)
//                            .setDuration(0)
//                            .start()
//                    }
//                    MotionEvent.ACTION_UP -> {
//                    }
//                    else -> return false
//                }
//                return true
//            }
//        })

//        params.x = ViewGroup.LayoutParams.WRAP_CONTENT
//        params.y = ViewGroup.LayoutParams.WRAP_CONTENT
//        wm?.updateViewLayout(mView, params)

        wm?.addView(mView, params)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (wm != null) {
            if (mView != null) {
                wm?.removeView(mView)
                mView = null
            }
        }
    }
}