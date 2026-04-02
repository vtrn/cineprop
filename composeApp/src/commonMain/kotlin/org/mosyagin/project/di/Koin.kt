package org.mosyagin.project.di

import kotlinx.coroutines.NonCancellable.get
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.mosyagin.project.db.CinePropDatabase
import org.mosyagin.project.db.createDriver
import org.mosyagin.project.db.ProjectListScreenModel
import org.mosyagin.project.parser.KppParser
import org.mosyagin.project.parser.ScriptParser
import org.mosyagin.project.parser.update.ScriptUpdateManager
import org.mosyagin.project.repository.*
import org.mosyagin.project.ui.screens.*
import kotlin.coroutines.EmptyCoroutineContext.get

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
    factory { (sceneUserDataId: Long, scriptFileId: Long) -> 
        SceneDetailScreenModel(get(), get(), sceneUserDataId, scriptFileId)
    }
    factory { (sceneUserDataId: Long) -> 
        SceneDiffViewModel(sceneUserDataId = sceneUserDataId, repository = get(), parser = get())
    }
    factory { ScriptViewModel(get()) }
    
    // Регистрация для управления версиями
    factory { (projectId: Long, seriesNumber: Int) -> 
        ScriptVersionViewModel(get(), projectId, seriesNumber) 
    }

    // Task #48: Workspace для Desktop
    factory { (projectId: Long) -> 
        SceneWorkspaceViewModel(projectId, get()) 
    }
    
    // Tracker Workspace для Desktop
    factory { (projectId: Long) -> 
        TrackerViewModel(projectId, get(), get()) 
    }

    // Prop Workspace для Desktop
    factory { (projectId: Long) -> 
        PropWorkspaceViewModel(projectId, get())
    }

    // Asset Manager для Desktop
    factory { (projectId: Long) -> 
        PropAssetManagerViewModel(get(), projectId)
    }

    // Script Workspace для Desktop
    factory { (projectId: Long) -> 
        ScriptWorkspaceViewModel(projectId, get())
    }

    // Kpp Workspace для Desktop
    factory { (projectId: Long) -> 
        KppWorkspaceViewModel(projectId, get())
    }

    // Character Workspace для Desktop
    factory { (projectId: Long) -> 
        CharacterWorkspaceViewModel(projectId, get())
    }
    
    // Settings
    factory { SettingsViewModel(get()) }
}

/**
 * Общий модуль приложения.
 */
val appModule = module {
    single { ScriptParser() }
    single { KppParser(get(), get()) }
    single { ScriptUpdateManager(get(), get()) }
    
    single<ProjectRepository> { ProjectRepositoryImpl(get()) }
    single<SceneRepository> { SceneRepositoryImpl(get()) }
    single<ScriptRepository> { ScriptRepositoryImpl(get(), get()) }
    single<KppRepository> { KppRepositoryImpl(get()) }
    single<ShiftRepository> { ShiftRepositoryImpl(get()) }
    
    // Явно указываем интерфейс для SettingsRepository
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}
