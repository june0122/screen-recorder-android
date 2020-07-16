package com.june0122.overlay_sample.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.june0122.overlay_sample.R
import com.june0122.overlay_sample.ui.fragment.SetOverlayFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, SetOverlayFragment(), SetOverlayFragment::class.java.name)
            .commit()
    }
}