package com.ugcs.dji.msdk4.example

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex
import com.secneo.sdk.Helper
import dji.sdk.base.BaseProduct
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import timber.log.Timber.*
import timber.log.Timber.Forest.plant


class Msdk4OnboardExample: Application() {
	companion object {
		lateinit var app: Application
		var product: BaseProduct? = null

		fun getProductInstance(): BaseProduct? {
			product = DJISDKManager.getInstance().product
			return product
		}

		fun isAircraftConnected(): Boolean {
			return getProductInstance() != null && getProductInstance() is Aircraft
		}

		fun getAircraftInstance(): Aircraft? {
			if (!isAircraftConnected())
				return null
			return getProductInstance() as Aircraft
		}
	}

	override fun onCreate() {
		super.onCreate()
		plant(DebugTree())
	}

	override fun attachBaseContext(base: Context?) {
		super.attachBaseContext(base)
		MultiDex.install(this)
		Helper.install(this)
		app = this
	}
}