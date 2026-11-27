import org.gradle.process.ExecOperations
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

val releaseAppVersion: String = providers.gradleProperty("releaseAppVersion").get()
val releaseAppRevision = SimpleDateFormat("yy.M.d").format(Date()) ?: "0"
val appInfoDescription = "ONI PO Translate Helper"
val appInfoVendor = "DolphinWing"
val appInfoCopyright = "Copyright (c) 2026 DolphinWing"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.plugin.compose)
    alias(libs.plugins.versions.plugin)
}

val projectGroup: String = providers.gradleProperty("projectGroup").get()
group = projectGroup
version = releaseAppVersion

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.components.resources)

    // https://github.com/houbb/opencc4j
    implementation(libs.opencc4j)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "MainKt"

        // https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            // Required for JDK HttpClient (GeminiClient) and Unsafe references in bundled jlink runtime
            modules("java.net.http", "jdk.unsupported")

            packageName = "OniTranslator"
            packageVersion = releaseAppVersion
            version = releaseAppVersion
            description = appInfoDescription
            vendor = appInfoVendor
            copyright = appInfoCopyright

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

abstract class PackageInnoSetupTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val appVersion: Property<String>

    @get:Input
    abstract val appPublisher: Property<String>

    @get:Input
    abstract val appDescription: Property<String>

    @get:Input
    abstract val appCopyright: Property<String>

    @get:Input
    abstract val isccPathOverride: Property<String>

    @get:InputDirectory
    abstract val appImageDir: DirectoryProperty

    @get:InputFile
    abstract val issFile: RegularFileProperty

    @get:InputFile
    abstract val iconFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun buildInstaller() {
        val osName = System.getProperty("os.name") ?: ""
        if (!osName.startsWith("Windows", ignoreCase = true)) {
            throw GradleException("packageInnoSetup is only supported on Windows (current OS: $osName).")
        }

        val srcDir = appImageDir.get().asFile
        val exeFile = File(srcDir, "OniTranslator.exe")
        if (!exeFile.isFile) {
            throw GradleException("Packaged app executable not found: ${exeFile.absolutePath}")
        }

        val runtimeReleaseFile = File(srcDir, "runtime/release")
        if (!runtimeReleaseFile.isFile) {
            throw GradleException("Bundled runtime release file not found: ${runtimeReleaseFile.absolutePath}")
        }
        val releaseContent = runtimeReleaseFile.readText()
        if (!releaseContent.contains("java.net.http")) {
            throw GradleException(
                "Bundled runtime is missing required module 'java.net.http' in ${runtimeReleaseFile.absolutePath}:\n$releaseContent"
            )
        }

        val iscc = locateIscc(isccPathOverride.orNull?.trim().orEmpty())
        val outDir = outputDir.get().asFile
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw GradleException("Failed to create installer output directory: ${outDir.absolutePath}")
        }

        val ver = appVersion.get()
        val publisher = appPublisher.get()
        val desc = appDescription.get()
        val copy = appCopyright.get()
        val iss = issFile.get().asFile
        val icon = iconFile.get().asFile
        val hasZhTwLang = File(iscc.parentFile, "Languages/ChineseTraditional.isl").isFile

        val args = buildList {
            add(iscc.absolutePath)
            add("/Qp")
            add("/DAppVersion=$ver")
            add("/DAppPublisher=$publisher")
            add("/DAppDescription=$desc")
            add("/DAppCopyright=$copy")
            add("/DSourceDir=${srcDir.absolutePath}")
            add("/DOutputDir=${outDir.absolutePath}")
            add("/DIconFile=${icon.absolutePath}")
            if (hasZhTwLang) {
                add("/DHasZhTwLang")
            }
            add(iss.absolutePath)
        }

        logger.lifecycle("Compiling Inno Setup installer using ${iscc.absolutePath} (zh-TW=${hasZhTwLang})")
        execOperations.exec {
            commandLine(args)
        }

        val installerFile = File(outDir, "OniTranslator-Setup-$ver.exe")
        if (!installerFile.isFile) {
            throw GradleException("Expected installer was not produced: ${installerFile.absolutePath}")
        }

        val sizeMb = String.format(Locale.US, "%.2f", installerFile.length() / (1024.0 * 1024.0))
        val digest = MessageDigest.getInstance("SHA-256")
        installerFile.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val sha256 = digest.digest().joinToString("") { "%02x".format(it) }

        logger.lifecycle("Installer ready: ${installerFile.absolutePath}")
        logger.lifecycle("Size: $sizeMb MB | SHA-256: $sha256")
    }

    private fun locateIscc(overridePath: String): File {
        if (overridePath.isNotEmpty()) {
            val candidate = File(overridePath)
            if (candidate.isFile) return candidate
            throw GradleException("Specified ISCC.exe not found at: $overridePath")
        }

        val pathEnv = System.getenv("PATH").orEmpty()
        for (dir in pathEnv.split(File.pathSeparator)) {
            if (dir.isBlank()) continue
            val candidate = File(dir.trim(), "ISCC.exe")
            if (candidate.isFile) return candidate
        }

        val defaultDirs = listOfNotNull(
            System.getenv("ProgramFiles(x86)")?.let { File(it, "Inno Setup 6/ISCC.exe") },
            System.getenv("ProgramFiles")?.let { File(it, "Inno Setup 6/ISCC.exe") },
            System.getenv("LOCALAPPDATA")?.let { File(it, "Programs/Inno Setup 6/ISCC.exe") },
        )
        defaultDirs.firstOrNull { it.isFile }?.let { return it }

        throw GradleException(
            "Unable to locate Inno Setup 6 ISCC.exe. Pass -PisccPath=... or set ISCC_PATH environment variable."
        )
    }
}

tasks.register<PackageInnoSetupTask>("packageInnoSetup") {
    group = "distribution"
    description = "Builds a Windows Inno Setup installer from the createDistributable app image."
    dependsOn("createDistributable")

    appVersion.set(releaseAppVersion)
    appPublisher.set(appInfoVendor)
    appDescription.set(appInfoDescription)
    appCopyright.set(appInfoCopyright)
    isccPathOverride.set(
        providers.gradleProperty("isccPath")
            .orElse(providers.environmentVariable("ISCC_PATH"))
            .orElse("")
    )
    appImageDir.set(layout.buildDirectory.dir("compose/binaries/main/app/OniTranslator"))
    issFile.set(layout.projectDirectory.file("installer.iss"))
    iconFile.set(layout.projectDirectory.file("src/main/resources/nisbet_ponder.ico"))
    outputDir.set(layout.buildDirectory.dir("installer"))
}
