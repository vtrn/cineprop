package org.mosyagin.project


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.mosyagin.project.ui.screens.ProjectListScreen
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createDriver
import org.mosyagin.project.ui.theme.CinePropTheme

@Composable
fun App() {
    val driver = createDriver()
    val database = CinePropDatabase(driver)
    val queries = database.databaseQueries

    // ВАЖНО: Тема должна быть самым верхним уровнем
    CinePropTheme {
        // Surface дает приложению "холст", закрашивая фон в цвет темы
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Navigator(ProjectListScreen(queries)) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
