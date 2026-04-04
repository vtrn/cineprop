package org.mosyagin.project.export

import org.koin.dsl.module

/**
 * Платформенный модуль для Desktop (Экспорт)
 */
val desktopExportModule = module {
    single<PropExporter> { DesktopPropExporter() }
    single<FileSaver> { DesktopFileSaver() }
}
