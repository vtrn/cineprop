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
        modules(appModule, databaseModule, screenModelModule, platformModule, supabaseModule)
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
    // Добавлен get() для SyncManager
    factory { ProjectListScreenModel(get(), get(), get()) }
    
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
        SceneWorkspaceViewModel(projectId, get(), get()) 
    }
    
    factory { (projectId: Long) -> 
        TrackerViewModel(projectId, get(), get()) 
    }

    factory { (projectId: Long) -> 
        PropWorkspaceViewModel(
            projectId = projectId, 
            sceneRepository = get(),
            projectRepository = get(),
            syncRepository = get(),
            propExporter = get(),
            fileSaver = get()
        )
    }

    factory { (projectId: Long) -> 
        PropAssetManagerViewModel(get(), get(), projectId)
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
    
    // ScriptUpdateManager теперь требует SyncRepository
    single { ScriptUpdateManager(get(), get(), get()) }
    
    // 1. Очередь синхронизации
    single<SyncRepository> { SyncRepositoryImpl(get()) }
    
    // 2. Менеджер синхронизации
    single { SyncManager(get(), get(), get()) }
    
    // 3. Инициализируем связь один раз при старте
    single(createdAtStart = true) {
        val repo = get<SyncRepository>()
        val manager = get<SyncManager>()
        repo.setSyncManager(manager)
        "SyncLinkInitialized"
    }
    
    single<ProjectRepository> { ProjectRepositoryImpl(get(), get()) }
    single<SceneRepository> { SceneRepositoryImpl(get(), get()) }

    single<ScriptRepository> { ScriptRepositoryImpl(get(), get(), get()) }
    single<KppRepository> { KppRepositoryImpl(get(), get()) }
    single<ShiftRepository> { ShiftRepositoryImpl(get(), get()) }
    
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}
