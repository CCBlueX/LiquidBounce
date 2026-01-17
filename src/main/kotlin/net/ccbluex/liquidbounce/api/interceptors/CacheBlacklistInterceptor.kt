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

package net.ccbluex.liquidbounce.api.interceptors

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response

class CacheBlacklistInterceptor(
    val blacklistedHosts: Set<String>,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.url.host in blacklistedHosts) {
            val newRequest = request.newBuilder()
                .cacheControl(cacheControl)
                .build()
            return chain.proceed(newRequest)
        }
        return chain.proceed(request)
    }

    companion object {
        private val cacheControl = CacheControl.Builder().noCache().noStore().build()
    }
}
