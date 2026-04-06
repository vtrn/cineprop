import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.buildkonfig)
    id("app.cash.sqldelight")
    kotlin("plugin.serialization") version "2.1.0" 
}

// Загрузка секретов из local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

buildkonfig {
    packageName = "org.mosyagin.project"
    
    val supabaseUrl = localProperties.getProperty("SUPABASE_URL") ?: ""
    val supabaseKey = localProperties.getProperty("SUPABASE_KEY") ?: ""

    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(STRING, "SUPABASE_KEY", supabaseKey)
    }
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

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenModel)
            implementation(libs.voyager.transitions)
            implementation(libs.voyager.koin)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.kotlinx.coroutines.get()}")

            implementation("app.cash.sqldelight:runtime:${libs.versions.sqldelight.get()}")
            implementation("app.cash.sqldelight:coroutines-extensions:${libs.versions.sqldelight.get()}")
            
            implementation(compose.materialIconsExtended) 
            
            implementation(libs.kotlinx.datetime)

            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.auth)
            implementation(libs.supabase.storage)
            implementation(libs.supabase.realtime) 
            implementation(libs.ktor.client.core)
            
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.koin.test)
        }
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.uiToolingPreview)
            implementation("app.cash.sqldelight:android-driver:${libs.versions.sqldelight.get()}")
            implementation(libs.pdfbox.android)
            implementation(libs.koin.android)
            
            implementation(libs.poi.ooxml)
            // ИСПОЛЬЗУЕМ OKHTTP ДЛЯ ПОДДЕРЖКИ WEBSOCKETS НА ANDROID
            implementation("io.ktor:ktor-client-okhttp:${libs.versions.ktor.get()}")
        }

        androidUnitTest.dependencies {
            implementation(libs.robolectric)
            implementation(libs.core.ktx)
        }
        
        iosMain.dependencies {
            implementation("app.cash.sqldelight:native-driver:${libs.versions.sqldelight.get()}")
            // iOS движок Darwin поддерживает WebSockets из коробки
            implementation("io.ktor:ktor-client-darwin:${libs.versions.ktor.get()}")
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("app.cash.sqldelight:sqlite-driver:${libs.versions.sqldelight.get()}")
            implementation("org.apache.pdfbox:pdfbox:2.0.30")
            
            implementation(libs.poi.ooxml)
            implementation(libs.ktor.client.cio)
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
        mainClass = "org.mosyagin.project.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "CineApp"
            macOS { bundleID = "org.mosyagin.cineapp" }
        }
    }
}
