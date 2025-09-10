/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.highlight
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.text.Text


/**
 * Closes GenericContainerScreen with its title contains specified words
 */
object ModuleGUICloser : ClientModule("GUICloser", Category.MISC, aliases = arrayOf("AutoClose", "ContainerCloser")) {
    override val baseKey: String
        get() = "liquidbounce.module.guiCloser"

    private var filters = setOf<Regex>()

    @Suppress("unused")
    private val filterBy = textList("Filter", mutableSetOf("Vote")).onChanged { newValue ->
        filters = newValue.mapTo(HashSet(newValue.size, 1.0F)) {
            val regexPattern = it
                .replace("*", ".*")
                .replace("?", ".")

            Regex("^$regexPattern\$")
        }
    }

    private val printScreenTitle by boolean("PrintScreenTitle", false).doNotIncludeAlways()

    private fun isInFilter(entry: Text) = filters.any { regex ->
        regex.matches(entry.string)
    }

    @Suppress("unused")
    private val openScreenHandler = handler<ScreenEvent> {
        val screen = it.screen as? GenericContainerScreen ?: return@handler

        if (isInFilter(screen.title)) {
            it.cancelEvent()
        } else if (printScreenTitle) {
            chat(regular("GUICloser: container screen title: "), highlight(screen.title.string).copyable())
        }
    }
}
