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
import com.labymedia.ultralight.databind.Databind
import com.labymedia.ultralight.databind.DatabindConfiguration
import com.labymedia.ultralight.javascript.JavascriptContext
import com.labymedia.ultralight.javascript.JavascriptObject
import net.ccbluex.liquidbounce.base.ultralight.ScreenViewOverlay
import net.ccbluex.liquidbounce.base.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.base.ultralight.ViewOverlay
import net.ccbluex.liquidbounce.base.ultralight.js.bindings.*
import net.ccbluex.liquidbounce.utils.client.ThreadLock

/**
 * Context setup
 */
class UltralightJsContext(viewOverlay: ViewOverlay, ulView: ThreadLock<UltralightView>) {

    private val contextProvider = ViewContextProvider(ulView)
    val databind = Databind(
        DatabindConfiguration
            .builder()
            .contextProviderFactory(ViewContextProvider.Factory(ulView))
            .build()
    )


    var events = UltralightJsEvents(contextProvider, viewOverlay)


    fun setupContext(viewOverlay: ViewOverlay, context: JavascriptContext) {
        val globalContext = context.globalContext
        val globalObject = globalContext.globalObject

        // Pass the view to the context
        setProperty(globalObject, context, "view", viewOverlay)

        setProperty(globalObject, context, "engine", UltralightEngine)
        setProperty(globalObject, context, "client", UltralightJsClient)
        setProperty(globalObject, context, "storage", UltralightStorage)
        setProperty(globalObject, context, "events", events)
        setProperty(globalObject, context, "pages", UltralightJsPages)
        setProperty(globalObject, context, "kotlin", UltralightJsKotlin)
        setProperty(globalObject, context, "utils", UltralightJsUtils)


        if (viewOverlay is ScreenViewOverlay) {
            setProperty(globalObject, context, "screen", viewOverlay.adaptedScreen ?: viewOverlay.screen)
            viewOverlay.parentScreen?.let { parentScreen ->
                setProperty(globalObject, context, "parentScreen", parentScreen)
            }
        }
    }

    /**
     * Sets a property on the given object
     */
    private fun setProperty(obj: JavascriptObject, context: JavascriptContext, name: String, value: Any) {
        obj.setProperty(name, databind.conversionUtils.toJavascript(context, value), 0)
    }

}
