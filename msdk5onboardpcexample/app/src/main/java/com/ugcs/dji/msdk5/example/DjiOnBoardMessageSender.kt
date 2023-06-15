package com.ugcs.dji.msdk5.example

import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.OnboardKey
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.v5.common.callback.CommonCallbacks.CompletionCallback
import dji.v5.common.error.IDJIError
import dji.v5.et.set
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
import dji.v5.manager.interfaces.IPayloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.charset.StandardCharsets

class DjiOnBoardMessageSender(
	val coroutineScope: CoroutineScope
) {

	val receivedData = mutableListOf<String>()
	val receivedDataFlow = MutableSharedFlow<String>()
	val connectedPayloadConnection = mutableListOf<IPayloadManager>()
	val handlers = mutableListOf<(data: ByteArray) -> Unit>()
	var onboardSdkSendDataKey: DJIKey.ActionKey<ByteArray, EmptyMsg>

	init {
		try {
			onboardSdkSendDataKey = KeyTools.createKey(
				OnboardKey.KeySendDataToOnBoardSDKDevice,
				ComponentIndexType.UP_TYPE_C
			)
		} catch (e: Exception) {
			Timber.e("Error while creating key", e)
			throw IllegalStateException("Error while creating key", e)
		}
		val payloadManager = PayloadCenter.getInstance().payloadManager[PayloadIndexType.EXTERNAL]!!
		try {
			payloadManager.init()
		} catch (e: Exception) {
			Timber.e("Error while initializing payload manager fot EXTERNAL payload", e)
		}
		Timber.i("Add payload data and widget info listener for %s", PayloadIndexType.EXTERNAL)
		val handler: (data: ByteArray) -> Unit = {
			Timber.i(
				"[%s] Received data from %s payload [%d bytes]",
				PayloadIndexType.EXTERNAL,
				it.size
			)
			val responseMessage = it.toString(StandardCharsets.UTF_8)
			Timber.i("Received data: %s", responseMessage)
			coroutineScope.launch {
				receivedDataFlow.emit(responseMessage)
			}
		}
		handlers.add(handler)
		payloadManager.addPayloadDataListener(handler)
	}

	fun send() {
		val message = "Hello from MSDK5"
		val bytes = message.toByteArray()
		Timber.i(
			"Sending message '%s'[%d bytes] to the payload [%s].",
			message, bytes.size, PayloadIndexType.EXTERNAL
		)
		onboardSdkSendDataKey.set(bytes,
			onSuccess = {
				Timber.i("Message '%s' sent successfully.", message)
				coroutineScope.launch {
					receivedDataFlow.emit("Send message: '${message}'")
				}
			},
			onFailure = { error ->
				Timber.e("Message '%s' sending failed: %s", message, error)
			}
		)
	}
}