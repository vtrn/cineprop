package org.mosyagin.project.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createDriver
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.parser.KppParser
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.repository.*
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
    // Обновляем фабрику для SceneDetailScreenModel с учетом новых параметров
    factory { (sceneUserDataId: Long, scriptFileId: Long) -> 
        SceneDetailScreenModel(get(), sceneUserDataId, scriptFileId) 
    }
    factory { ScriptViewModel(get()) }
}

/**
 * Общий модуль приложения.
 */
val appModule = module {
    single { ScriptParser() }
    single { KppParser(get(), get()) }
    
    single<ProjectRepository> { ProjectRepositoryImpl(get()) }
    single<SceneRepository> { SceneRepositoryImpl(get()) }
    single<ScriptRepository> { ScriptRepositoryImpl(get(), get()) }
    single<KppRepository> { KppRepositoryImpl(get()) }
    single<ShiftRepository> { ShiftRepositoryImpl(get()) }
}
