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
package net.ccbluex.liquidbounce.integration.interop

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.coroutines.CancellationException

class HttpStatusException(
    val status: HttpStatusCode,
    val body: Map<String, String>
) : CancellationException()

suspend fun ApplicationCall.badRequest(reason: String): Nothing =
    throw HttpStatusException(HttpStatusCode.BadRequest, mapOf("reason" to reason))

suspend fun ApplicationCall.forbidden(reason: String): Nothing =
    throw HttpStatusException(HttpStatusCode.Forbidden, mapOf("reason" to reason))

suspend fun ApplicationCall.unauthorized(reason: String): Nothing =
    throw HttpStatusException(HttpStatusCode.Unauthorized, mapOf("reason" to reason))

suspend fun ApplicationCall.notFound(path: String, reason: String): Nothing =
    throw HttpStatusException(HttpStatusCode.NotFound, mapOf("path" to path, "reason" to reason))

suspend fun ApplicationCall.serviceUnavailable(reason: String): Nothing =
    throw HttpStatusException(HttpStatusCode.ServiceUnavailable, mapOf("reason" to reason))

suspend fun ApplicationCall.internalServerError(reason: String): Nothing =
    throw HttpStatusException(HttpStatusCode.InternalServerError, mapOf("reason" to reason))
