package com.ugcs.dji.msdk5.example

import android.app.Application
import android.content.Context
import com.secneo.sdk.Helper
import timber.log.Timber.DebugTree
import timber.log.Timber.Forest.plant

class Msdk5OnboardExample : Application() {
	override fun onCreate() {
		super.onCreate()
		plant(DebugTree())
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		Helper.install(this)
	}
}