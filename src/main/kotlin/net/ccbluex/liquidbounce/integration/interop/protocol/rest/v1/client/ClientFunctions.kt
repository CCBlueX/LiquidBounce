/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 */
package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.services.client.ClientUpdate.update
import net.ccbluex.liquidbounce.config.types.FileDialogMode
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.usesViaFabricPlus
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.util.Util
import org.lwjgl.PointerBuffer
import org.lwjgl.system.MemoryStack
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

// GET /api/v1/client/info
@Suppress("UNUSED_PARAMETER")
fun getClientInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("gameVersion", mc.gameVersion)
    addProperty("clientVersion", LiquidBounce.clientVersion)
    addProperty("clientName", LiquidBounce.CLIENT_NAME)
    addProperty("development", LiquidBounce.IN_DEVELOPMENT)
    addProperty("fps", mc.currentFps)
    addProperty("gameDir", mc.runDirectory.path)
    addProperty("inGame", inGame)
    addProperty("viaFabricPlus", usesViaFabricPlus)
    addProperty("hasProtocolHack", usesViaFabricPlus)
})

// GET /api/v1/client/update
@Suppress("UNUSED_PARAMETER")
fun getUpdateInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("development", LiquidBounce.IN_DEVELOPMENT)
    addProperty("commit", LiquidBounce.clientCommit)

    val updateInfo = update ?: return@apply
    add("update", JsonObject().apply {
        addProperty("buildId", updateInfo.buildId)
        addProperty("commitId", updateInfo.commitId.substring(0, 7))
        addProperty("branch", updateInfo.branch)
        addProperty("clientVersion", updateInfo.lbVersion)
        addProperty("minecraftVersion", updateInfo.mcVersion)
        addProperty("release", updateInfo.release)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(updateInfo.date)
        addProperty("date", SimpleDateFormat().format(dateFormat))
        addProperty("message", updateInfo.message)

        addProperty("url", updateInfo.url)
    })
})

// POST /api/v1/client/exit
@Suppress("UNUSED_PARAMETER")
fun postExit(requestObject: RequestObject): FullHttpResponse {
    mc.scheduleStop()
    return httpOk(JsonObject())
}

// GET /api/v1/client/window
@Suppress("UNUSED_PARAMETER")
fun getWindowInfo(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("width", mc.window.width)
    addProperty("height", mc.window.height)
    addProperty("scaledWidth", mc.window.scaledWidth)
    addProperty("scaledHeight", mc.window.scaledHeight)
    addProperty("scaleFactor", mc.window.scaleFactor)
    addProperty("guiScale", mc.options.guiScale.value)
})

// POST /api/v1/client/browse
fun postBrowse(requestObject: RequestObject): FullHttpResponse {
    val jsonObj = requestObject.asJson<JsonObject>()
    val target = jsonObj["target"]?.asString ?: return httpForbidden("No target specified")

    val url = POSSIBLE_URL_TARGETS[target] ?: return httpForbidden("Unknown target")

    Util.getOperatingSystem().open(url)
    return httpOk(JsonObject())
}

// POST /api/v1/client/openInExplorer
@Suppress("SpreadOperator")
fun postOpenInExplorer(requestObject: RequestObject): FullHttpResponse {
    val jsonObj = requestObject.asJson<JsonObject>()
    val file = jsonObj["file"]?.asString
        ?.let {
            File(it)
        }
        ?: return httpBadRequest("File is not specified")

    if (!file.exists()) {
        return httpBadRequest("File does not exist")
    }

    runCatching {
        val os = System.getProperty("os.name").lowercase()

        when {
            os.contains("win") -> {
                val command = if (file.isDirectory()) {
                    arrayOf("explorer.exe", file.absolutePath)
                } else {
                    arrayOf("explorer.exe", "/select,", file.absolutePath)
                }

                ProcessBuilder(*command).start()
            }

            os.contains("mac") -> {
                val command = if (file.isDirectory()) {
                    arrayOf("open", file.absolutePath)
                } else {
                    arrayOf("open", "-R", file.absolutePath)
                }

                ProcessBuilder(*command).start()
            }

            os.contains("nix") || os.contains("nux") || os.contains("aix") -> {
                val command = arrayOf("xdg-open", file.parentFile.absolutePath ?: file.absolutePath)
                ProcessBuilder(*command).start()
            }

            else -> {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file.parentFile ?: file)
                }
            }
        }
    }

    return httpOk(JsonObject())
}

// POST /api/v1/client/fileDialog
fun postFileDialog(requestObject: RequestObject): FullHttpResponse {
    val jsonObj = requestObject.asJson<JsonObject>()
    val mode = FileDialogMode.entries.find { it.name == jsonObj["mode"]?.asString }
        ?: return httpBadRequest("No dialog mode provided")

    val filterPatterns = jsonObj["supportedExtensions"]
        ?.asJsonArray
        ?.map { it.toString() }
        ?.toTypedArray()

    val files = MemoryStack.stackPush().use { stack ->
        val filterPatterns: PointerBuffer? = filterPatterns?.let {
            val patternList = it.map { ext -> "*.$ext" }
            val buffer = stack.mallocPointer(patternList.size)
            patternList.forEach { pattern ->
                buffer.put(stack.ASCII(pattern))
            }
            buffer.flip()
        }

        when (mode) {
            FileDialogMode.OPEN_FILE -> TinyFileDialogs.tinyfd_openFileDialog(
                mode.title,
                null,
                filterPatterns,
                null,
                false
            )
            FileDialogMode.SAVE_FILE -> TinyFileDialogs.tinyfd_saveFileDialog(
                mode.title,
                null,
                filterPatterns,
                null
            )
            FileDialogMode.OPEN_DIRECTORY -> TinyFileDialogs.tinyfd_selectFolderDialog(mode.title, null)
        }
    }

    return httpOk(JsonObject().apply {
        files?.let {
            val file = it.split("|")[0]
            addProperty("file", file)
        }

        addProperty("cancelled", files == null)
    })
}

private val POSSIBLE_URL_TARGETS: Map<String, URI> = run {
    val properties = Properties()

    properties.load(LiquidBounce::class.java.getResourceAsStream("/resources/liquidbounce/client_urls.properties"))

    properties.stringPropertyNames().associateWith { URI(properties.getProperty(it)) }
}
