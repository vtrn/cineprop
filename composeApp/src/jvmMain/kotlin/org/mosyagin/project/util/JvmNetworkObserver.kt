package org.mosyagin.project.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

class JvmNetworkObserver : NetworkObserver {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                _isOnline.value = checkConnection()
                delay(5000) // Проверка каждые 5 секунд
            }
        }
    }

    private fun checkConnection(): Boolean {
        return try {
            val socket = Socket()
            // Пытаемся подключиться к DNS Google или любому надежному адресу
            socket.connect(InetSocketAddress("8.8.8.8", 53), 2000)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
