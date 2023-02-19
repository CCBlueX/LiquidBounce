/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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
import com.labymedia.ultralight.javascript.JavascriptValue
import net.ccbluex.liquidbounce.utils.client.ThreadLock
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.function.Consumer

/**
 * This class is used in case Ultralight needs a Javascript context.
 */
class ViewContextProvider(private val view: ThreadLock<UltralightView>) : ContextProvider {

    override fun syncWithJavascript(callback: Consumer<JavascriptContextLock>) {
        runCatching {
            view.get().lockJavascriptContext().use { lock -> callback.accept(lock) }
        }.onFailure {
            logger.warn("An exception occurred which prevented a JavaScript action from being executed by Ultralight JS Engine.")
            return@onFailure
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
