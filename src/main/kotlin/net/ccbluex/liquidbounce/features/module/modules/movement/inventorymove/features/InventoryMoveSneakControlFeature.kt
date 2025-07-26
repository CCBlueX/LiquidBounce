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
package net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SneakNetworkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

object InventoryMoveSneakControlFeature : ToggleableConfigurable(ModuleInventoryMove, "SneakControl", false) {

    private val clientMode by enumChoice("Client", SneakMode.DO_NOT_CHANGE)
    private val serverMode by enumChoice("Server", SneakMode.DO_NOT_CHANGE)

    private enum class SneakMode(override val choiceName: String) : NamedChoice {

        /**
         * This can be used to not change the sprint state.
         */
        DO_NOT_CHANGE("DoNotChange"),

        /**
         * This can be used to force sneaking on Scaffold,
         * while not allowing to sprint omnidirectional
         * when Scaffold is not active.
         */
        FORCE_SNEAK("ForceSneak"),

        /**
         * This can be used to disable sneaking on Scaffold,
         * while still allowing to sprint omnidirectional
         * when Scaffold is not active.
         */
        FORCE_NO_SNEAK("ForceNoSneak"),

    }

    override val running: Boolean
        get() = super.running && InventoryManager.isInventoryOpen

    @Suppress("unused")
    private val sneakInputHandler = handler<MovementInputEvent>(
        priority = EventPriorityConvention.MODEL_STATE
    ) { event ->
        when (clientMode) {
            SneakMode.FORCE_SNEAK -> {
                event.sneak = true
            }
            SneakMode.FORCE_NO_SNEAK -> {
                event.sneak = false
            }
            SneakMode.DO_NOT_CHANGE -> { }
        }
    }

    @Suppress("unused")
    private val sneakNetworkHandler = handler<SneakNetworkEvent>(
        priority = EventPriorityConvention.MODEL_STATE
    ) { event ->
        when (serverMode) {
            SneakMode.FORCE_SNEAK -> {
                event.sneak = true
            }
            SneakMode.FORCE_NO_SNEAK -> {
                event.sneak = false
            }
            SneakMode.DO_NOT_CHANGE -> { }
        }
    }

}
