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

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer

/**
 * Reports how much of a response body has been read.
 */
class OkHttpProgressInterceptor(private val progressListener: ProgressListener) : Interceptor {

    fun interface ProgressListener {
        fun update(bytesRead: Long, contentLength: Long, done: Boolean)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        return response.newBuilder().body(ProgressResponseBody(response.body, progressListener)).build()
    }

    private class ProgressResponseBody(
        private val body: ResponseBody,
        private val progressListener: ProgressListener
    ) : ResponseBody() {

        private val source by lazy {
            object : ForwardingSource(body.source()) {
                private var totalBytesRead = 0L

                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    // -1 once the body is exhausted
                    if (bytesRead != -1L) {
                        totalBytesRead += bytesRead
                    }
                    progressListener.update(totalBytesRead, body.contentLength(), bytesRead == -1L)
                    return bytesRead
                }
            }.buffer()
        }

        override fun contentType(): MediaType? = body.contentType()

        override fun contentLength() = body.contentLength()

        override fun source(): BufferedSource = source

    }

}
