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
import org.mosyagin.project.crypto.*

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
    factory { ProjectListScreenModel(get(), get(), get(), get()) }
    factory { AuthScreenModel(get()) }
    
    factory { (sceneUserDataId: String, scriptFileId: String) -> 
        SceneDetailScreenModel(get(), sceneUserDataId, scriptFileId)
    }
    factory { (sceneUserDataId: String) -> 
        SceneDiffViewModel(sceneUserDataId = sceneUserDataId, repository = get(), parser = get())
    }
    factory { ScriptViewModel(get()) }
    
    factory { (projectId: String, seriesNumber: Int) -> 
        ScriptVersionViewModel(get(), projectId, seriesNumber) 
    }

    factory { (projectId: String) -> 
        SceneWorkspaceViewModel(projectId, get(), get()) 
    }
    
    factory { (projectId: String) -> 
        TrackerViewModel(projectId, get(), get()) 
    }

    factory { (projectId: String) -> 
        PropWorkspaceViewModel(
            projectId = projectId, 
            sceneRepository = get(),
            projectRepository = get(),
            syncRepository = get(),
            propExporter = get(),
            fileSaver = get()
        )
    }

    factory { (projectId: String) -> 
        PropAssetManagerViewModel(get(), get(), projectId)
    }

    factory { (projectId: String) -> 
        ScriptWorkspaceViewModel(projectId, get())
    }

    factory { (projectId: String) -> 
        KppWorkspaceViewModel(projectId, get())
    }

    factory { (projectId: String) -> 
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
    
    // ScriptUpdateManager (queries, parser, syncRepository, encrypter)
    single { ScriptUpdateManager(get(), get(), get(), get()) }
    
    // 1. Очередь синхронизации
    single<SyncRepository> { SyncRepositoryImpl(get()) }
    
    // 2. Менеджер синхронизации
    single { SyncManager(get(), get(), get()) }
    
    // 3. Шифрование данных (keyProvider, settingsRepository)
    single<DataEncrypter> {
        AesEncrypter(get(), get())
    }
    
    // 4. Инициализируем связь один раз при старте
    single(createdAtStart = true) {
        val repo = get<SyncRepository>()
        val manager = get<SyncManager>()
        repo.setSyncManager(manager)
        "SyncLinkInitialized"
    }
    
    single<ProjectRepository> { ProjectRepositoryImpl(get(), get()) }
    
    // SceneRepository (queries, syncRepository, encrypter)
    single<SceneRepository> { SceneRepositoryImpl(get(), get(), get()) }

    // ScriptRepository (queries, parser, syncRepository, encrypter)
    single<ScriptRepository> { ScriptRepositoryImpl(get(), get(), get(), get()) }

    single<KppRepository> { KppRepositoryImpl(get(), get()) }
    single<ShiftRepository> { ShiftRepositoryImpl(get(), get()) }
    
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    
    // Auth - ДЕЛАЕМ EAGER, чтобы он сразу начал слушать сессию
    single<AuthRepository>(createdAtStart = true) { AuthRepositoryImpl(get()) }
}
