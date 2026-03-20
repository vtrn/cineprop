package org.mosyagin.project.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createDriver
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.ui.screens.SceneDetailScreenModel
import org.mosyagin.project.ui.screens.ScriptViewModel

/**
 * Инициализация Koin.
 * Вызывается при запуске приложения на каждой платформе.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(appModule, databaseModule, screenModelModule)
    }

/**
 * Модуль базы данных.
 */
val databaseModule = module {
    single {
        val driver = createDriver()
        val database = CinePropDatabase(driver)
        database.databaseQueries
    }
}

/**
 * Модуль для ScreenModels (ViewModels).
 */
val screenModelModule = module {
    factory { ProjectListScreenModel(get()) }
    // Для моделей с параметрами используем factory с параметрами
    factory { (sceneId: Long) -> SceneDetailScreenModel(get(), sceneId) }
    factory { ScriptViewModel(get()) }
}

/**
 * Общий модуль приложения.
 */
val appModule = module {
    // В будущем здесь будут репозитории
}
