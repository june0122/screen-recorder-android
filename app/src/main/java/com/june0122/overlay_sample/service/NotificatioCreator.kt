package com.june0122.overlay_sample.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.receiver.MyBroadcastReceiver
import com.june0122.overlay_sample.ui.activity.MainActivity
import com.june0122.overlay_sample.utils.*

data class NotificationData(
        var contentTitle: String,
        var mediaSessionTag: String,
        var smallIconImage: Int,
        var largeIconImage: Int
)

object NotificationCreator {
    private const val serviceId = "Screen Record Service"
    private lateinit var notificationData: NotificationData

    fun getNotification(service: Service, context: Context, intent: Intent?): Notification? {
        setNotificationData(service)
        createNotificationChannel(service, context)

        val contentTitle = notificationData.contentTitle
        val contentText = intent?.getStringExtra("inputExtra")
        val largeIconBitmap = BitmapFactory.decodeResource(context.resources, notificationData.largeIconImage)
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingIntent =
                PendingIntent.getActivity(
                        context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
                )

        val closeScreenRecordIntent = PendingIntent.getBroadcast(
                context,
                PI_CODE_DELETE_SCREEN_RECORD_SERVICE,
                Intent(context, MyBroadcastReceiver::class.java)
                        .apply { action = MyBroadcastReceiver.ACTION_DELETE_SCREEN_RECORD_SERVICE },
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val closeScreenshotIntent = PendingIntent.getBroadcast(
                context,
                PI_CODE_DELETE_SCREENSHOT_SERVICE,
                Intent(context, MyBroadcastReceiver::class.java)
                        .apply { action = MyBroadcastReceiver.ACTION_DELETE_SCREENSHOT_SERVICE },
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
                MediaSessionCompat(context, notificationData.mediaSessionTag).apply {
                    hideMediaStyleNotificationSeekBar(this)
                }

        val screenRecordNotification = NotificationCompat.Builder(context, serviceId)
                .setSmallIcon(notificationData.smallIconImage)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(closeScreenRecordIntent)
                .addAction(
                        NotificationCompat.Action(
                                R.drawable.ic_noti_stop,
                                context.getString(R.string.stop),
                                stopRecordIntent
                        )
                )
                .addAction(
                        NotificationCompat.Action(
                                R.drawable.ic_noti_close,
                                context.getString(R.string.close),
                                closeScreenRecordIntent
                        )
                )
                .setStyle(
                        androidx.media.app.NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0, 1)
                                .setMediaSession(mediaSession.sessionToken)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setLargeIcon(largeIconBitmap)
                .build()

        val screenshotNotification = NotificationCompat.Builder(context, serviceId)
                .setSmallIcon(R.drawable.ic_noti_screenshot)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(closeScreenshotIntent)
                .addAction(
                        NotificationCompat.Action(
                                R.drawable.ic_noti_close,
                                context.getString(R.string.close),
                                closeScreenshotIntent
                        )
                )
                .setStyle(
                        androidx.media.app.NotificationCompat.MediaStyle()
                                .setShowActionsInCompactView(0)
                                .setMediaSession(mediaSession.sessionToken)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setLargeIcon(largeIconBitmap)
                .build()

        when (service.javaClass.simpleName) {
            "ScreenRecordService", "AudioCaptureService" -> {
                service.startForeground(SCREEN_RECORD_SERVICE_ID, screenRecordNotification)

                return screenRecordNotification
            }

            "ScreenshotService" -> {
                service.startForeground(SCREENSHOT_SERVICE_ID, screenshotNotification)

                return screenshotNotification
            }
        }

        return null
    }

    private fun setNotificationData(service: Service) {
        when (service.javaClass.simpleName) {
            "ScreenRecordService" -> {
                notificationData = NotificationData(
                        "SCREEN RECORD",
                        "ScreenRecordSession",
                        R.drawable.ic_noti_screen_record_off,
                        R.drawable.portrait_ahri
                )
            }

            "AudioCaptureService" -> {
                notificationData = NotificationData(
                        "SCREEN RECORD",
                        "AudioCaptureSession",
                        R.drawable.ic_noti_screen_record,
                        R.drawable.portrait_tryndamere
                )
            }

            "ScreenshotService" -> {
                notificationData = NotificationData(
                        "SCREEN RECORD",
                        "ScreenshotSession",
                        R.drawable.ic_noti_screenshot,
                        R.drawable.portrait_lux
                )
            }
        }
    }

    private fun hideMediaStyleNotificationSeekBar(mediaSession: MediaSessionCompat) {
        val mediaMetadata = MediaMetadata.Builder()
                .putLong(MediaMetadata.METADATA_KEY_DURATION, -1L)
                .build()

        mediaSession.setMetadata(MediaMetadataCompat.fromMediaMetadata(mediaMetadata))
    }

    private fun createNotificationChannel(service: Service, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            var serviceChannel: NotificationChannel? = null

            when (service.javaClass.simpleName) {
                "ScreenRecordService", "AudioCaptureService" -> {
                    serviceChannel = NotificationChannel(
                            "Screen Record Service",
                            "Screen Record Channel",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
                }

                "ScreenshotService" -> {
                    serviceChannel = NotificationChannel(
                            "Screenshot Service",
                            "Screenshot Channel",
                            NotificationManager.IMPORTANCE_DEFAULT
                    )
                }
            }

            val manager = context.getSystemService(NotificationManager::class.java)

            if (serviceChannel != null) {
                manager?.createNotificationChannel(serviceChannel)
            }
        }
    }

    @Suppress("unused")
    private fun clearExistingNotifications(context: Context, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }
}
