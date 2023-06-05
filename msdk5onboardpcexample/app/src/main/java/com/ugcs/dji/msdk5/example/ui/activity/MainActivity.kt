package com.ugcs.dji.msdk5.example.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ugcs.dji.msdk5.example.ui.theme.Msdk5onboardpcexampleTheme
import com.ugcs.dji.msdk5.example.ui.viewmodel.MainActivityViewModel
import dji.v5.common.error.IDJIError
import dji.v5.common.register.DJISDKInitEvent
import dji.v5.manager.SDKManager
import dji.v5.manager.interfaces.SDKManagerCallback
import timber.log.Timber

class MainActivity : ComponentActivity() {

	private lateinit var viewModel: MainActivityViewModel

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val viewModel: MainActivityViewModel by viewModels()
		this.viewModel = viewModel
		registerApp()
		setContent {
			Msdk5onboardpcexampleTheme {
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

	private fun registerApp() {
		SDKManager.getInstance().init(this, object : SDKManagerCallback {
			override fun onRegisterSuccess() {
				Timber.i("onRegisterSuccess")
				viewModel.productRegisteredAndConnected()
			}

			override fun onRegisterFailure(error: IDJIError?) {
				Timber.i("onRegisterFailure")
			}

			override fun onProductDisconnect(productId: Int) {
				Timber.i("onProductDisconnect")
			}

			override fun onProductConnect(productId: Int) {
				Timber.i("onProductConnect: ${productId}")
			}

			override fun onProductChanged(productId: Int) {
				Timber.i("onProductChanged: ${productId}")
			}

			override fun onInitProcess(event: DJISDKInitEvent?, totalProcess: Int) {
				Timber.i("onInitProcess: ")
				if (event == DJISDKInitEvent.INITIALIZE_COMPLETE) {
					SDKManager.getInstance().registerApp()
				}
			}

			override fun onDatabaseDownloadProgress(current: Long, total: Long) {
				Timber.i("onDatabaseDownloadProgress: ${current / total}")
			}

		})
	}
}

@Composable
fun AppName(modifier: Modifier = Modifier) {
	Text("MSDK5 OnBoard PC Example")
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
	Msdk5onboardpcexampleTheme {
		Column {
			DroneStatus()
			ConnectToDrone()
			SendMessageToOnboardSdk()
			Messages()
		}
	}
}