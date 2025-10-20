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
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.client.util.InputUtil
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import org.lwjgl.glfw.GLFW

fun interface MouseClick {
    operator fun invoke(callbackSlot: Slot?, slotId: Int, mouseButton: Int, actionType: SlotActionType)
}

fun interface ClickAction {
    operator fun invoke(handler: ScreenHandler, slot: Slot, callback: MouseClick)
}

/**
 * Quick item movement
 *
 * @author sqlerrorthing
 */
object ModuleItemScroller : ClientModule("ItemScroller", Category.MISC) {
    @JvmStatic
    val clickMode by enumChoice("ClickMode", ClickMode.QUICK_MOVE)

    private val delay by intRange("Delay", 2..3, 0..20, suffix = "ticks")

    private val chronometer = Chronometer()

    fun resetChronometer() {
        chronometer.reset()
    }

    fun canPerformScroll(handle: Long): Boolean {
        return (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT)
                        || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT))
                && this.running
                && GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS
                && chronometer.hasAtLeastElapsed(delay.random() * 50L);
    }
}

@Suppress("UNUSED")
enum class ClickMode(
    override val choiceName: String,
    val action: ClickAction
) : NamedChoice {
    QUICK_MOVE("QuickMove", { _, slot, callback ->
        callback(slot, slot.id, GLFW.GLFW_MOUSE_BUTTON_LEFT, SlotActionType.QUICK_MOVE)
    })
}
