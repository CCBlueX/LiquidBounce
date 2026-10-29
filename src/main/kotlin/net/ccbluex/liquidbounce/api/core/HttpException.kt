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
 * A response with a non-2xx status code, thrown by [HttpClient]'s interceptor.
 *
 * An [IOException] because OkHttp only passes a failure thrown by an interceptor through to an async
 * callback as itself; every other [Throwable] it replaces with `IOException("canceled due to ...")` that
 * keeps the original only as `cause`. That is what makes `catch (e: HttpException)` reachable - it does
 * not mean the request failed on the transport level, so do not retry it as a network error.
 */
class HttpException(val method: HttpMethod, val url: String, val code: Int, val content: String)
    : IOException("${method.name} $url failed with code $code: $content")

/**
 * The [HttpException] behind this, if any - either itself, or one a wrapper carries as `cause`
 * (e.g. a [java.util.concurrent.CompletionException]) or as a suppressed exception.
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
