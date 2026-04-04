package org.mosyagin.project.di

import org.koin.dsl.module
import org.mosyagin.project.export.*

val desktopModule = module {
    single<PropExporter> { DesktopPropExporter() }
    single<FileSaver> { DesktopFileSaver() }
}
