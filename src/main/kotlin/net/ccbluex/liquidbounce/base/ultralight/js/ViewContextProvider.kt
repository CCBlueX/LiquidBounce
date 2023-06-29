/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.base.ultralight.js

import com.labymedia.ultralight.UltralightView
import com.labymedia.ultralight.databind.context.ContextProvider
import com.labymedia.ultralight.databind.context.ContextProviderFactory
import com.labymedia.ultralight.javascript.JavascriptContextLock
import com.labymedia.ultralight.javascript.JavascriptException
import com.labymedia.ultralight.javascript.JavascriptValue
import net.ccbluex.liquidbounce.utils.client.ThreadLock
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.function.Consumer

/**
 * This class is used in case Ultralight needs a Javascript context.
 */
class ViewContextProvider(private val view: ThreadLock<UltralightView>) : ContextProvider {

    override fun syncWithJavascript(callback: Consumer<JavascriptContextLock>) {
        view.get().lockJavascriptContext().use { lock ->
            runCatching { callback.accept(lock) }.onFailure {
                if (it is JavascriptException) {
                    // We can't move this down outside the js context lock!
                    val err = it.value.toObject()
                    logger.error("An error occoured in javascript: $err at ${err.getProperty("stack")}")
                }
            }
        }
    }

    class Factory(private val view: ThreadLock<UltralightView>) : ContextProviderFactory {

        override fun bindProvider(value: JavascriptValue): ContextProvider {
            // We only have one view, so we can ignore the value.
            // Else use the formula pointed at above to find a view for a given value.
            return ViewContextProvider(view)
        }

    }
}
