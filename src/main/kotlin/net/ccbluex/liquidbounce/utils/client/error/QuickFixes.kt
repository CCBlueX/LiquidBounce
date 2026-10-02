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
package net.ccbluex.liquidbounce.utils.client.error

import net.ccbluex.liquidbounce.features.addon.AddonApi
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [ErrorHandler.fatal] shows the first registered quick fix whose [QuickFix.testError] matches, unless the
 * caller passes one.
 */
@AddonApi
object QuickFixes {

    // Errors reach the handler from any thread
    private val registry = CopyOnWriteArrayList<QuickFix>()

    val CLASS_NOT_FOUND = register(QuickFix(
        description = "Some class not found",
        testError = { it is ClassNotFoundException },
        whatYouNeed = Instructions(false) { _ ->
            arrayOf(
                "Make sure you have all the libraries required by minecraft installed"
            )
        },
        whatToDo = Instructions(false) {
            val message = it.message
            if (message == null) {
                null
            } else {
                when {
                    message.contains("viaversion") -> arrayOf("Try to install ViaFabric")
                    message.contains("modmenu") -> arrayOf("Try to install ModMenu")
                    else -> null
                }
            }
        }
    ))

    val BROWSER_IS_NOT_RESPONDING = QuickFix(
        description = "The browser is not responding",
        whatToDo = Instructions(true) {
            arrayOf(
                "Disable System-wide proxy",
                "Disable Web Security/AV software",
                "Disable Smart App Control",
                "Restart LiquidBounce and try again."
            )
        }
    )

    val BROWSER_FAILED_TO_LOAD_UI = QuickFix(
        description = "The browser failed to load the UI.",
        whatToDo = Instructions(true) {
            arrayOf(
                "Disable System-wide proxy",
                "Disable Web Security/AV software",
                "Restart LiquidBounce and try again."
            )
        }
    )

    val entries: List<QuickFix> get() = registry

    fun register(quickFix: QuickFix): QuickFix {
        registry += quickFix
        return quickFix
    }

    fun unregister(quickFix: QuickFix): Boolean = registry.remove(quickFix)

}
