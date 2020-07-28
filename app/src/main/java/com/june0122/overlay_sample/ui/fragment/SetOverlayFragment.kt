package com.june0122.overlay_sample.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
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
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.service.ForegroundService
import com.june0122.overlay_sample.service.ForegroundService.Companion.startService
import com.june0122.overlay_sample.service.ForegroundService.Companion.stopService
import com.june0122.overlay_sample.utils.OverlayImageButton
import kotlinx.android.synthetic.main.fragment_set_overlay.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

class SetOverlayFragment : Fragment() {
    companion object {
        const val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1
        const val PERMISSIONS_MULTIPLE_REQUEST = 2
        const val REQUEST_MEDIA_PROJECTION_SCREENSHOT = 1001
        const val REQUEST_MEDIA_PROJECTION_VIDEO = 1002
    }

    private val requiredPermissionList = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    )

    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var overlayView: View
    private lateinit var overlayButton: OverlayImageButton
    private lateinit var params: WindowManager.LayoutParams

    private var screenDensity: Int = 0
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private var mp: MediaProjection? = null
    private var mpManager: MediaProjectionManager? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var captureResultImageView: ImageView? = null
    private var wm: WindowManager? = null
    private var startClickTime: Long = 0
    private var xCoordinate: Float = 0f
    private var yCoordinate: Float = 0f

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

        val displayMetrics = DisplayMetrics()
        mActivity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenDensity = displayMetrics.densityDpi
        displayWidth = displayMetrics.widthPixels
        displayHeight = displayMetrics.heightPixels
        mpManager = mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureResultImageView = mActivity.findViewById(R.id.captureResultImageView)

        params = WindowManager
                .LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        0,
                        0,
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
                    gravity = Gravity.NO_GRAVITY
                }

        initMediaActionOfButton(
                startScreenshotButton,
                REQUEST_MEDIA_PROJECTION_SCREENSHOT,
                R.string.start_screenshot,
                R.string.deactivate_screenshot
        )

        initMediaActionOfButton(
                startVideoCaptureButton,
                REQUEST_MEDIA_PROJECTION_VIDEO,
                R.string.start_video_capture,
                R.string.deactivate_screen_record
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (!Settings.canDrawOverlays(mContext)) {
                    Toast.makeText(mContext, "오버레이 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_MEDIA_PROJECTION_SCREENSHOT -> {
                setMediaActionOfButton(
                        ::getScreenshot,
                        startScreenshotButton,
                        resultCode,
                        data,
                        R.drawable.ic_twotone_camera,
                        R.string.stop_screenshot,
                        R.string.activate_screenshot
                )
            }

            REQUEST_MEDIA_PROJECTION_VIDEO -> {
                setMediaActionOfButton(
                        ::getScreenRecord,
                        startVideoCaptureButton,
                        resultCode,
                        data,
                        R.drawable.ic_twotone_videocam,
                        R.string.stop_video_capture,
                        R.string.activate_screen_record
                )
            }
        }
    }

    private fun initMediaActionOfButton(
            activatedButton: Button,
            requestCode: Int,
            toggleOnText: Int,
            deactivateMsg: Int
    ) {
        activatedButton.setOnClickListener {
            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context?.packageName)
                )
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            } else {
                when (activatedButton.isSelected) {
                    true -> {
                        wm?.removeView(overlayView)
                        stopService(mContext)
                        virtualDisplay?.release()
                        activatedButton.isSelected = false
                        activatedButton.setText(toggleOnText)

                        Toast.makeText(context, deactivateMsg, Toast.LENGTH_SHORT).show()
                    }

                    false -> {
                        startActivityForResult(mpManager?.createScreenCaptureIntent(), requestCode)
                        startService(mContext, "서비스가 실행 중입니다.")
                    }
                }
            }
        }
    }

    private fun setMediaActionOfButton(
            mediaAction: () -> Unit,
            activatedButton: Button,
            resultCode: Int,
            data: Intent?,
            buttonImage: Int, toggleOffText: Int, activateMsg: Int
    ) {
        if (resultCode != RESULT_OK) {
            stopService(mContext)
            Toast.makeText(mContext, "캡처 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()

            return
        }

        if (data != null && resultCode == RESULT_OK && !activatedButton.isSelected) {
            activatedButton.isSelected = true
            activatedButton.setText(toggleOffText)

            mActivity.setResult(RESULT_OK)
            setUpMediaProjection(resultCode, data)
            Log.d("debug", "Setup Media Projection")

            overlayView = View.inflate(mContext, R.layout.overlay_media_action_button, null)
            overlayButton = overlayView.findViewById(R.id.mediaActionButton)
            overlayButton.setImageResource(buttonImage)
            wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm?.addView(overlayView, params)
            Toast.makeText(context, activateMsg, Toast.LENGTH_SHORT).show()
            overlayButtonListener(mediaAction)
        }
    }

    private fun overlayButtonListener(action: () -> Unit) {
        overlayButton.apply {
            setOnClickListener {
                Log.d("debug", "CLICK_OVERLAY_BUTTON")
                GlobalScope.launch(Main) {
                    delay(80L)
                    action()
                    overlayView.visibility = View.VISIBLE
                    wm?.updateViewLayout(overlayView, params)
                }
                overlayView.visibility = View.GONE
                wm?.updateViewLayout(overlayView, params)
            }

            setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    when (event?.action) {
                        MotionEvent.ACTION_DOWN -> {
                            startClickTime = Calendar.getInstance().timeInMillis
                            xCoordinate = overlayView.x - event.rawX + params.x
                            yCoordinate = overlayView.y - event.rawY + params.y

                            touchEventLogging(event)

                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            GlobalScope.launch(Main) {
                                delay(10L)
                                wm?.updateViewLayout(overlayView, params)

                                touchEventLogging(event)

                            }
                            params.x = (event.rawX + xCoordinate).toInt()
                            params.y = (event.rawY + yCoordinate).toInt()
                        }

                        MotionEvent.ACTION_UP -> {
                            val clickDuration = Calendar.getInstance().timeInMillis - startClickTime
                            if (clickDuration < OverlayImageButton.MAX_CLICK_DURATION) {
                                overlayButton.performClick()
                                Log.d("debug", "ACTION_UP")
                            }
                        }
                    }
                    return true
                }
            })
        }
    }

    private fun touchEventLogging(event: MotionEvent) {
        Log.d(
                "debug",
                "ACTION_MOVE : " +
                        "[params] ${params.x}, ${params.y} / " +
                        "[event] ${event.rawX}, ${event.rawY} / " +
                        "[coordinate] $xCoordinate, $yCoordinate"
        )
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

        captureResultImageView?.setImageBitmap(bitmap)
    }

    private fun getScreenRecord() {
        Toast.makeText(context, "getScreenRecord()", Toast.LENGTH_SHORT).show()
        // Write code to screen recording
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
        virtualDisplay?.release()
        Log.d("debug", "release VirtualDisplay")

        super.onDestroy()
    }
}
