package com.june0122.overlay_sample.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.ui.service.ForegroundService
import kotlinx.android.synthetic.main.fragment_set_overlay.*

class SetOverlayFragment : Fragment() {
    companion object {
        const val ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1
        const val PERMISSIONS_MULTIPLE_REQUEST = 2
    }

    private val requiredPermissionList = arrayListOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    )

    private lateinit var mContext: Context
    private lateinit var mActivity: Activity

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

        activateOverlayButton.setOnClickListener {
            checkOverlayPermission()
            checkPermissions()
            ForegroundService.startService(view.context, "서비스가 실행 중입니다.")
        }

        deactivateOverlayButton.setOnClickListener {
            ForegroundService.stopService(view.context)
            Toast.makeText(context, "서비스가 종료되었습니다.", Toast.LENGTH_SHORT).show()
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

        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(mContext)) {
                Toast.makeText(mContext, "오버레이 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                mActivity.startService(Intent(mContext, ForegroundService::class.java))
                Toast.makeText(context, "서비스가 실행되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
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
}