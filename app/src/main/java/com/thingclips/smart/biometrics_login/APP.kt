package com.thingclips.smart.biometrics_login

import android.app.Application
import com.thingclips.smart.home.sdk.ThingHomeSdk

class APP: Application () {
    override fun onCreate() {
        super.onCreate()
        ThingHomeSdk.init(this)
    }
}