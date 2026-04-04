package org.mosyagin.project.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
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

/**
 * Ожидаемые модули от платформ (Android/JVM)
 */
expect val platformModule: Module

/**
 * Инициализация Koin.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(appModule, databaseModule, screenModelModule, platformModule)
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
    
    factory { (projectId: Long, seriesNumber: Int) -> 
        ScriptVersionViewModel(get(), projectId, seriesNumber) 
    }

    factory { (projectId: Long) -> 
        SceneWorkspaceViewModel(projectId, get()) 
    }
    
    factory { (projectId: Long) -> 
        TrackerViewModel(projectId, get(), get()) 
    }

    // Prop Workspace: передаем все 5 параметров (projectId + 4 зависимости из Koin)
    factory { (projectId: Long) -> 
        PropWorkspaceViewModel(
            projectId = projectId, 
            sceneRepository = get(),
            projectRepository = get(),
            propExporter = get(),
            fileSaver = get()
        )
    }

    factory { (projectId: Long) -> 
        PropAssetManagerViewModel(get(), projectId)
    }

    factory { (projectId: Long) -> 
        ScriptWorkspaceViewModel(projectId, get())
    }

    factory { (projectId: Long) -> 
        KppWorkspaceViewModel(projectId, get())
    }

    factory { (projectId: Long) -> 
        CharacterWorkspaceViewModel(projectId, get())
    }
    
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
    
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}
