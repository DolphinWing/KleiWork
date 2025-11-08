import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.text.SimpleDateFormat
import java.util.Date

val releaseAppVersion = "3.1.1"
val releaseAppRevision = SimpleDateFormat("yy.M.d").format(Date()) ?: "0"

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
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
    implementation(compose.materialIconsExtended)
    implementation(compose.preview)
    implementation(compose.uiTooling)

    // https://github.com/houbb/opencc4j
    implementation("com.github.houbb:opencc4j:1.8.1")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)

            packageName = "OniTranslator"
            packageVersion = releaseAppVersion
            version = releaseAppVersion
            description = "ONI PO Translate Helper"
            vendor = "DolphinWing"

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

        args += listOf("v=$releaseAppVersion")
    }
}
