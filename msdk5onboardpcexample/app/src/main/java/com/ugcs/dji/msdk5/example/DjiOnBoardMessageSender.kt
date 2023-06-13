package com.ugcs.dji.msdk5.example

import dji.sdk.keyvalue.value.payload.WidgetType
import dji.sdk.keyvalue.value.payload.WidgetValue
import dji.v5.common.callback.CommonCallbacks.CompletionCallback
import dji.v5.common.error.IDJIError
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

	init {
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
		payloadManager.setWidgetValue(
			WidgetValue(WidgetType.INPUT, 0, 0),
			object : CompletionCallback {
				override fun onSuccess() {
					Timber.i("Widget callback on success")
				}

				override fun onFailure(error: IDJIError) {
					Timber.i("Widget callback on error", error)
				}

			})
		payloadManager.addPayloadDataListener(handler)
		payloadManager.addPayloadWidgetInfoListener {
			Timber.i("[%s] Widget info: %s", PayloadIndexType.EXTERNAL, it)
		}
		payloadManager.addPayloadBasicInfoListener {
			Timber.i("[%s] Payload basic info listener: %s", PayloadIndexType.EXTERNAL, it)
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