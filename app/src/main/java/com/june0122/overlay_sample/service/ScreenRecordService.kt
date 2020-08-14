package com.june0122.overlay_sample.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.media.app.NotificationCompat.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.receiver.MyBroadcastReceiver
import com.june0122.overlay_sample.ui.activity.MainActivity
import com.june0122.overlay_sample.utils.PI_CODE_DELETE_SCREEN_RECORD_SERVICE
import com.june0122.overlay_sample.utils.PI_CODE_STOP_SCREEN_RECORD_SERVICE
import com.june0122.overlay_sample.utils.SCREEN_RECORD_SERVICE_ID

class ScreenRecordService : Service() {
    private val serviceId = "Foreground Service Example"

    private val context: Context
        get() = this

    companion object {
        fun startService(context: Context, message: String) {
            val startIntent = Intent(context, ScreenRecordService::class.java)
            startIntent.putExtra("inputExtra", message)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, ScreenRecordService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        setScreenRecordNotification(intent)

        return START_STICKY
    }

    private fun setScreenRecordNotification(intent: Intent?) {
        val contentTitle = "SCREEN RECORD"
        val contentText = intent?.getStringExtra("inputExtra")
        val largeIconBitmap = BitmapFactory.decodeResource(resources, R.drawable.portrait_ahri)
        val notificationIntent = Intent( this, MainActivity::class.java)
        val pendingIntent =
                PendingIntent.getActivity(
                        this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )

        val closeIntent = PendingIntent.getBroadcast(
                context,
                PI_CODE_DELETE_SCREEN_RECORD_SERVICE,
                Intent(context, MyBroadcastReceiver::class.java)
                        .apply { action = MyBroadcastReceiver.ACTION_DELETE_SCREEN_RECORD_SERVICE },
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopRecordIntent = PendingIntent.getBroadcast(
                context,
                PI_CODE_STOP_SCREEN_RECORD_SERVICE,
                Intent(context, MyBroadcastReceiver::class.java)
                        .apply { action = MyBroadcastReceiver.ACTION_STOP_SCREEN_RECORD_SERVICE },
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mediaSession =
                MediaSessionCompat(applicationContext, "screenRecordSession").apply {
                    hideMediaStyleNotificationSeekBar(this)
                }

        /** If you want to show SeekBar and need to control your media service, use the code below */
//        mediaSession.setFlags(0)
//        mediaSession.setPlaybackState(PlaybackStateCompat.Builder()
//                .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
//                .build())

        val screenRecordNotification = NotificationCompat.Builder(this, serviceId)
                .setSmallIcon(R.drawable.ic_noti_screen_record)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(closeIntent)
                .addAction(
                        NotificationCompat.Action(
                                R.drawable.ic_noti_stop,
                                getString(R.string.stop),
                                stopRecordIntent
                        )
                )
                .addAction(
                        NotificationCompat.Action(
                                R.drawable.ic_noti_close,
                                getString(R.string.close),
                                closeIntent
                        )
                )
                .setStyle(
                        MediaStyle()
                                .setShowActionsInCompactView(0, 1)
                                .setMediaSession(mediaSession.sessionToken)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setLargeIcon(largeIconBitmap)
                .build()

        createNotificationChannel()
        startForeground(SCREEN_RECORD_SERVICE_ID, screenRecordNotification)
    }

    private fun hideMediaStyleNotificationSeekBar(mediaSession: MediaSessionCompat) {
        val mediaMetadata = MediaMetadata.Builder()
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
                .build()

        mediaSession.setMetadata(MediaMetadataCompat.fromMediaMetadata(mediaMetadata))
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

    @Suppress("unused")
    private fun clearExistingNotifications(notificationId: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

    }
}