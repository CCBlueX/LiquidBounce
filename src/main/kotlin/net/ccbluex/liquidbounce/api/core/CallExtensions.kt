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

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CompletableFuture

/** Enqueues this call and forwards cancellation of the returned future to it. */
internal fun Call.enqueueAsFuture(): CompletableFuture<Response> {
    val future = CompletableFuture<Response>()
    future.whenComplete { _, _ ->
        if (future.isCancelled) cancel()
    }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            // A response can arrive after cancellation; nobody owns its body in that case.
            if (!future.complete(response)) response.close()
        }

        override fun onFailure(call: Call, e: IOException) {
            future.completeExceptionally(e)
        }
    })
    return future
}
