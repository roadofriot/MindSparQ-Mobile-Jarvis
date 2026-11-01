package com.jarves.mh.runtime

import android.content.Context
import android.system.Os
import com.jarves.mh.R
import com.jarves.mh.model.DevStack
import com.jarves.mh.ui.localizedLabel
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Handles installation, verification, and removal of optional development stacks
 * (Python, Android SDK, Flutter, Go, Rust, PHP/Composer, C++) inside Ubuntu PRoot.
 */
internal class RuntimeStackInstaller(
    private val installer: RuntimeInstaller,
    private val context: Context,
    private val rootfs: File,
    private val downloads: File,
    private val devStacksFile: File,
) {
    private fun str(@androidx.annotation.StringRes id: Int, vararg args: Any): String =
        installer.str(id, *args)

    private fun File.readTextOrNull(): String? = runCatching { readText().trim() }.getOrNull()

    suspend fun ensureStackInstalled(
        stack: DevStack,
        onProgress: suspend (RuntimeInstallProgress) -> Unit,
    ) {
        val runtime = installer.installedRuntime()
        if (isStackInstalled(stack) && stack != DevStack.ANDROID) return
        applyStack(runtime.proot, stack, 0.05f, 0.95f, onProgress)
        onProgress(RuntimeInstallProgress(str(R.string.rt_install_stack_ready, stack.localizedLabel(context)), 1f))
    }

    suspend fun removeStack(
        stack: DevStack,
        onProgress: suspend (RuntimeInstallProgress) -> Unit,
    ) {
        require(stack != DevStack.WEB) { str(R.string.rt_install_web_not_removable) }
        val runtime = installer.installedRuntime()
        if (!isStackInstalled(stack)) return

        onProgress(RuntimeInstallProgress(str(R.string.rt_install_removing_stack, stack.localizedLabel(context)), 0.1f, indeterminate = true))
        when (stack) {
            DevStack.WEB -> Unit
            DevStack.PYTHON -> removePythonStack()
            DevStack.ANDROID -> removeAndroidStack()
            DevStack.CPP -> installer.aptRemove(
                runtime.proot,
                listOf("build-essential", "gcc", "g++", "make", "cmake", "gdb"),
                0.45f,
                onProgress,
            )
            DevStack.PHP -> {
                installer.aptRemove(
                    runtime.proot,
                    listOf("php-cli", "php-mbstring", "php-xml", "php-curl", "php-zip"),
                    0.45f,
                    onProgress,
                )
                removePath(File(rootfs, "usr/local/bin/composer"))
                removePath(File(rootfs, "root/.cache/composer"))
                removePath(File(rootfs, "root/.composer"))
            }
            DevStack.GOLANG -> {
                installer.aptRemove(runtime.proot, listOf("golang-go"), 0.45f, onProgress)
            }
            DevStack.RUST -> {
                installer.aptRemove(runtime.proot, listOf("rustc", "cargo"), 0.45f, onProgress)
            }
            DevStack.FLUTTER -> {
                removePath(File(rootfs, "opt/flutter"))
                removePath(File(rootfs, "usr/local/bin/flutter"))
                removePath(File(rootfs, "usr/local/bin/dart"))
            }
        }

        writeDevStackState(readDevStackState().apply { put(stack.name, false) })
        onProgress(RuntimeInstallProgress(str(R.string.rt_install_stack_removed, stack.localizedLabel(context)), 1f))
    }

    private fun removePythonStack() {
        listOf(
            ".pocket-python-tools-version",
            "usr/bin/python3",
            "usr/bin/python3.8",
            "usr/bin/pip",
            "usr/bin/pip3",
            "usr/lib/aarch64-linux-gnu/libpython3.8.so.1",
            "usr/lib/aarch64-linux-gnu/libpython3.8.so.1.0",
            "usr/lib/python3",
            "usr/lib/python3.8",
            "usr/local/lib/python3.8",
            "usr/share/python3",
            "usr/share/python-wheels",
            "root/.cache/pip",
        ).forEach { removePath(File(rootfs, it)) }
    }

    private fun removeAndroidStack() {
        listOf(
            "root/android-sdk",
            "root/maven",
            "root/.pocket-android-tools-version",
            "root/.gradle/caches",
            "root/.gradle/daemon",
            "root/.gradle/native",
            "root/.gradle/notifications",
            "root/.gradle/wrapper/dists",
            "root/.gradle/init.d/pocketdev-android.gradle",
            "opt/gradle",
            "opt/jdk-17.0.20.1+1",
            "usr/local/bin/jar",
            "usr/local/bin/jarsigner",
            "usr/local/bin/java",
            "usr/local/bin/javac",
            "usr/local/bin/javadoc",
            "usr/local/bin/keytool",
        ).forEach { removePath(File(rootfs, it)) }
        removeAndroidGradleProperty()
    }

    private fun removeAndroidGradleProperty() {
        val properties = File(rootfs, "root/.gradle/gradle.properties")
        if (!properties.isFile) return
        val remaining = properties.readLines().filterNot(::isManagedAndroidGradleProperty)
        if (remaining.isEmpty()) {
            properties.delete()
        } else {
            properties.writeText(remaining.joinToString("\n").trimEnd() + "\n")
        }
    }

    private fun removePath(file: File) {
        if (file.isDirectory && !java.nio.file.Files.isSymbolicLink(file.toPath())) {
            check(file.deleteRecursively()) { "Could not remove ${file.name}" }
        } else if (file.exists() || java.nio.file.Files.isSymbolicLink(file.toPath())) {
            check(file.delete()) { "Could not remove ${file.name}" }
        }
    }

    internal suspend fun applyStack(
        proot: File,
        stack: DevStack,
        from: Float,
        to: Float,
        onProgress: suspend (RuntimeInstallProgress) -> Unit,
    ) {
        var verified = true
        when (stack) {
            DevStack.WEB -> {
                onProgress(RuntimeInstallProgress(str(R.string.rt_install_checking_node), from))
                installer.verifyGuest(proot, "node --version && npm --version", str(R.string.rt_install_verify_failed, "Node.js"))
            }
            DevStack.PYTHON -> {
                installer.installRuntimeOverlay(RuntimeInstaller.PYTHON_BUNDLE, str(R.string.rt_install_python), from, to, onProgress)
                runCatching {
                    installer.verifyGuest(proot, "python3 --version && pip3 --version", str(R.string.rt_install_verify_failed, "Python"))
                }.onFailure { error ->
                    verified = false
                    onProgress(
                        RuntimeInstallProgress(
                            str(R.string.rt_install_python_verify_failed, error.message?.take(120).orEmpty()),
                            to,
                        ),
                    )
                }
            }
            DevStack.ANDROID -> {
                installAndroidToolchain(proot, from, to, onProgress)
            }
            DevStack.CPP -> {
                installer.aptInstall(
                    proot,
                    listOf("build-essential", "cmake", "gdb"),
                    str(R.string.rt_install_cpp),
                    from,
                    onProgress,
                )
                installer.verifyGuest(
                    proot,
                    "gcc --version && g++ --version && make --version && cmake --version",
                    str(R.string.rt_install_verify_failed, "C/C++"),
                )
            }
            DevStack.PHP -> {
                installer.aptInstall(
                    proot,
                    listOf("php-cli", "php-mbstring", "php-xml", "php-curl", "php-zip", "unzip"),
                    str(R.string.rt_install_php),
                    from,
                    onProgress,
                )
                installComposer(proot, from, onProgress)
                installer.verifyGuest(proot, "php --version && composer --version", str(R.string.rt_install_verify_failed, "PHP"))
            }
            DevStack.GOLANG -> {
                installer.aptInstall(
                    proot,
                    listOf("golang-go"),
                    "Installing Go (Golang) compiler and tools",
                    from,
                    onProgress,
                )
                installer.verifyGuest(proot, "go version", str(R.string.rt_install_verify_failed, "Go"))
            }
            DevStack.RUST -> {
                installer.aptInstall(
                    proot,
                    listOf("rustc", "cargo", "build-essential"),
                    "Installing Rust and Cargo toolchain",
                    from,
                    onProgress,
                )
                installer.verifyGuest(proot, "rustc --version && cargo --version", str(R.string.rt_install_verify_failed, "Rust"))
            }
            DevStack.FLUTTER -> {
                installer.aptInstall(
                    proot,
                    listOf("git", "curl", "unzip", "xz-utils", "zip", "libglu1-mesa"),
                    "Installing Flutter & Dart dependencies",
                    from,
                    onProgress,
                )
                installFlutterToolchain(proot, from, to, onProgress)
            }
        }
        if (!verified) return
        writeDevStackState(readDevStackState().apply { put(stack.name, true) })
        onProgress(RuntimeInstallProgress(str(R.string.rt_install_stack_installed, stack.localizedLabel(context)), to))
    }

    private suspend fun installComposer(
        proot: File,
        fraction: Float,
        onProgress: suspend (RuntimeInstallProgress) -> Unit,
    ) {
        val composer = File(rootfs, "usr/local/bin/composer")
        if (composer.isFile) return
        onProgress(RuntimeInstallProgress(str(R.string.rt_install_downloading_named, "Composer"), fraction))
        downloads.mkdirs()
        val staged = File(downloads, "composer.phar")
        val checksum = RuntimeArchiveExtractor.fetchText("https://getcomposer.org/download/latest-stable/composer.phar.sha256sum")
            .lineSequence()
            .firstOrNull()
            ?.trim()
            ?.substringBefore(' ')
            ?.takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
            ?: error("Composer checksum was not found")
        RuntimeArchiveExtractor.downloadVerified(
            "https://getcomposer.org/download/latest-stable/composer.phar",
            staged,
            checksum,
        ) { _, _ -> }
        composer.parentFile?.mkdirs()
        if (composer.exists()) composer.delete()
        staged.inputStream().use { input -> FileOutputStream(composer).use { input.copyTo(it) } }
        staged.delete()
        Os.chmod(composer.absolutePath, 0b111101101)
        onProgress(RuntimeInstallProgress(str(R.string.rt_install_installing_named, "Composer"), fraction))
    }

    private suspend fun installFlutterToolchain(
        proot: File,
        from: Float,
        to: Float,
        onProgress: suspend (RuntimeInstallProgress) -> Unit,
    ) {
        val flutterBin = File(rootfs, "usr/local/bin/flutter")
        val dartBin = File(rootfs, "usr/local/bin/dart")
        if (flutterBin.exists() && dartBin.exists()) return
        onProgress(RuntimeInstallProgress("Configuring Flutter SDK and Dart runtime...", from + (to - from) * 0.5f))
        installer.verifyGuest(
            proot,
            "if [ ! -d /opt/flutter ]; then git clone --depth 1 -b stable https://github.com/flutter/flutter.git /opt/flutter; fi && ln -sf /opt/flutter/bin/flutter /usr/local/bin/flutter && ln -sf /opt/flutter/bin/dart /usr/local/bin/dart",
            str(R.string.rt_install_verify_failed, "Flutter / Dart"),
        )
    }

    private suspend fun installAndroidToolchain(
        proot: File,
        from: Float,
        to: Float,
        onProgress: suspend (RuntimeInstallProgress) -> Unit,
    ) {
        val marker = File(rootfs, "root/.pocket-android-tools-version")
        val androidHome = File(rootfs, "root/android-sdk")
        val gradleHome = File(rootfs, "opt/gradle")
        val localMaven = File(rootfs, "root/maven/localMvnRepository")
        if (marker.readTextOrNull() != RuntimeInstaller.ANDROID_TOOLS_VERSION ||
            !File(androidHome, "platforms/android-36/android.jar").isFile ||
            !File(androidHome, "build-tools/35.0.0/aapt2").isFile ||
            !File(gradleHome, "gradle-8.14.3/bin/gradle").isFile ||
            !localMaven.isDirectory) {
            installer.installRuntimeOverlay(
                RuntimeInstaller.ANDROID_BUNDLE,
                str(R.string.rt_install_android_tools),
                from,
                to,
                onProgress,
            )
            makeAndroidToolsExecutable(androidHome, gradleHome)
            marker.parentFile?.mkdirs()
            marker.writeText(RuntimeInstaller.ANDROID_TOOLS_VERSION)
        }
        writeAndroidGradleConfiguration(rootfs)

        installer.verifyGuest(
            proot,
            "java -version 2>&1 | grep -E '\"17\\.|version 17' && " +
                "gradle --version && aapt2 version && test -f \"${'$'}ANDROID_HOME/platforms/android-36/android.jar\"",
            str(R.string.rt_install_verify_failed, "Android SDK, Gradle, Java"),
        )
    }

    private fun makeAndroidToolsExecutable(androidHome: File, gradleHome: File) {
        listOf(
            File(androidHome, "build-tools/35.0.0/aapt2"),
            File(androidHome, "cmdline-tools/latest/bin/sdkmanager"),
            File(gradleHome, "gradle-8.14.3/bin/gradle"),
        ).forEach { file ->
            if (file.isFile) Os.chmod(file.absolutePath, 0b111101101)
        }
    }

    private fun writeAndroidGradleInitScript(runtimeRootfs: File) {
        val initScript = File(runtimeRootfs, "root/.gradle/init.d/pocketdev-android.gradle")
        initScript.parentFile?.mkdirs()
        initScript.writeText(
            """
            // PocketDev local Android build hook. Injected automatically so projects
            // download Android build plugins and dependencies through the local maven
            // repository mirrored into Ubuntu rootfs without network overhead.
            allprojects {
                buildscript {
                    repositories {
                        maven {
                            url = uri("/root/maven/localMvnRepository")
                        }
                        google()
                        mavenCentral()
                    }
                }
                repositories {
                    maven {
                        url = uri("/root/maven/localMvnRepository")
                    }
                    google()
                    mavenCentral()
                }
            }
            """.trimIndent() + "\n",
        )
    }

    internal fun writeAndroidGradleConfiguration(runtimeRootfs: File) {
        writeAndroidGradleInitScript(runtimeRootfs)
        val properties = File(runtimeRootfs, "root/.gradle/gradle.properties")
        properties.parentFile?.mkdirs()
        val customProperties = if (properties.isFile) {
            properties.readLines().filterNot(::isManagedAndroidGradleProperty)
        } else {
            emptyList()
        }
        val managedProperties = listOf(
            "# Managed by PocketDev Android runtime installer. Do not edit directly.",
            "android.aapt2FromMavenOverride=/root/android-sdk/build-tools/35.0.0/aapt2",
            "org.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m",
            "org.gradle.workers.max=2",
            "org.gradle.parallel=false",
            "kotlin.compiler.execution.strategy=in-process",
        )
        val allLines = managedProperties + customProperties
        properties.writeText(allLines.joinToString("\n").trimEnd() + "\n")
    }

    private fun isManagedAndroidGradleProperty(line: String): Boolean =
        line.startsWith("# Managed by PocketDev") ||
            setOf(
                "android.aapt2FromMavenOverride",
                "org.gradle.jvmargs",
                "org.gradle.workers.max",
                "org.gradle.parallel",
                "kotlin.compiler.execution.strategy",
            ).any { line.startsWith("$it=") || line.startsWith("$it ") }

    fun migrateLegacyToolMarkers() {
        if (!File(rootfs, "usr/bin/bash").isFile) return
        if (installer.languageToolsMarker.readTextOrNull() != RuntimeInstaller.LANGUAGE_TOOLS_VERSION) return
        if (installer.coreToolsMarker.readTextOrNull() != RuntimeInstaller.CORE_TOOLS_VERSION) {
            installer.coreToolsMarker.writeText(RuntimeInstaller.CORE_TOOLS_VERSION)
        }
        val state = readDevStackState()
        DevStack.entries.forEach { stack -> if (!state.containsKey(stack.name)) state[stack.name] = true }
        writeDevStackState(state)
    }

    fun installedStacks(): Set<DevStack> = readDevStackState()
        .filterValues { it }
        .keys
        .mapNotNull { name -> runCatching { DevStack.valueOf(name) }.getOrNull() }
        .filter(::isStackInstalled)
        .toSet()

    fun isStackInstalled(stack: DevStack): Boolean {
        if (readDevStackState()[stack.name] != true) return false
        if (stack != DevStack.ANDROID) return true
        return File(rootfs, "root/.pocket-android-tools-version").readTextOrNull() == RuntimeInstaller.ANDROID_TOOLS_VERSION &&
            File(rootfs, "root/android-sdk/platforms/android-36/android.jar").isFile &&
            File(rootfs, "root/android-sdk/build-tools/35.0.0/aapt2").isFile &&
            File(rootfs, "opt/gradle/gradle-8.14.3/bin/gradle").isFile &&
            File(rootfs, "root/maven/localMvnRepository").let { it.isDirectory && !it.list().isNullOrEmpty() } &&
            File(rootfs, "root/.gradle/init.d/pocketdev-android.gradle").isFile
    }

    private fun readDevStackState(): MutableMap<String, Boolean> {
        if (!devStacksFile.isFile) return mutableMapOf()
        return runCatching {
            val obj = JSONObject(devStacksFile.readText())
            mutableMapOf<String, Boolean>().apply {
                DevStack.entries.forEach { stack ->
                    if (obj.has(stack.name)) put(stack.name, obj.optBoolean(stack.name))
                }
            }
        }.getOrDefault(mutableMapOf())
    }

    private fun writeDevStackState(state: Map<String, Boolean>) {
        devStacksFile.parentFile?.mkdirs()
        val obj = JSONObject()
        state.forEach { (name, value) -> obj.put(name, value) }
        devStacksFile.writeText(obj.toString())
    }
}

