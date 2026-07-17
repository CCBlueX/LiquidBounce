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
package net.ccbluex.liquidbounce.utils.netty

import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException

// ---- Netty Future Coroutine Bridges ----

/**
 * Suspend until this Netty Future completes,
 * and rethrows the cause of the failure if this future failed.
 */
suspend fun <V, F : Future<V>> F.syncSuspend(): F {
    if (isDone) return unwrapDone().getOrThrow()

    return suspendCancellableCoroutine { cont ->
        addListener(FutureResultContListener(cont))

        cont.invokeOnCancellation {
            this.cancel(false)
        }
    }
}

/**
 * Suspend until this Netty Future completes.
 */
suspend fun <F : Future<*>> F.awaitSuspend(): F {
    if (isDone) return this

    return suspendCancellableCoroutine { cont ->
        addListener(FutureContListener(cont))

        cont.invokeOnCancellation {
            this.cancel(false)
        }
    }
}

private class FutureContListener<V, F : Future<V>>(
    private val cont: CancellableContinuation<F>
) : GenericFutureListener<F> {
    override fun operationComplete(future: F) {
        if (cont.isActive) {
            cont.resumeWith(Result.success(future))
        }
    }
}

private class FutureResultContListener<V, F : Future<V>>(
    private val cont: CancellableContinuation<F>
) : GenericFutureListener<F> {
    override fun operationComplete(future: F) {
        if (cont.isActive) {
            cont.resumeWith(future.unwrapDone())
        }
    }
}

private fun <V, F : Future<V>> F.unwrapDone(): Result<F> =
    when {
        isSuccess -> Result.success(this)
        isCancelled -> Result.failure(CancellationException("Netty Future was cancelled"))
        else -> Result.failure(
            this.cause() ?: IllegalStateException("Future failed without cause")
        )
    }
