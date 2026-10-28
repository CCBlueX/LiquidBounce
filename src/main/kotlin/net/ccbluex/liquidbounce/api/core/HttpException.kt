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

import java.io.IOException
import java.net.HttpURLConnection

/**
 * A response with a non-2xx status code.
 *
 * An [IOException] on purpose: OkHttp hands a failure thrown by an interceptor to the callback as-is only
 * when it is an [IOException] - anything else is replaced with a synthetic `canceled due to ...` [IOException]
 * that carries the original as suppressed. Staying an [IOException] keeps `catch (e: HttpException)` reachable.
 */
class HttpException(val method: HttpMethod, val url: String, val code: Int, val content: String)
    : IOException("${method.name} $url failed with code $code: $content")

/**
 * The [HttpException] behind this, if any - either itself, or the one a wrapper such as a
 * [java.util.concurrent.CompletionException] carries.
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
