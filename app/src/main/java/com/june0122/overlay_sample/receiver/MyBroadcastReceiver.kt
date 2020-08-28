package com.june0122.overlay_sample.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.june0122.overlay_sample.service.ScreenRecordService
import com.june0122.overlay_sample.service.ScreenshotService

class MyBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DELETE_SCREENSHOT_SERVICE = "delete_screenshot_service"
        const val ACTION_DELETE_SCREEN_RECORD_SERVICE = "delete_screen_record_service"
        const val ACTION_STOP_SCREEN_RECORD_SERVICE = "stop_screen_record_service"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("Receiver", "onReceive $action")

        when (intent?.action) {
            ACTION_DELETE_SCREENSHOT_SERVICE -> {
                Toast.makeText(context, "DELETE SERVICE CHECK", Toast.LENGTH_SHORT).show()
                ScreenshotService.stopService(context)
            }

            ACTION_DELETE_SCREEN_RECORD_SERVICE -> {
                ScreenRecordService.stopService(context)
            }

            ACTION_STOP_SCREEN_RECORD_SERVICE -> {
            }
        }
    }
}