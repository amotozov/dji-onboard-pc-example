package com.ugcs.dji.msdk4.example.dji

import dji.keysdk.DJIKey
import dji.keysdk.KeyManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface KeyManagerFacade {
	fun <T> flow(key: DJIKey): Flow<T?>

	@Suppress("UNCHECKED_CAST")
	companion object {
		fun create(keyManager: KeyManager = KeyManager.getInstance()): KeyManagerFacade {
			return object: KeyManagerFacade {
				override fun <T> flow(key: DJIKey): Flow<T?> = callbackFlow {
					val cachedValue = keyManager.getValue(key)
					if (cachedValue != null)
						send(cachedValue as T)

					val listener: (Any?, Any?) -> Unit = { _, newValue ->
						trySend(newValue as T)
					}
					keyManager.addListener(key, listener)
					val newValue = keyManager.getValue(key)
					if (cachedValue != newValue) {
						send(newValue as T)
					}
					awaitClose { keyManager.removeListener(listener)}
				}

			}
		}
	}
}