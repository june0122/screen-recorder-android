package com.june0122.overlay_sample.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.june0122.overlay_sample.R

class OverlayButtonFragment : Fragment() {
    private val serviceId = "Foreground Service Example"
    private var mView: View? = null
    private var wm: WindowManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_overlay_button, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}