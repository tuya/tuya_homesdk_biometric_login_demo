package com.thingclips.smart.biometrics_login

import android.view.View

private const val DEFAULT_INTERVAL: Long = 600

fun View.preventRepeatedClick(listener: View.OnClickListener) {
    this.setOnClickListener(PreventRepeatedClickListener(DEFAULT_INTERVAL, listener))
}

fun View.preventRepeatedClick(interval: Long, listener: View.OnClickListener) {
    this.setOnClickListener(PreventRepeatedClickListener(interval, listener))
}

private class PreventRepeatedClickListener(
    private val interval: Long,
    private val listener: View.OnClickListener
) : View.OnClickListener {

    private var lastClickTime: Long = 0

    override fun onClick(v: View?) {
        val time = System.currentTimeMillis()
        if (time - lastClickTime > interval) {
            lastClickTime = time
            listener.onClick(v)
        }
    }
}
