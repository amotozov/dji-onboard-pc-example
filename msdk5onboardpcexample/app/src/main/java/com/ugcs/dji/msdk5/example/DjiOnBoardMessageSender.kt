package com.ugcs.dji.msdk5.example

import dji.sdk.keyvalue.value.payload.WidgetType
import dji.sdk.keyvalue.value.payload.WidgetValue
import dji.v5.common.callback.CommonCallbacks.CompletionCallback
import dji.v5.common.error.IDJIError
import dji.v5.manager.aircraft.payload.PayloadCenter
import dji.v5.manager.interfaces.IPayloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.charset.StandardCharsets

class DjiOnBoardMessageSender (
	val coroutineScope: CoroutineScope
) {

	val receivedData = mutableListOf<String>()
	val receivedDataFlow = MutableSharedFlow<String>()
	val connectedPayloadConnection = mutableListOf<IPayloadManager>()
	val handlers = mutableListOf<(data: ByteArray) -> Unit>()

	init {
		val payloadWidgetToPayloadManager = PayloadCenter.getInstance().payloadManager
		if (payloadWidgetToPayloadManager != null) {
			for ((payloadWidgetType, payloadManager) in payloadWidgetToPayloadManager) {
				payloadManager.init()
				Timber.i("Add payload data and widget info listener for %s", payloadWidgetType)
				val handler: (data: ByteArray) -> Unit = {
					Timber.i("[%s] Received data from %s payload [%d bytes]", payloadWidgetType, it.size)
					val responseMessage = it.toString(StandardCharsets.UTF_8)
					Timber.i("Received data: %s", responseMessage)
					coroutineScope.launch {
						receivedDataFlow.emit(responseMessage)
					}
				}
				handlers.add(handler)
				payloadManager.setWidgetValue(WidgetValue(WidgetType.INPUT, 0, 0), object: CompletionCallback {
					override fun onSuccess() {
						Timber.i("Widget callback on success")
					}

					override fun onFailure(error: IDJIError) {
						Timber.i("Widget callback on error", error)
					}

				})
				payloadManager.addPayloadDataListener(handler)
				payloadManager.addPayloadWidgetInfoListener {
					Timber.i("[%s] Widget info: %s", payloadWidgetType, it)
				}
				payloadManager.addPayloadBasicInfoListener {
					Timber.i("[%s] Payload basic info listener: %s", payloadWidgetType, it)
				}
			}
		}
	}

	fun send() {
		val payloadManager = PayloadCenter.getInstance().payloadManager
		payloadManager?.let {
			val message = "Hello from MSDK5"
			val bytes = message.toByteArray()
			for ((k, v) in payloadManager) {
				Timber.i(
					"Sending message '%s'[%d bytes] to the payload [%s].",
					message, bytes.size, k
				)
				v.sendDataToPayload(bytes, object: CompletionCallback {
					override fun onSuccess() {
						Timber.i("Message '%s' sent successfully.", message,)
						coroutineScope.launch {
							receivedDataFlow.emit("Send message: '${message}'",)
						}
					}

					override fun onFailure(error: IDJIError) {
						Timber.e("Message '%s' sending failed: %s", message, error)
					}
				})
			}
		}
	}
}