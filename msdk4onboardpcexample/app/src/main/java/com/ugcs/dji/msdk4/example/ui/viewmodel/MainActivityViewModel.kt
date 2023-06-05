package com.ugcs.dji.msdk4.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ugcs.dji.msdk4.example.ui.DjiOnBoardMessageSender
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
	var onBoardSender: DjiOnBoardMessageSender? = null
	var aircraft: Aircraft? = null

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
			val mProduct = DJISDKManager.getInstance().product as? Aircraft
			aircraft = mProduct
			if (aircraft == null) {
				Timber.i("Aircraft not connected")
				_state.update { currentState ->
					currentState.copy(
						droneModelName = "not_connected",
						droneConnected = false
					)
				}
				return@launch
			}
			aircraft?.let {
				Timber.i("Aircrat connected. Model name: %s", it.model.displayName)
				val model = it.model
				val fc = it.flightController
				if (fc == null) {
					Timber.i("OnBoard Sender not initailized. FlightController null")
					_state.update { currentState ->
						currentState.copy(
							log = currentState.log.apply {
								add(
									Message(
										System.currentTimeMillis(),
										"OnBoard Sender not initialized. FlightController null"
									)
								)
							}
						)
					}
					return@launch
				}
				onBoardSender = DjiOnBoardMessageSender(fc, viewModelScope).also {
					viewModelScope.launch {
						it.receivedDataFlow.collect {
							_state.update { currentState ->
								currentState.copy(
									log = mutableListOf(*currentState.log.toTypedArray() + Message(
										System.currentTimeMillis(),
										"Received message: $it"
									)
									)
								)
							}
						}
					}
				}
				_state.update { currentState ->
					currentState.copy(
						droneModelName = model.displayName,
						droneConnected = true
					)
				}
			}
		}
	}

	fun send() {
		viewModelScope.launch {
			if (onBoardSender == null) {
				_state.update { currentState ->
					currentState.copy(
						log = currentState.log.apply {
							add(
								Message(
									System.currentTimeMillis(),
									"OnBoard Sender not initailized."
								)
							)
						}
					)
				}
				return@launch
			}
			onBoardSender?.sendTestMessage()
			_state.update { currentState ->
				currentState.copy(
					log = currentState.log.apply {
						add(
							Message(
								System.currentTimeMillis(),
								"Sending message: Hello MSDK4 onboard"
							)
						)
					}
				)
			}
		}
	}
}

data class MainActivityData(
	var droneModelName: String,
	var droneConnected: Boolean,
	var productRegisteredAndConnected: Boolean,
	var log: MutableList<Message>
)

data class Message(
	var time: Long,
	var text: String
)