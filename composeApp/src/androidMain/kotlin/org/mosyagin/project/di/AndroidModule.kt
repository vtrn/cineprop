package org.mosyagin.project.di

import org.koin.dsl.module
import org.mosyagin.project.export.*

val androidModule = module {
    single<PropExporter> { AndroidPropExporter() }
    single<FileSaver> { AndroidFileSaver(get()) }
}
