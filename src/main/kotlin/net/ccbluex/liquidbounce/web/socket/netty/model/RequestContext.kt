/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.web.socket.netty.model

import io.netty.handler.codec.http.HttpMethod
import java.util.*
import java.util.stream.Collectors

data class RequestContext(var httpMethod: HttpMethod, var uri: String, var headers: Map<String, String>) {
    val contentBuffer = StringBuilder()
    val path = if (uri.contains("?")) uri.substring(0, uri.indexOf('?')) else uri
    val params = getUriParams(uri)
}

/**
 * The received uri should be like: '...?param1=value&param2=value'
 */
private fun getUriParams(uri: String): Map<String, String> {
    if (uri.contains("?")) {
        val paramsString = uri.substring(uri.indexOf('?') + 1)

        // in case of duplicated params, will be used la last value
        return Arrays.stream(
            if (paramsString.contains("&")) paramsString.split("&".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray() else arrayOf(paramsString))
            .map { value: String ->
                value.split("=".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            }
            .collect(
                Collectors.toMap(
                    { paramValue: Array<String> -> paramValue[0] },
                    { paramValue: Array<String> -> paramValue[1] },
                    { v1: String?, v2: String -> v2 })
            )
    }
    return HashMap()
}
