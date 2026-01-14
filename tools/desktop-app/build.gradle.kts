import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.text.SimpleDateFormat
import java.util.Date

val releaseAppVersion: String by project
val releaseAppRevision = SimpleDateFormat("yy.M.d").format(Date()) ?: "0"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.versions.plugin)
}

group = "dolphin.desktop.apps"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    // implementation(compose.material)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.preview)
    implementation(compose.uiTooling)
    implementation(compose.components.resources)

    // https://github.com/houbb/opencc4j
    implementation(libs.opencc4j)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        // https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)

            packageName = "OniTranslator"
            packageVersion = releaseAppVersion
            version = releaseAppVersion
            description = "ONI PO Translate Helper"
            vendor = "DolphinWing"
            copyright = "Copyright (c) 2025 DolphinWing"

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))

            windows {
                dirChooser = true
                packageVersion = releaseAppVersion
                msiPackageVersion = releaseAppVersion
                // exePackageVersion = releaseAppVersion
                upgradeUuid = "f33ea1be-e738-43e0-9918-9360b0620fc0"
                // https://slack-chats.kotlinlang.org/t/26915548/i-m-trying-to-set-the-icon-for-a-desktop-application-in-kotl
                iconFile.set(File("src/main/resources/nisbet_ponder.ico"))
            }

            linux {
                debMaintainer = "dolphinwing74+github@gmail.com"
                packageVersion = releaseAppVersion
                debPackageVersion = releaseAppVersion
                // rpmPackageVersion = releaseAppVersion
                appRelease = releaseAppRevision
            }
        }

        buildTypes.release.proguard {
            obfuscate.set(true)
            optimize.set(true)
        }

        args += listOf("v=$releaseAppVersion")
    }
}

compose.resources {
    // https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }

    gradleReleaseChannel = "current"
}

fun isNonStable(version: String): Boolean {
    val uppercaseVersion = version.uppercase()
    return listOf("ALPHA", "BETA", "RC", "SNAPSHOT", "M", "DEV").any {
        uppercaseVersion.contains(it)
    }
}
