package com.june0122.overlay_sample.ui.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.ui.activity.MainActivity


class ForegroundService : Service() {
    private val serviceId = "Foreground Service Example"

//    protected val context: Context
//        get() = this

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

    }

    override fun onDestroy() {
        super.onDestroy()
    }
}