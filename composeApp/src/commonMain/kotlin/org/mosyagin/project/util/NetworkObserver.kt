package org.mosyagin.project.util

import kotlinx.coroutines.flow.StateFlow

interface NetworkObserver {
    val isOnline: StateFlow<Boolean>
}
