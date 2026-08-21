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

package net.ccbluex.liquidbounce.api.core

import com.google.gson.Gson
import com.google.gson.JsonElement
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink

/**
 * Deliberately plain: request bodies must not be pretty-printed and must not run through the client's
 * serialization exclusion strategies, so none of the instances in [net.ccbluex.liquidbounce.config.gson]
 * are appropriate here.
 */
private val requestBodyGson = Gson()

private fun Buffer.asRequestBody(mediaType: MediaType) = object : RequestBody() {
    override fun contentType() = mediaType
    override fun contentLength(): Long = size
    override fun writeTo(sink: BufferedSink) {
        sink.writeAll(this@asRequestBody.copy())
    }
}

fun Gson.makeRequestBody(data: Any?): RequestBody {
    val buffer = Buffer()
    buffer.outputStream().writer(Charsets.UTF_8).use {
        toJson(data, it)
    }
    return buffer.asRequestBody(HttpClient.MediaTypes.JSON)
}

@JvmOverloads
fun JsonElement.toRequestBody(gson: Gson = requestBodyGson): RequestBody = gson.makeRequestBody(this)
