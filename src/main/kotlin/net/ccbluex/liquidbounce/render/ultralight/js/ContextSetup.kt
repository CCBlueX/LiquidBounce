/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
 *
 * and
 *
 * Ultralight Java - Java wrapper for the Ultralight web engine
 * Copyright (C) 2020 - 2021 LabyMedia and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.ccbluex.liquidbounce.render.ultralight.js

import com.labymedia.ultralight.javascript.JavascriptContext
import net.ccbluex.liquidbounce.render.ultralight.ScreenView
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.render.ultralight.View
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.UltralightJsClient
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.UltralightJsKotlin
import net.ccbluex.liquidbounce.render.ultralight.js.bindings.UltralightJsUi
import net.ccbluex.liquidbounce.utils.client.mc

/**
 * Context setup
 */
object ContextSetup {

    fun setupContext(view: View, context: JavascriptContext) {
        val globalContext = context.globalContext
        val globalObject = globalContext.globalObject

        globalObject.setProperty(
            "engine",
            view.databind.conversionUtils.toJavascript(context, UltralightEngine),
            0
        )

        globalObject.setProperty(
            "view",
            view.databind.conversionUtils.toJavascript(context, view),
            0
        )

        globalObject.setProperty(
            "client",
            view.databind.conversionUtils.toJavascript(context, UltralightJsClient),
            0
        )

        globalObject.setProperty(
            "events",
            view.databind.conversionUtils.toJavascript(context, view.jsEvents),
            0
        )

        // todo: minecraft has to be remapped
        globalObject.setProperty(
            "minecraft",
            view.databind.conversionUtils.toJavascript(context, mc),
            0
        )

        globalObject.setProperty(
            "ui",
            view.databind.conversionUtils.toJavascript(context, UltralightJsUi),
            0
        )

        globalObject.setProperty(
            "kotlin",
            view.databind.conversionUtils.toJavascript(context, UltralightJsKotlin),
            0
        )

        if (view is ScreenView) {
            globalObject.setProperty(
                "screen",
                view.databind.conversionUtils.toJavascript(context, view.adaptedScreen ?: view.screen),
                0
            )

            val parentScreen = view.parentScreen

            if (parentScreen != null) {
                globalObject.setProperty(
                    "parentScreen",
                    view.databind.conversionUtils.toJavascript(context, view.parentScreen),
                    0
                )
            }
        }
    }
}
