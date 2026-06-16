import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.ui)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodelCompose)
                implementation(libs.androidx.lifecycle.runtimeCompose)
                
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.phosphor.icons)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.andyechc.downgram.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Downgram"
            packageVersion = "1.0.0"
            
            // Configuración de iconos para las distribuciones nativas
            linux {
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/ic_launcher_foreground.xml")) // El plugin intentará rasterizar o puedes proveer un .png
            }
            windows {
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/icon-windows.ico"))
            }
            macOS {
                iconFile.set(project.file("src/jvmMain/composeResources/drawable/icon-mac.icns"))
            }
            
            // Incluimos los binarios de python empaquetados en los recursos
            appResourcesRootDir.set(project.file("backend-bin"))
        }
    }
}
