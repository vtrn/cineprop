package org.mosyagin.project


import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.mosyagin.project.ui.screens.ProjectListScreen

@Composable
fun App() {
    MaterialTheme {
        // Мы передаем стартовый экран в Navigator
        Navigator(ProjectListScreen()) { navigator ->
            // SlideTransition не только дает анимацию,
            // но и правильно управляет стеком экранов
            SlideTransition(navigator)
        }
    }
}