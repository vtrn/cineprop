package org.mosyagin.project

import androidx.compose.ui.window.ComposeUIViewController
import org.mosyagin.project.di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}
