import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    id("app.cash.sqldelight")
}

sqldelight {
    databases {
        create("CinePropDatabase") {
            packageName.set("org.mosyagin.project.db")
        }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Voyager for navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenModel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Coroutines
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")

            // SQLDelight
            implementation("app.cash.sqldelight:runtime:${libs.versions.sqldelight.get()}")
            implementation("app.cash.sqldelight:coroutines-extensions:${libs.versions.sqldelight.get()}")
            implementation(compose.materialIconsExtended)
            
            // Datetime
            implementation(libs.kotlinx.datetime)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.uiToolingPreview)
            implementation("app.cash.sqldelight:android-driver:${libs.versions.sqldelight.get()}")
            implementation(libs.pdfbox.android)
            implementation(libs.koin.android)
        }
        
        iosMain.dependencies {
            implementation("app.cash.sqldelight:native-driver:${libs.versions.sqldelight.get()}")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinx.coroutines.get()}")
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("app.cash.sqldelight:sqlite-driver:${libs.versions.sqldelight.get()}")
            // Добавляем PDFBox для Desktop
            implementation("org.apache.pdfbox:pdfbox:2.0.30")
        }
        
        jvmTest.dependencies {
            implementation("app.cash.sqldelight:sqlite-driver:${libs.versions.sqldelight.get()}")
        }

        androidUnitTest.dependencies {
            implementation("androidx.test:core:1.6.1")
            implementation("org.robolectric:robolectric:4.12.2")
            implementation(libs.koin.test)
        }
    }
}

android {
    namespace = "org.mosyagin.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.mosyagin.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        // ИСПРАВЛЕНО: Полное имя класса с учетом пакета
        mainClass = "org.mosyagin.project.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)

            packageName = "CineApp"
            packageVersion = "1.0.0"
            description = "CineApp Professional Production Tool"

            macOS {
                bundleID = "org.mosyagin.cineapp"
            }

            windows {
                packageName = "CineApp"
                shortcut = true
                menu = true
                upgradeUuid = "80f86641-3b7c-474c-b9b5-6f9a0c0f993d"
                // iconFile.set(project.file("launcher_icons/icon.ico"))
            }
        }
    }
}
