import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(libs.koin.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.tika)
}

compose.desktop {
    application {
        mainClass = "io.github.numq.grokviewer.application.ApplicationKt"

        nativeDistributions {
            modules("java.instrument", "java.sql", "jdk.unsupported")
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "GrokViewer"
            packageVersion = "1.0.0"
            windows {
                iconFile.set(project.file("src/main/resources/drawable/icon.png"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/drawable/icon.png"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/drawable/icon.png"))
            }
        }
    }
}