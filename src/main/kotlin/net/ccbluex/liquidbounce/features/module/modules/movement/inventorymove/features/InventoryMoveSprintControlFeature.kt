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
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

object InventoryMoveSprintControlFeature : ToggleableConfigurable(ModuleInventoryMove, "SprintControl", false) {

    private val clientMode by enumChoice("Client", SprintMode.DO_NOT_CHANGE)
    private val serverMode by enumChoice("Server", SprintMode.DO_NOT_CHANGE)

    private enum class SprintMode(override val choiceName: String) : NamedChoice {

        /**
         * This can be used to not change the sprint state.
         */
        DO_NOT_CHANGE("DoNotChange"),

        /**
         * This can be used to force sprinting on Scaffold,
         * while not allowing to sprint omnidirectional
         * when Scaffold is not active.
         */
        FORCE_SPRINT("ForceSprint"),

        /**
         * This can be used to disable sprinting on Scaffold,
         * while still allowing to sprint omnidirectional
         * when Scaffold is not active.
         */
        FORCE_NO_SPRINT("ForceNoSprint"),

    }

    override val running: Boolean
        get() = super.running && InventoryManager.isInventoryOpen

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(
        priority = EventPriorityConvention.MODEL_STATE
    ) { event ->
        // Movement Tick will affect the client-side sprint state,
        // while we also apply it to Input to count as pressing the Sprint-Key
        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            when (clientMode) {
                SprintMode.FORCE_SPRINT -> if (event.directionalInput.isMoving) {
                    event.sprint = true
                }

                SprintMode.FORCE_NO_SPRINT -> {
                    event.sprint = false
                }

                SprintMode.DO_NOT_CHANGE -> { }

            }
        }

        // Network and Input both count as Network Type
        // which will make the server think we are not sprinting
        if (event.source == SprintEvent.Source.NETWORK || event.source == SprintEvent.Source.INPUT) {
            when (serverMode) {

                SprintMode.FORCE_SPRINT -> if (event.directionalInput.isMoving) {
                    event.sprint = true
                }

                SprintMode.FORCE_NO_SPRINT -> {
                    event.sprint = false
                }

                SprintMode.DO_NOT_CHANGE -> { }
            }
        }
    }

}
