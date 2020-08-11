package com.june0122.overlay_sample.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.service.ScreenRecordService
import com.june0122.overlay_sample.service.ScreenshotService
import com.june0122.overlay_sample.utils.*
import kotlinx.android.synthetic.main.fragment_set_overlay.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList

class SetOverlayFragment : Fragment() {
    companion object {
        const val REQUEST_MEDIA_PROJECTION_SCREENSHOT = 1001
        const val REQUEST_MEDIA_PROJECTION_VIDEO = 1002

        private val ORIENTATIONS = SparseIntArray()

        fun createOrientations() {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }
    }

    private val requiredPermissionList = listOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    )

    private lateinit var mContext: Context
    private lateinit var mActivity: Activity
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var overlayView: View
    private lateinit var overlayButton: OverlayImageButton
    private lateinit var screenRecordVideoView: VideoView
    private lateinit var screenshotImageView: ImageView
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var realDisplaySize: Point
    private lateinit var overlayIntent: Intent

    private var screenDensity: Int = 0
    private var displayWidth: Int = 0
    private var displayHeight: Int = 0
    private var displayRatio: Float = 0f
    private var mediaProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaProjectionCallback: MediaProjection.Callback? = null
    private var mpManager: MediaProjectionManager? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var wm: WindowManager? = null
    private var startClickTime: Long = 0
    private var xCoordinate: Float = 0f
    private var yCoordinate: Float = 0f
    private var videoUri = ""
    private var isRecording = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = context as Activity
        mContext = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_set_overlay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context?.packageName)
        )

        checkMultiplePermissions()
        checkOverlayPermission()

        val displayMetrics = DisplayMetrics()
        mActivity.windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        screenDensity = displayMetrics.densityDpi
        realDisplaySize = getScreenSize(mContext)
        displayWidth = realDisplaySize.x
        displayHeight = realDisplaySize.y
        displayRatio = displayHeight.toFloat() / displayWidth.toFloat()
        mpManager = mActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenshotImageView = mActivity.findViewById(R.id.screenshotImageView)

        mediaRecorder = MediaRecorder()
        rootLayout = mActivity.findViewById(R.id.rootLayout)
        screenRecordVideoView = mActivity.findViewById(R.id.screenRecordVideoView)

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

    private fun initMediaActionOfButton(
            activatedButton: Button,
            requestCode: Int,
            toggleOnText: Int,
            deactivateMsg: Int
    ) {
        activatedButton.setOnClickListener {
            if (!Settings.canDrawOverlays(context)) {
                requestOverlayPermission.launch(overlayIntent)
            } else {
                when (activatedButton) {
                    startScreenshotButton -> {
                        checkButtonStatus(activatedButton, screenshotLauncher, toggleOnText, deactivateMsg, requestCode) {
                            ScreenshotService.stopService(mContext)
                        }
                    }

                    startVideoCaptureButton -> {
                        checkButtonStatus(activatedButton, screenRecordLauncher, toggleOnText, deactivateMsg, requestCode) {
                            ScreenRecordService.stopService(mContext)
                        }
                    }
                }
            }
        }
    }

    private fun checkButtonStatus(
            activatedButton: Button,
            launcher: ActivityResultLauncher<Int>,
            toggleOnText: Int,
            deactivateMsg: Int,
            requestCode: Int,
            stopActivatedService: () -> Unit
    ) {
        when (activatedButton.isSelected) {
            true -> {
                stopActivatedService()
                wm?.removeView(overlayView)
                virtualDisplay?.release()
                activatedButton.isSelected = false
                activatedButton.setText(toggleOnText)
                Toast.makeText(context, deactivateMsg, Toast.LENGTH_SHORT).show()
            }
            false -> {
                launcher.launch(requestCode)
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
        activatedButton.isSelected = true
        activatedButton.setText(toggleOffText)
        mActivity.setResult(RESULT_OK)

        if (data != null) {
            when (activatedButton) {
                startScreenshotButton -> {
                    ScreenshotService.startService(mContext, "서비스가 실행 중입니다.")
                    setOverlayButton(mediaAction, resultCode, data, buttonImage, activateMsg)
                }

                startVideoCaptureButton -> {
                    ScreenRecordService.startService(mContext, "서비스가 실행 중입니다.")
                    setOverlayButton(mediaAction, resultCode, data, buttonImage, activateMsg)
                }
            }
        }
    }

    private fun setOverlayButton(mediaAction: () -> Unit, resultCode: Int, data: Intent, buttonImage: Int, activateMsg: Int) {
        GlobalScope.launch(Main) {
            delay(80L)
            setUpMediaProjection(mediaAction, resultCode, data)
        }

        overlayView = View.inflate(mContext, R.layout.overlay_media_action_button, null)
        overlayButton = overlayView.findViewById(R.id.mediaActionButton)
        overlayButton.setImageResource(buttonImage)
        wm = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm?.addView(overlayView, params)
        Toast.makeText(context, activateMsg, Toast.LENGTH_SHORT).show()
        overlayButtonListener(mediaAction)
    }

    private fun overlayButtonListener(action: () -> Unit) {
        overlayButton.apply {
            setOnClickListener {
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
//                            touchEventLogging(event)
                            return true
                        }

                        MotionEvent.ACTION_MOVE -> {
                            GlobalScope.launch(Main) {
                                delay(10L)
                                wm?.updateViewLayout(overlayView, params)
//                                touchEventLogging(event)
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

//    private fun touchEventLogging(event: MotionEvent) {
//        Log.d(
//                "debug",
//                "ACTION_MOVE : " +
//                        "[params] ${params.x}, ${params.y} / " +
//                        "[event] ${event.rawX}, ${event.rawY} / " +
//                        "[coordinate] $xCoordinate, $yCoordinate"
//        )
//    }

    private fun setUpMediaProjection(mediaAction: () -> Unit, code: Int, intent: Intent) {
        mediaProjection = mpManager?.getMediaProjection(code, intent)
        Log.d("debug", "Setup Media Projection")

        setUpVirtualDisplay(mediaAction)
    }

    private fun setUpVirtualDisplay(mediaAction: () -> Unit) {
        when (mediaAction) {
            ::getScreenshot -> {
                imageReader = ImageReader.newInstance(
                        displayWidth, displayHeight, PixelFormat.RGBA_8888, 2
                )

                virtualDisplay = mediaProjection
                        ?.createVirtualDisplay(
                                "Screenshot",
                                displayWidth,
                                displayHeight,
                                screenDensity,
                                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                                imageReader?.surface,
                                null,
                                null
                        )
            }

//            ::getScreenRecord -> {
//            }
        }
    }

    private fun getScreenshot() {
        Log.d("debug", "Screenshot")

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

        showScreenshotResult(bitmap)
    }

    private fun showScreenshotResult(bitmap: Bitmap) {
        GlobalScope.launch(Main) {
            delay(2500)
            screenshotImageView.visibility = View.GONE
        }
        addTranslateAnimation(screenshotImageView)
        screenshotImageView.visibility = View.VISIBLE
        screenshotImageView.setImageBitmap(bitmap)
    }

    private fun getScreenRecord() {
        Log.d("debug", "Screen Record")
        Toast.makeText(context, "getScreenRecord", Toast.LENGTH_SHORT).show()

        toggleScreenShare(overlayButton)
    }

    private fun checkMultiplePermissions() {
        val rejectedPermissionList = ArrayList<String>()

        for (permission in requiredPermissionList) {
            if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) {
                rejectedPermissionList.add(permission)
            }
        }

        if (rejectedPermissionList.isNotEmpty()) {
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            requestMultiplePermissions.launch(rejectedPermissionList.toArray(array))
        }
    }

    private fun checkOverlayPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (!Settings.canDrawOverlays(context)) {
                    requestOverlayPermission.launch(overlayIntent)
                }
            }
        }
    }

    override fun onDestroy() {
        if (wm != null) {
            wm?.removeView(overlayView)
        }

        ScreenshotService.stopService(mContext)
        ScreenRecordService.stopService(mContext)
        virtualDisplay?.release()

        Log.d("debug", "release VirtualDisplay")

        super.onDestroy()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun toggleScreenShare(view: View) {
        if (!isRecording) {
            initRecorder()
            recordScreen()
            isRecording = true
        } else {

            try {
                mediaRecorder?.stop()
            } catch (stopException: RuntimeException) {
                stopException.printStackTrace()
            }
            mediaRecorder?.reset()
            isRecording = false

            showScreenRecordResult()
        }
    }

    private fun showScreenRecordResult() {
        screenRecordVideoView.apply {
            addTranslateAnimation(this)
            visibility = View.VISIBLE
            setVideoURI(Uri.parse(videoUri))
            start()

            setOnCompletionListener {
                screenRecordVideoView.stopPlayback()
                screenRecordVideoView.visibility = View.GONE
            }
        }
    }

    private fun addTranslateAnimation(v: View) {
        v.tag = v.visibility
        v.viewTreeObserver.addOnGlobalLayoutListener {
            val newVisibility = v.visibility
            if (v.tag != newVisibility) {
                v.tag = newVisibility
                val animation: TranslateAnimation

                if (newVisibility == View.VISIBLE) {
                    animation = TranslateAnimation(0f, 0f, -v.height.toFloat(), 0f)
                    animation.interpolator = DecelerateInterpolator()
                } else {
                    Log.d("Animation", "OK")
                    animation = TranslateAnimation(0f, 0f, 0f, -v.height.toFloat())
                    animation.interpolator = AccelerateInterpolator()
                }

                animation.duration = 350
                v.startAnimation(animation)
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun recordScreen() {
        virtualDisplay = createVirtualDisplay()
        mediaRecorder?.start()
    }

    private fun createVirtualDisplay(): VirtualDisplay? {
        return mediaProjection?.createVirtualDisplay(
                "ScreenRecord",
                displayWidth,
                displayHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
        )
    }

    @SuppressLint("SimpleDateFormat")
    private fun initRecorder() {
        videoUri = ""
        videoUri = mActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                .toString() + java.lang.StringBuilder("/AHAM_")
                .append(java.text.SimpleDateFormat("dd-MM-yyyy-hh_mm_ss")
                        .format(Date())).append(".mp4").toString()

        Log.d("debug", "getFilePath: $videoUri")

        try {
            mediaRecorder?.apply {
                createOrientations()

                val rotation = mActivity.windowManager.defaultDisplay.rotation
                val orientation = ORIENTATIONS.get(rotation + 90)

                setAudioSource(MediaRecorder.AudioSource.DEFAULT)
                setVideoSource(MediaRecorder.VideoSource.SURFACE)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setVideoFrameRate(60)
                setVideoSize(displayWidth, displayHeight)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                setOutputFile(videoUri)
                setVideoEncodingBitRate(VIDEO_MAX_BITRATE_1080P_60)
                setAudioSamplingRate(AUDIO_SAMPLING_RATE_44100)
                setAudioEncodingBitRate(AUDIO_BITRATE_320K)
                setOrientationHint(orientation)
                try {
                    prepare()
                } catch (stopException: RuntimeException) {
                    stopException.printStackTrace()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun stopRecordScreen() {
        if (virtualDisplay == null)
            return

        virtualDisplay?.release()
        destroyMediaProjection()
    }

    private fun destroyMediaProjection() {
        mediaProjection?.unregisterCallback(mediaProjectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }

    inner class MediaProjectionCallback : MediaProjection.Callback() {
        override fun onStop() {
            if (startVideoCaptureButton.isSelected) {
                startVideoCaptureButton.isSelected = false
                mediaRecorder?.stop()
                mediaRecorder?.reset()
            }
            mediaProjection = null
            stopRecordScreen()
            super.onStop()
        }
    }

    private val requestOverlayPermission =
            registerForActivityResult(StartActivityForResult()) {
                if (!Settings.canDrawOverlays(mContext)) {
                    Toast.makeText(mContext, "오버레이 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

    private val requestMultiplePermissions =
            registerForActivityResult(RequestMultiplePermissions()) { permissions ->
                if (permissions.isNotEmpty()) {
                    for ((permission, grantResult) in permissions) {
                        if (!grantResult) {
                            Toast.makeText(mContext, "$permission 거부되었습니다.", Toast.LENGTH_SHORT).show()
                            Log.d("permission", "$permission denied")
                        }
                    }
                }
            }

    private val screenshotLauncher =
            registerForActivityResult(ScreenshotContract()) {
                mediaProjectionCallback = MediaProjectionCallback()
                mediaProjection?.registerCallback(mediaProjectionCallback, null)
            }

    private val screenRecordLauncher =
            registerForActivityResult(ScreenRecordContract()) {
                mediaProjectionCallback = MediaProjectionCallback()
                mediaProjection?.registerCallback(mediaProjectionCallback, null)
            }

    inner class ScreenshotContract : ActivityResultContract<Int, String>() {

        override fun createIntent(context: Context, input: Int?): Intent =
                Intent(mpManager?.createScreenCaptureIntent()).apply {
                    putExtra("ACTION TYPE", input)
                }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            mediaProjectionCallback = MediaProjectionCallback()
            mediaProjection?.registerCallback(mediaProjectionCallback, null)

            when (resultCode) {
                RESULT_OK -> {
                    setMediaActionOfButton(
                            ::getScreenshot,
                            startScreenshotButton,
                            resultCode,
                            intent,
                            R.drawable.ic_twotone_camera,
                            R.string.stop_screenshot,
                            R.string.activate_screenshot
                    )
                    return null
                }

                RESULT_CANCELED -> {
                    ScreenshotService.stopService(mContext)
                    Toast.makeText(mContext, "캡처 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                    return null
                }

                else -> return null
            }
        }
    }

    inner class ScreenRecordContract : ActivityResultContract<Int, Int>() {

        override fun createIntent(context: Context, input: Int?): Intent =
                Intent(mpManager?.createScreenCaptureIntent()).apply {
                    putExtra("ACTION TYPE", input)
                }

        override fun parseResult(resultCode: Int, intent: Intent?): Int? {
            mediaProjectionCallback = MediaProjectionCallback()
            mediaProjection?.registerCallback(mediaProjectionCallback, null)

            val actionType = Intent().getIntExtra("ACTION TYPE", 1111)
            Log.d("ACTION_TYPE", "$actionType")

            when (resultCode) {
                RESULT_OK -> {
                    setMediaActionOfButton(
                            ::getScreenRecord,
                            startVideoCaptureButton,
                            resultCode,
                            intent,
                            R.drawable.ic_twotone_videocam,
                            R.string.stop_video_capture,
                            R.string.activate_screen_record
                    )
                    return null
                }

                RESULT_CANCELED -> {
                    ScreenRecordService.stopService(mContext)
                    Toast.makeText(mContext, "캡처 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                    return null
                }

                else -> return null
            }
        }
    }
}