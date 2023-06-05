package com.ugcs.dji.msdk5.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ugcs.dji.msdk5.example.DjiOnBoardMessageSender
import com.ugcs.dji.msdk5.example.KeyManagerFacade
import dji.sdk.keyvalue.key.CameraKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.ProductKey
import dji.sdk.keyvalue.value.product.ProductType
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber

class MainActivityViewModel : ViewModel() {
	val _state = MutableStateFlow(
		MainActivityData(
			droneModelName = "not_connected",
			droneConnected = false,
			productRegisteredAndConnected = false,
			log = mutableListOf(Message(System.currentTimeMillis(), "Application started"))
		)
	)
	val state = _state.asStateFlow()
	private val keyManager = KeyManagerFacade.create()
	private val aircraft = MutableStateFlow<DjiAircraft?>(null)
	private var messageSender: DjiOnBoardMessageSender? = null

	data class DjiAircraft(
		val sn: String,
		val platform: String
	)

	fun productRegisteredAndConnected() {
		viewModelScope.launch {
			_state.update { currentState ->
				currentState.copy(
					productRegisteredAndConnected = true
				)
			}
		}
	}

	fun connectToDrone() {
		viewModelScope.launch {
			keyManager.flow(FlightControllerKey.KeyConnection).collect { isConnected ->
				Timber.i("Aircraft is connected: $isConnected")
				if (isConnected == true) {
					try {
						withTimeout(10000) {
							val sn = keyManager.flow(FlightControllerKey.KeySerialNumber)
								.first { it != null }!!
							val vehiclePlatform =
								keyManager.flow(ProductKey.KeyProductType).first { it != null }
									.toVehiclePlatformString()
							Timber.i("Aircraft connected: {sn: '$sn', platform: '$vehiclePlatform'}")
							if (aircraft.value != null
								&& aircraft.value?.sn != sn
								&& aircraft.value?.platform != vehiclePlatform) {
								// Connected another new aircraft.
								aircraft.value = null
							}
							if (aircraft.value == null) {
								aircraft.value = DjiAircraft(sn, vehiclePlatform)
								_state.update { currentState ->
									currentState.copy(
										droneModelName = vehiclePlatform,
										droneConnected = true
									)
								}
								messageSender = DjiOnBoardMessageSender(viewModelScope)
							}
						}
					} catch (e: TimeoutCancellationException) {
						Timber.e("Failed to initialize DjiAircraft.", e)
					}
				} else {
					aircraft.value = null
				}
			}
		}
	}

	fun send() {
		viewModelScope.launch {
			Timber.i("Send message call")
			if (messageSender == null) {
				Timber.i("Aircraft not connected")
			}
			messageSender?.send()
		}
	}
}

data class MainActivityData(
	var droneModelName: String,
	var droneConnected: Boolean,
	val productRegisteredAndConnected: Boolean,
	var log: MutableList<Message>
)

data class Message(
	var time: Long,
	var text: String
)

fun ProductType?.toVehiclePlatformString(): String {
	return when (this) {
		ProductType.M300_RTK -> "M300_RTK"
		ProductType.DJI_MAVIC_3_ENTERPRISE_SERIES -> "Mavic3_Enterprise_Series"
		ProductType.M30_SERIES -> "Matrice30_Series"
		else -> throw UnsupportedOperationException("Unsupported product type: $this")
	}
}