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
package net.ccbluex.liquidbounce.api.thirdparty

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.bodyAsJson
import net.ccbluex.liquidbounce.utils.client.mc
import okhttp3.FormBody
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import kotlin.io.path.div

/**
 * Shares logs through logs.liquidbounce.net.
 */
object LogsApi {

    private const val LOG_API = "https://logs.liquidbounce.net/api/1/log"
    private const val MAX_LOG_BYTES = 10L * 1024 * 1024
    private const val MAX_LOG_LINES = 25000

    /**
     * The end of `latest.log` that fits into one upload, or null without a log.
     */
    suspend fun readLatestLog(): String? = withContext(Dispatchers.IO) {
        val path = (mc.gameDirectory.toPath() / "logs" / "latest.log").toOkioPath()
        val size = FileSystem.SYSTEM.metadataOrNull(path)?.size ?: return@withContext null
        val skipped = (size - MAX_LOG_BYTES).coerceAtLeast(0L)
        var content = FileSystem.SYSTEM.read(path) {
            skip(skipped)
            readUtf8(size - skipped)
        }

        if (skipped > 0) {
            // Drop the line that was cut off
            content = content.substringAfter('\n')
        }

        content.lineSequence().toList().takeLast(MAX_LOG_LINES).joinToString("\n")
    }

    /**
     * Uploads the given content and returns the URL of the log.
     */
    suspend fun upload(content: String): String = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().add("content", content).build()
        val response = HttpClient.request(LOG_API, HttpMethod.POST, body = body).bodyAsJson<LogResponse>()
        check(response.success && response.url != null) { "Failed to upload log: ${response.error}" }
        response.url
    }

    private class LogResponse(val success: Boolean, val url: String?, val error: String?)

}
