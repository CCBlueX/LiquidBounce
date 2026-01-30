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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import java.util.function.BiPredicate

/**
 * Closes HandledScreen with its title contains specified words
 */
object ModuleGUICloser : ClientModule(
    "GUICloser",
    ModuleCategories.MISC,
    aliases = listOf("AutoClose", "ContainerCloser")
) {

    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.guiCloser"

    private val mode by enumChoice("Mode", Mode.MATCHES)
    private val filters by regexList("Filter", mutableSetOf(Regex("^Vote$")))

    private enum class Mode(override val tag: String) : Tagged, BiPredicate<Regex, Component> {
        MATCHES("Matches"),
        CONTAINS("Contains");

        override fun test(regex: Regex, text: Component): Boolean = when (this) {
            MATCHES -> regex.matches(text.string)
            CONTAINS -> regex.containsMatchIn(text.string)
        }
    }

    private val printScreenTitle by boolean("PrintScreenTitle", false).doNotIncludeAlways()

    private fun isInFilter(entry: Component) = filters.any { regex ->
        mode.test(regex, entry)
    }

    @Suppress("unused")
    private val openScreenHandler = handler<ScreenEvent> {
        val screen = it.screen as? AbstractContainerScreen<*> ?: return@handler

        if (isInFilter(screen.title)) {
            it.cancelEvent()
        } else if (printScreenTitle) {
            chat(regular("GUICloser: container screen title: "), highlight(screen.title.string).copyable())
        }
    }
}
