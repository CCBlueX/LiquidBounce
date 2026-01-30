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

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.Window
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.Slot
import org.lwjgl.glfw.GLFW

fun interface MouseClick {
    operator fun invoke(callbackSlot: Slot?, slotId: Int, mouseButton: Int, actionType: ClickType)
}

fun interface ClickAction {
    operator fun invoke(handler: AbstractContainerMenu, slot: Slot, callback: MouseClick)
}

/**
 * Quick item movement
 *
 * @author sqlerrorthing
 */
object ModuleItemScroller : ClientModule("ItemScroller", ModuleCategories.MISC) {
    @JvmStatic
    val clickMode by enumChoice("ClickMode", ClickMode.QUICK_MOVE)

    private val delay by intRange("Delay", 2..3, 0..20, suffix = "ticks")

    private val chronometer = Chronometer()

    fun resetChronometer() {
        chronometer.reset()
    }

    fun canPerformScroll(window: Window): Boolean {
        return (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT))
                && this.running
                && GLFW.glfwGetMouseButton(window.handle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS
                && chronometer.hasAtLeastElapsed(delay.random() * 50L)
    }
}

@Suppress("UNUSED")
enum class ClickMode(
    override val tag: String,
    val action: ClickAction
) : Tagged {
    QUICK_MOVE("QuickMove", { _, slot, callback ->
        callback(slot, slot.index, GLFW.GLFW_MOUSE_BUTTON_LEFT, ClickType.QUICK_MOVE)
    })
}
