/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.integration.backend.backends.ultralight

import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.mcef.listeners.MCEFProgressListener
import net.ccbluex.liquidbounce.mcef.utils.FileUtils
import net.ccbluex.liquidbounce.utils.client.env
import java.io.File
import java.security.MessageDigest

/**
 * The Ultralight runtime and the natives of Ultralight Java Reborn for this platform.
 *
 * The Ultralight license only allows us to pass Ultralight on to our users as part of LiquidBounce, so the runtime
 * is downloaded from our API, just like the natives of CEF. It comes with the Ultralight EULA and notices.
 */
internal class UltralightRuntime(folder: File) {

    /**
     * The platform in the naming of Ultralight, or null if Ultralight doesn't support it.
     */
    val platform: String? = run {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val osName = when {
            "win" in os -> "win"
            "mac" in os -> "mac"
            "linux" in os -> "linux"
            else -> return@run null
        }

        val archName = when (arch) {
            "amd64", "x86_64" -> "x64"
            "aarch64", "arm64" -> "arm64"
            else -> return@run null
        }

        // Ultralight has no build for Windows on ARM
        if (osName == "win" && archName == "arm64") null else "$osName-$archName"
    }

    private val directory = folder.resolve(ULTRALIGHT_VERSION)

    /**
     * The runtime with `bin/`, `resources/` and `license/`.
     */
    val runtimeDirectory = directory.resolve("runtime")

    /**
     * The natives jar of Ultralight Java Reborn, which can be replaced for development.
     */
    val nativesJar = env("LB_ULTRALIGHT_NATIVES", "net.ccbluex.liquidbounce.ultralight.natives")?.let(::File)
        ?: directory.resolve("ultralight-java-reborn-platform-jni-$UJR_VERSION-$platform.jar")

    val eula: File
        get() = runtimeDirectory.resolve("license/EULA.txt")

    private val completeMarker = directory.resolve(".complete")
    private val completeContent = "$ULTRALIGHT_VERSION $UJR_VERSION"

    val isAvailable: Boolean
        get() = completeMarker.isFile && completeMarker.readText() == completeContent &&
            runtimeDirectory.isDirectory && nativesJar.isFile

    suspend fun download(progressListener: MCEFProgressListener) {
        val platform = requireNotNull(platform) {
            "Ultralight doesn't support ${System.getProperty("os.name")} on ${System.getProperty("os.arch")}"
        }

        directory.deleteRecursively()
        directory.mkdirs()

        val runtimeUrl = "$RUNTIME_URL/$ULTRALIGHT_VERSION/$platform"
        val runtimeArchive = directory.resolve("runtime.tar.gz")
        FileUtils.downloadFile(progressListener, "Ultralight", runtimeUrl, runtimeArchive)
        verify(runtimeArchive, fetch("$runtimeUrl/checksum"))
        FileUtils.extractTarGz(progressListener, "Ultralight", runtimeArchive, runtimeDirectory)
        runtimeArchive.delete()

        if (!nativesJar.isFile) {
            val nativesUrl = "$NATIVES_URL/$UJR_VERSION/${nativesJar.name}"
            FileUtils.downloadFile(progressListener, "Ultralight Java Reborn", nativesUrl, nativesJar)
            verify(nativesJar, fetch("$nativesUrl.sha256"))
        }

        completeMarker.writeText(completeContent)
    }

    private suspend fun fetch(url: String) = HttpClient.request(url, HttpMethod.GET).parse<String>().trim()

    private fun verify(file: File, expectedSha256: String) {
        val actual = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).toHexString()
        check(actual.equals(expectedSha256, ignoreCase = true)) {
            "Checksum mismatch for ${file.name}: expected $expectedSha256, got $actual"
        }
    }

    companion object {
        const val ULTRALIGHT_VERSION = "1.4.0"
        const val UJR_VERSION = "0.1.0"

        private const val RUNTIME_URL = "https://api.liquidbounce.net/api/v3/resource/ultralight"
        private const val NATIVES_URL =
            "https://maven.ccbluex.net/releases/net/ccbluex/ultralight/ultralight-java-reborn-platform-jni"
    }

}
