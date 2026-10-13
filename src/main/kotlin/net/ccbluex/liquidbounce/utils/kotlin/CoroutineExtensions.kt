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

package net.ccbluex.liquidbounce.utils.kotlin

import com.google.common.base.Suppliers
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.minecraft.ReportedException
import org.apache.logging.log4j.Logger
import java.util.function.Supplier
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun Array<out Job>.joinAll() = forEach { it.join() }

internal class CommonCoroutineExceptionHandler(
    private val logger: Supplier<out Logger>,
): CoroutineExceptionHandler {
    constructor(logger: Logger): this(Suppliers.ofInstance(logger))

    private fun coroutineName(context: CoroutineContext) =
        context[CoroutineName]?.toString() ?: context.toString()

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        when (exception) {
            is CancellationException -> return
            is ReportedException -> ErrorHandler.fatal(exception, additionalMessage = coroutineName(context))
            else -> logger.get().error("Unhandled exception caught for ${coroutineName(context)}", exception)
        }
    }

    override val key: CoroutineContext.Key<*>
        get() = CoroutineExceptionHandler.Key

    companion object {
        val Default = CommonCoroutineExceptionHandler(logger)
    }
}
