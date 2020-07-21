package com.june0122.overlay_sample.ui.fragment

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.service.ForegroundService
import com.june0122.overlay_sample.service.ForegroundService.Companion.startService
import com.june0122.overlay_sample.service.ForegroundService.Companion.stopService
import kotlinx.android.synthetic.main.fragment_set_overlay.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.nio.ByteBuffer


class SetOverlayFragment : Fragment() {
    companion object {
        const val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1
        const val PERMISSIONS_MULTIPLE_REQUEST = 2
        const val REQUEST_MEDIA_PROJECTION = 1001
    }

    private val requiredPermissionList = arrayListOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    )

    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

    private var screenDensity: Int = 0
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private var mp: MediaProjection? = null
    private var mpManager: MediaProjectionManager? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var tempDisplayImageView: ImageView? = null
    private var wm: WindowManager? = null

    private lateinit var overlayView: View
    private lateinit var params: WindowManager.LayoutParams

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as Activity
        mContext = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkOverlayPermission()
        checkPermissions()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_set_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tempDisplayImageView = mActivity.findViewById(R.id.tempImageView)

        val displayMetrics = DisplayMetrics()
        mActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenDensity = displayMetrics.densityDpi
        displayWidth = displayMetrics.widthPixels
        displayHeight = displayMetrics.heightPixels

        mpManager =
                mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        startScreenshotButton.isSelected = false
        startScreenshotButton.setOnClickListener {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context?.packageName)
                )
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            } else {

                when (startScreenshotButton.isSelected) {
                    true -> {
                        wm?.removeView(overlayView)

                        stopService(mContext)
                        virtualDisplay?.release()

                        startScreenshotButton.isSelected = false
                        startScreenshotButton.setText(R.string.start_screenshot)

                        Toast.makeText(context, "스크린샷 버튼이 비활성화되었습니다.", Toast.LENGTH_SHORT).show()
                    }

                    false -> {
                        startActivityForResult(
                                mpManager?.createScreenCaptureIntent(),
                                REQUEST_MEDIA_PROJECTION
                        )

                        startService(mContext, "서비스가 실행 중입니다.")
                    }
                }
            }
        }
    }

    private fun checkOverlayPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + context?.packageName)
                    )
                    startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
                }
            }
            else -> {
                mActivity.startService(Intent(mContext, ForegroundService::class.java))
                Toast.makeText(context, "서비스가 실행되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (!Settings.canDrawOverlays(mContext)) {
                    Toast.makeText(mContext, "오버레이 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode != RESULT_OK) {
                    stopService(mContext)
                    Toast.makeText(mContext, "캡처 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                    return
                }

                if (data != null && resultCode == RESULT_OK && !startScreenshotButton.isSelected) {
                    startScreenshotButton.isSelected = true
                    startScreenshotButton.setText(R.string.stop_screenshot)

                    mActivity.setResult(RESULT_OK)

                    setUpMediaProjection(resultCode, data)
                    Log.d("debug", "Setup Media Projection")


                    params = WindowManager.LayoutParams(
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

                    overlayView = View.inflate(mContext, R.layout.fragment_overlay_button, null)

                    wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    wm?.addView(overlayView, params)

                    Toast.makeText(context, "스크린샷 버튼이 활성화되었습니다.", Toast.LENGTH_SHORT).show()

                    overlayView.setOnClickListener {
                        GlobalScope.launch(Main) {
                            delay(80L)
                            getScreenshot()
                            overlayView.visibility = View.VISIBLE
                            wm?.updateViewLayout(overlayView, params)
                        }
                        overlayView.visibility = View.GONE
                        wm?.updateViewLayout(overlayView, params)
                    }
                }
            }
        }
    }

    private fun setUpMediaProjection(code: Int, intent: Intent) {
        mp = mpManager?.getMediaProjection(code, intent)
        setUpVirtualDisplay()
    }

    private fun setUpVirtualDisplay() {
        imageReader = ImageReader.newInstance(
                displayWidth, displayHeight, PixelFormat.RGBA_8888, 2
        )

        virtualDisplay = mp?.createVirtualDisplay(
                "ScreenCapture",
                displayWidth, displayHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
        )
    }

    private fun getScreenshot() {
        Log.d("debug", "getScreenshot")

        val image: Image = imageReader?.acquireLatestImage() ?: return
        val planes: Array<Image.Plane> = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride: Int = planes[0].pixelStride
        val rowStride: Int = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * displayWidth

        val bitmap: Bitmap = Bitmap.createBitmap(
                displayWidth + rowPadding / pixelStride, displayHeight, Bitmap.Config.ARGB_8888
        )

        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        tempDisplayImageView?.setImageBitmap(bitmap)
    }

    private fun checkPermissions() {
        val rejectedPermissionList = ArrayList<String>()

        for (permission in requiredPermissionList) {
            if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) {
                rejectedPermissionList.add(permission)
            }
        }

        if (rejectedPermissionList.isNotEmpty()) {
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            requestPermissions(rejectedPermissionList.toArray(array), PERMISSIONS_MULTIPLE_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_MULTIPLE_REQUEST && grantResults.isNotEmpty()) {
            for ((i, permission) in permissions.withIndex()) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(mContext, "$permission 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        if (wm != null) {
            wm?.removeView(overlayView)
        }
        stopService(mContext)

        Log.d("debug", "release VirtualDisplay")
        virtualDisplay?.release()
        super.onDestroy()
    }
}