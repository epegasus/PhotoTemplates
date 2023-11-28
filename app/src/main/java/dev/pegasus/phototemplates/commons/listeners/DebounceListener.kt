package dev.pegasus.phototemplates.commons.listeners

import android.os.SystemClock
import android.view.View

object RapidSafeListener {
    private const val RAPID_DEFAULT_TIME = 500L
    private var lastClickTime: Long = 0

    fun View.setOnRapidClickSafeListener(rapidTime: Long = RAPID_DEFAULT_TIME, action: () -> Unit) {
        this.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (SystemClock.elapsedRealtime() - lastClickTime < rapidTime) return
                else action()
                lastClickTime = SystemClock.elapsedRealtime()
            }
        })
    }
}

