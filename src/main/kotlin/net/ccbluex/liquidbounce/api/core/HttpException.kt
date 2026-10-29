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

import java.net.HttpURLConnection

class HttpException(val method: HttpMethod, val url: String, val code: Int, val content: String)
    : Exception("${method.name} $url failed with code $code: $content")

/**
 * The [HttpException] behind this. OkHttp hands one thrown by an interceptor of an async call on
 * wrapped in an [java.io.IOException].
 */
val Throwable.httpException: HttpException?
    get() = this as? HttpException
        ?: cause as? HttpException
        ?: suppressed.firstNotNullOfOrNull { it as? HttpException }

/**
 * [block]'s result, `null` when the server answers 404.
 */
internal inline fun <T> orNotFound(block: () -> T): T? = try {
    block()
} catch (e: Exception) {
    if (e.httpException?.code == HttpURLConnection.HTTP_NOT_FOUND) null else throw e
}
