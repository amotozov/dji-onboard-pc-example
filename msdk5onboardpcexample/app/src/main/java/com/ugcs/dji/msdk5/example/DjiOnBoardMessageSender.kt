package com.ugcs.dji.msdk5.example

import dji.v5.common.callback.CommonCallbacks.CompletionCallback
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.aircraft.payload.PayloadIndexType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.charset.StandardCharsets

class DjiOnBoardMessageSender(
	val coroutineScope: CoroutineScope
) {

	val receivedDataFlow = MutableSharedFlow<String>()

	init {
		val payloadManager = PayloadCenter.getInstance().payloadManager[PayloadIndexType.EXTERNAL]!!
		try {
			payloadManager.init()
		} catch (e: Exception) {
			Timber.e("Error while initializing payload manager fot EXTERNAL payload", e)
		}
		Timber.i("Add payload data and widget info listener for %s", PayloadIndexType.EXTERNAL)
		PayloadCenter.getInstance().payloadManager[PayloadIndexType.EXTERNAL]!!.addPayloadDataListener {
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
	}

	fun send() {
		val payloadManager = PayloadCenter.getInstance().payloadManager[PayloadIndexType.EXTERNAL]!!
		val message = "Hello from MSDK5"
		val bytes = message.toByteArray()
		Timber.i(
			"Sending message '%s'[%d bytes] to the payload [%s].",
			message, bytes.size, PayloadIndexType.EXTERNAL
		)
		payloadManager.sendDataToPayload(bytes, object : CompletionCallback {
			override fun onSuccess() {
				Timber.i("Message '%s' sent successfully.", message)
				coroutineScope.launch {
					receivedDataFlow.emit("Send message: '${message}'")
				}
			}

			override fun onFailure(error: IDJIError) {
				Timber.e("Message '%s' sending failed: %s", message, error)
			}
		})
	}
}