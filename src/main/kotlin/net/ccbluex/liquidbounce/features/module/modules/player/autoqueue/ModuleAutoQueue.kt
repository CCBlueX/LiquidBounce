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
package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.presets.AutoQueueCustom
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.presets.AutoQueueGommeDuels
import net.ccbluex.liquidbounce.features.module.modules.player.autoqueue.presets.AutoQueueHypixelSW
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.ccbluex.liquidbounce.integration.screen.impl.CustomStandaloneMinecraftScreen
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import java.util.function.BooleanSupplier

object ModuleAutoQueue : ClientModule("AutoQueue", ModuleCategories.PLAYER, aliases = listOf("AutoPlay")) {
    val presets = choices("Presets", AutoQueueHypixelSW, arrayOf(
        AutoQueueHypixelSW,
        AutoQueueGommeDuels,
        AutoQueueCustom
    )).apply(::tagBy)

    private val pause by multiEnumChoice("Pause", PauseCondition.entries)

    val shouldPause
        get() = pause.any { it.asBoolean }

    private enum class PauseCondition(override val tag: String) : Tagged, BooleanSupplier {
        CLICK_GUI_OPEN("ClickGuiOpen") {
            override fun getAsBoolean(): Boolean {
                val screen = mc.gui.screen()
                return screen is CustomSharedMinecraftScreen && screen.screenType == CustomScreenType.CLICK_GUI ||
                    screen is CustomStandaloneMinecraftScreen && screen.screenType == CustomScreenType.CLICK_GUI
            }
        },
        CHAT_SCREEN_OPEN("ChatScreenOpen") {
            override fun getAsBoolean() = mc.gui.screen() is ChatScreen
        },
        CONTAINER_SCREEN_OPEN("ContainerScreenOpen") {
            override fun getAsBoolean() = mc.gui.screen() is AbstractContainerScreen<*>
        },
        PAUSE_SCREEN_OPEN("PauseScreenOpen") {
            override fun getAsBoolean() = mc.gui.screen() is PauseScreen
        },
    }
}
