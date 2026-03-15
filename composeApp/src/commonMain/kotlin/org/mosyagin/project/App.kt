package org.mosyagin.project


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import org.mosyagin.project.ui.screens.ProjectListScreen
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createDriver
import org.mosyagin.project.ui.theme.CinePropTheme

@Composable
fun App() {
    // ЗАПОМИНАЕМ базу, чтобы она не пересоздавалась
    val queries = remember {
        val driver = createDriver()
        val database = CinePropDatabase(driver)
        database.databaseQueries
    }

    val initialScreen = remember(queries) { ProjectListScreen(queries) }

    CinePropTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Navigator(initialScreen) { navigator ->
                SlideTransition(navigator)
            }
        }
    }
}
