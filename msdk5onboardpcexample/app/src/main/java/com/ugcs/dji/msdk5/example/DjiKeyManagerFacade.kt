package com.ugcs.dji.msdk5.example

import dji.sdk.keyvalue.key.DJIKeyInfo
import dji.sdk.keyvalue.key.KeyTools
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.interfaces.IKeyManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Encapsulates DJI Key Manager for use with coroutines.
 */
interface KeyManagerFacade {
    fun <T> flow(key: DJIKeyInfo<T>): Flow<T?>

    companion object {
        fun create(keyManager: IKeyManager = KeyManager.getInstance()): KeyManagerFacade {
            return object : KeyManagerFacade {
                override fun <T> flow(key: DJIKeyInfo<T>): Flow<T?> = callbackFlow {
                    val djiKey = KeyTools.createKey(key)
                    val cachedValue = keyManager.getValue(djiKey)
                    if (cachedValue != null) {
                        send(cachedValue)
                    }
                    val listenHolder = Any()
                    keyManager.listen(djiKey, listenHolder) { _, newValue ->
                        trySend(newValue)
                    }
                    val newValue = keyManager.getValue(djiKey)
                    if (cachedValue != newValue) {
                        send(newValue)
                    }
                    awaitClose { keyManager.cancelListen(djiKey, listenHolder) }
                }
            }
        }
    }
}


fun IDJIError?.asException(message: String? = null): Throwable {
    fun buildErrorMessage(djiError: IDJIError) = with(djiError) {
        val stringBuilder = StringBuilder()
        if (message != null) {
            stringBuilder.append("$message ")
        }
        stringBuilder.append("errorType: ${errorType()}, errorCode: ${errorCode()}, description: ${description()}")
        stringBuilder.toString()
    }

    return if (this == null) {
        DjiUnknownException()
    } else {
        DJIErrorException(buildErrorMessage(this))
    }
}

class DJIErrorException(message: String) :
    Exception(message)

class DjiUnknownException : Exception("DJI MSDK operation failed due to an unknown error.")
