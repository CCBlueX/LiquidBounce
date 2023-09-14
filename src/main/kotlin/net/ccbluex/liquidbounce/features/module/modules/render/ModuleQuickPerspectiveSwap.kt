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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

object ModuleQuickPerspectiveSwap : Module("QuickPerspectiveSwap", Category.RENDER) {

    private val onUpdate = handler<WorldRenderEvent> {
        if (!InputUtil.isKeyPressed(mc.window.handle, bind)) {
            this.enabled = false
        }
    }

    override fun enable() {
        if (this.bind == GLFW.GLFW_KEY_UNKNOWN) {
            chat("You cannot use this module without a keybind as it will disable when the keybind isn't held anymore")

            this.enabled = false
        }
    }

}
