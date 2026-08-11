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
package net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove.movementKeys
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove.shouldHandleInputs
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.isInInventoryScreen
import net.ccbluex.liquidbounce.utils.network.sendCloseInventory
import net.minecraft.client.input.KeyEvent
import org.lwjgl.glfw.GLFW

object InventoryMoveSafeFeature : ToggleableValueGroup(ModuleInventoryMove, "Safe", false) {

    @Suppress("unused")
    private val keyHandler = handler<KeyboardKeyEvent> { event ->
        val key = movementKeys.keys.find { it.matches(KeyEvent(event.keyCode, event.scanCode, event.mods)) }
            ?: return@handler
        val pressed = shouldHandleInputs(key) && !event.isReleased
        movementKeys.put(key, pressed)

        if (isInInventoryScreen && InventoryManager.isInventoryOpenServerSide && pressed) {
            network.sendCloseInventory()
        }
    }

}
