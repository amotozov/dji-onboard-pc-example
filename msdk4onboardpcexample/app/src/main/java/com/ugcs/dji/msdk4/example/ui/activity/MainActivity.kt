package com.ugcs.dji.msdk4.example.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ugcs.dji.msdk4.example.ui.theme.Msdk4onboardpcexampleTheme
import com.ugcs.dji.msdk4.example.ui.viewmodel.MainActivityViewModel
import dji.common.error.DJIError
import dji.common.error.DJISDKError
import dji.sdk.base.BaseComponent
import dji.sdk.base.BaseProduct
import dji.sdk.base.BaseProduct.ComponentKey
import dji.sdk.sdkmanager.DJISDKInitEvent
import dji.sdk.sdkmanager.DJISDKManager
import dji.sdk.sdkmanager.DJISDKManager.SDKManagerCallback
import dji.thirdparty.afinal.core.AsyncTask
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean


class MainActivity : ComponentActivity() {
	private val FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change"
	private val mProduct: BaseProduct? = null
	private lateinit var mHandler: Handler

	private val REQUIRED_PERMISSION_LIST = arrayOf(
		Manifest.permission.VIBRATE,
		Manifest.permission.INTERNET,
		Manifest.permission.ACCESS_WIFI_STATE,
		Manifest.permission.WAKE_LOCK,
		Manifest.permission.ACCESS_COARSE_LOCATION,
		Manifest.permission.ACCESS_NETWORK_STATE,
		Manifest.permission.ACCESS_FINE_LOCATION,
		Manifest.permission.CHANGE_WIFI_STATE,
		Manifest.permission.WRITE_EXTERNAL_STORAGE,
		Manifest.permission.BLUETOOTH,
		Manifest.permission.BLUETOOTH_ADMIN,
		Manifest.permission.READ_EXTERNAL_STORAGE,
		Manifest.permission.READ_PHONE_STATE
	)
	private val missingPermission: MutableList<String> = ArrayList()
	private val isRegistrationInProgress = AtomicBoolean(false)
	private val REQUEST_PERMISSION_CODE = 12345
	private lateinit var viewModel: MainActivityViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val viewModel: MainActivityViewModel by viewModels()
		this.viewModel = viewModel

		// When the compile and target version is higher than 22, please request the following permission at runtime to ensure the SDK works well.
		checkAndRequestPermissions();
		//Initialize DJI SDK Manager
		mHandler = Handler(Looper.getMainLooper());

		setContent {
			Msdk4onboardpcexampleTheme {
				// A surface container using the 'background' color from the theme
				Surface(
					modifier = Modifier.fillMaxSize(),
					color = MaterialTheme.colorScheme.background
				) {
					Column {
						AppName()
						DroneStatus()
						ConnectToDrone()
						SendMessageToOnboardSdk()
						Messages()
					}
				}
			}
		}
	}

	private fun checkAndRequestPermissions() {
		// Check for permissions
		for (eachPermission in REQUIRED_PERMISSION_LIST) {
			if (ContextCompat.checkSelfPermission(
					this,
					eachPermission
				) != PackageManager.PERMISSION_GRANTED
			) {
				missingPermission.add(eachPermission)
			}
		}
		// Request for missing permissions
		if (missingPermission.isEmpty()) {
			startSDKRegistration()
		} else {
			showToast("Need to grant the permissions!")
			ActivityCompat.requestPermissions(
				this,
				missingPermission.toTypedArray(),
				REQUEST_PERMISSION_CODE
			)
		}
	}

	private fun startSDKRegistration() {
		if (isRegistrationInProgress.compareAndSet(false, true)) {
			AsyncTask.execute {
				showToast("registering, pls wait...")
				DJISDKManager.getInstance()
					.registerApp(this@MainActivity.applicationContext, object : SDKManagerCallback {
						override fun onRegister(djiError: DJIError) {
							if (djiError === DJISDKError.REGISTRATION_SUCCESS) {
								showToast("Register Success")
								DJISDKManager.getInstance().startConnectionToProduct()
							} else {
								showToast("Register sdk fails, please check the bundle id and network connection!")
							}
							Timber.i(djiError.getDescription())
						}

						override fun onProductDisconnect() {
							Timber.i("onProductDisconnect")
							showToast("Product Disconnected")
							notifyStatusChange()
						}

						override fun onProductConnect(baseProduct: BaseProduct) {
							Timber.i(String.format("onProductConnect newProduct:%s", baseProduct))
							showToast("Product Connected")
							viewModel.productRegisteredAndConnected()
							notifyStatusChange()
						}

						override fun onProductChanged(p0: BaseProduct?) {
							Timber.i("onProductChanged: %s", p0)
						}

						override fun onComponentChange(
							componentKey: ComponentKey?, oldComponent: BaseComponent?,
							newComponent: BaseComponent?
						) {
							newComponent?.setComponentListener { isConnected ->
								Timber.i("onComponentConnectivityChanged: $isConnected")
								notifyStatusChange()
							}
							Timber.i(
								"onComponentChange key:%s, oldComponent:%s, newComponent:%s",
								componentKey, oldComponent, newComponent
							)
						}

						override fun onInitProcess(djisdkInitEvent: DJISDKInitEvent, i: Int) {}
						override fun onDatabaseDownloadProgress(l: Long, l1: Long) {}
					})
			}
		}
	}

	private fun notifyStatusChange() {
		mHandler.removeCallbacks(updateRunnable)
		mHandler.postDelayed(updateRunnable, 500)
	}

	private val updateRunnable = Runnable {
		val intent = Intent(FLAG_CONNECTION_CHANGE)
		sendBroadcast(intent)
	}

	private fun showToast(toastMsg: String) {
		val handler = Handler(Looper.getMainLooper())
		handler.post { Toast.makeText(applicationContext, toastMsg, Toast.LENGTH_LONG).show() }
	}

}

@Composable
fun AppName(modifier: Modifier = Modifier) {
	Text("MSDK4 OnBoard PC Example")
}

@Composable
fun DroneStatus(
	modifier: Modifier = Modifier,
	viewModel: MainActivityViewModel = viewModel()
) {
	val mainState by viewModel.state.collectAsState()
	Row(modifier = modifier) {
		Text(
			text = "Connected drone model: ${mainState.droneModelName}",
			modifier = modifier
		)
	}
}

@Composable
fun SendMessageToOnboardSdk(
	modifier: Modifier = Modifier,
	viewModel: MainActivityViewModel = viewModel()
) {
	Button(onClick = {
		viewModel.send()
	}) {
		Text("Send")
	}
}

@Composable
fun ConnectToDrone(
	modifier: Modifier = Modifier,
	viewModel: MainActivityViewModel = viewModel()
) {
	val state by viewModel.state.collectAsState()
	Button(onClick = { viewModel.connectToDrone() }, enabled = state.productRegisteredAndConnected) {
		Text("Connect to drone")
	}
}

@Composable
fun Messages(
	modifier: Modifier = Modifier,
	viewModel: MainActivityViewModel = viewModel()
) {
	val mainState by viewModel.state.collectAsState()
	Column(modifier = Modifier
		.fillMaxHeight()
		.verticalScroll(rememberScrollState())) {
		for (message in mainState.log) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(text = "[${message.time}]")
				Text(text = message.text)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
	Msdk4onboardpcexampleTheme {
		Column {
			DroneStatus()
			ConnectToDrone()
			SendMessageToOnboardSdk()
			Messages()
		}
	}
}