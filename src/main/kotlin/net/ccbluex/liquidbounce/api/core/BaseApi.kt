/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AVATAR_USERNAME_URL
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AVATAR_UUID_URL
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.config
import okhttp3.Headers
import okhttp3.RequestBody
import java.util.*

fun formatAvatarUrl(uuid: UUID?, username: String): String {
    return if (uuid != null) {
        AVATAR_UUID_URL.format(uuid)
    } else {
        AVATAR_USERNAME_URL.format(username)
    }
}

/**
 * Base API class
 *
 * @param baseUrl The base URL of the API
 */
abstract class BaseApi(protected val baseUrl: String) {

    /**
     * Makes a request and parses the response to the specified type
     */
    protected suspend inline fun <reified T> request(
        endpoint: String,
        method: HttpMethod,
        crossinline headers: Headers.Builder.() -> Unit = {},
        body: RequestBody? = null
    ): T = HttpClient.request("$baseUrl$endpoint", method, headers = {
        add("X-Session-Token", config.sessionToken)
        headers(this)
    }, body = body).parse()

    protected suspend inline fun <reified T> head(
        endpoint: String,
        crossinline headers: Headers.Builder.() -> Unit = {}
    ): T = request(endpoint, HttpMethod.HEAD, headers)

    protected suspend inline fun <reified T> get(
        endpoint: String,
        crossinline headers: Headers.Builder.() -> Unit = {}
    ): T = request(endpoint, HttpMethod.GET, headers)

    protected suspend inline fun <reified T> post(
        endpoint: String,
        body: RequestBody? = null,
        crossinline headers: Headers.Builder.() -> Unit = {}
    ): T = request(endpoint, HttpMethod.POST, headers, body)

    protected suspend inline fun <reified T> put(
        endpoint: String,
        body: RequestBody? = null,
        crossinline headers: Headers.Builder.() -> Unit = {}
    ): T = request(endpoint, HttpMethod.PUT, headers, body)

    protected suspend inline fun <reified T> patch(
        endpoint: String,
        body: RequestBody? = null,
        crossinline headers: Headers.Builder.() -> Unit = {}
    ): T = request(endpoint, HttpMethod.PATCH, headers, body)

    protected suspend inline fun <reified T> delete(
        endpoint: String,
        crossinline headers: Headers.Builder.() -> Unit = {}
    ): T = request(endpoint, HttpMethod.DELETE, headers)

}
