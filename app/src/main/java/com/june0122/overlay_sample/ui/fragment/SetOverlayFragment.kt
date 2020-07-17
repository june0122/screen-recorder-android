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
import com.june0122.overlay_sample.ui.service.ForegroundService
import com.june0122.overlay_sample.ui.service.ForegroundService.Companion.startService
import com.june0122.overlay_sample.ui.service.ForegroundService.Companion.stopService
import kotlinx.android.synthetic.main.fragment_set_overlay.*
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

    private lateinit var rootView: View
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
        startService(mContext, "서비스가 실행 중입니다.")
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

        mpManager = mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        if (mpManager != null) {
//            startActivityForResult(mpManager?.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
//        }

        activateOverlayButton.setOnClickListener {
            checkOverlayPermission()
            checkPermissions()
            startService(mContext, "서비스가 실행 중입니다.")
        }

        deactivateOverlayButton.setOnClickListener {
            stopService(mContext)
            Toast.makeText(mContext, "서비스가 종료되었습니다.", Toast.LENGTH_SHORT).show()
        }

        tempScreenshotButton.setOnClickListener {
//            if (mpManager != null) {
//                startActivityForResult(mpManager?.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
//            }

            if (!Settings.canDrawOverlays(context)) {
                val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context?.packageName)
                )
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            } else {
                startActivityForResult(mpManager?.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)

                startService(mContext, "서비스가 실행 중입니다.")

                wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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

                rootView = LayoutInflater.from(context).inflate(R.layout.fragment_overlay_button, null)
                wm?.addView(rootView, params)


                val screenshotButton: ImageView? = rootView.rootView?.findViewById(R.id.screenshotButton)

                screenshotButton?.setOnClickListener {
                    getScreenshot()
                    Toast.makeText(mContext, "Clicked", Toast.LENGTH_SHORT).show()
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
                } else {
                    mActivity.startService(Intent(mContext, ForegroundService::class.java))
                    Toast.makeText(context, "서비스가 실행되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                mActivity.startService(Intent(mContext, ForegroundService::class.java))
                Toast.makeText(context, "서비스가 실행되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * AndroidX의 Activity 1.2.0-alpha02 와 Fragment 1.3.0-alpha02 부터 새로운 방식의 Activity Result API를 제공되었는데
     * 기존 startActivityForResult() 호출과 onActivityResult(requestCode, resultCode, data) 콜백 호출은
     * alpha stage의 라이브러리에서 @Deprecated annotation이 붙어 있는 상태이므로 새로운 API에 대한 적용을 염두해둬야 하는 부분이다.
     */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (!Settings.canDrawOverlays(mContext)) {
                    Toast.makeText(mContext, "오버레이 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    mActivity.startService(Intent(mContext, ForegroundService::class.java))
                    Toast.makeText(context, "서비스가 실행되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_MEDIA_PROJECTION -> {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(mContext, "User cancelled", Toast.LENGTH_SHORT).show()
                    return
                }

                if (data != null && resultCode == RESULT_OK) {
                    mActivity.setResult(RESULT_OK)
                    Toast.makeText(mContext, "Setup Media Projection", Toast.LENGTH_LONG).show()
                    setUpMediaProjection(resultCode, data)
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
                displayWidth, displayHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mp?.createVirtualDisplay("ScreenCapture",
                displayWidth, displayHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null)

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
                displayWidth + rowPadding / pixelStride, displayHeight,
                Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        tempDisplayImageView?.setImageBitmap(bitmap)
        Toast.makeText(mContext, "스크린이 캡쳐되었습니다.", Toast.LENGTH_SHORT).show()

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
            wm?.removeView(rootView)
        }
        stopService(mContext)

        Log.d("debug", "release VirtualDisplay")
        virtualDisplay?.release()
        super.onDestroy()
    }
}