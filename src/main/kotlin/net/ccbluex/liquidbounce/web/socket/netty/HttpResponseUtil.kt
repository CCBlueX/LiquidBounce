/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.socket.netty

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import io.netty.buffer.Unpooled
import io.netty.handler.codec.http.*
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import org.apache.tika.Tika
import java.io.File
import java.io.InputStream

private fun httpResponse(status: HttpResponseStatus, contentType: String = "text/plain",
                         content: String): FullHttpResponse {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        status,
        Unpooled.wrappedBuffer(content.toByteArray())
    )

    val httpHeaders = response.headers()
    httpHeaders[HttpHeaderNames.CONTENT_TYPE] = contentType
    httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = "*"
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS] = "GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS"
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS] = "Content-Type, Content-Length, Authorization, Accept, X-Requested-With"
    return response
}

private fun httpResponse(status: HttpResponseStatus, json: JsonElement)
        = httpResponse(status, "application/json", protocolGson.toJson(json))

fun httpOk(jsonElement: JsonElement)
        = httpResponse(HttpResponseStatus.OK, jsonElement)

fun httpNotFound(path: String, reason: String): FullHttpResponse {
    val jsonObject = JsonObject()
    jsonObject.addProperty("path", path)
    jsonObject.addProperty("reason", reason)
    return httpResponse(HttpResponseStatus.NOT_FOUND, jsonObject)
}

fun httpInternalServerError(exception: String): FullHttpResponse {
    val jsonObject = JsonObject()
    jsonObject.addProperty("reason", exception)
    return httpResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, jsonObject)
}

fun httpForbidden(reason: String): FullHttpResponse {
    val jsonObject = JsonObject()
    jsonObject.addProperty("reason", reason)
    return httpResponse(HttpResponseStatus.FORBIDDEN, jsonObject)
}

fun httpBadRequest(reason: String): FullHttpResponse {
    val jsonObject = JsonObject()
    jsonObject.addProperty("reason", reason)
    return httpResponse(HttpResponseStatus.BAD_REQUEST, jsonObject)
}

private val tika = Tika()

fun httpFile(file: File): FullHttpResponse {
    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK,
        Unpooled.wrappedBuffer(file.readBytes())
    )

    val httpHeaders = response.headers()
    httpHeaders[HttpHeaderNames.CONTENT_TYPE] = tika.detect(file)
    httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = "*"
    return response
}

fun httpFileStream(stream: InputStream): FullHttpResponse {
    val bytes = stream.readBytes()

    val response = DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK,
        Unpooled.wrappedBuffer(bytes)
    )

    val httpHeaders = response.headers()
    httpHeaders[HttpHeaderNames.CONTENT_TYPE] = tika.detect(bytes)
    httpHeaders[HttpHeaderNames.CONTENT_LENGTH] = response.content().readableBytes()
    httpHeaders[HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN] = "*"

    return response
}

