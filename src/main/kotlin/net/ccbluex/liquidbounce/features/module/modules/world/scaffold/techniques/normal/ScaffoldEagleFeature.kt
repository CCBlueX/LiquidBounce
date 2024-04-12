/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput

object ScaffoldEagleFeature : ToggleableConfigurable(ScaffoldNormalTechnique, "Eagle", false) {

    private val blocksToEagle by int("BlocksToEagle", 0, 0..10)
    private val edgeDistance by float("EdgeDistance", 0.01f, 0.01f..1.3f)

    // Makes you sneak until first block placed, so with eagle enabled you won't fall off, when enabled
    private var placedBlocks = 0

    val stateUpdateHandler =
        handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
            if (shouldEagle(it.directionalInput)) {
                it.sneaking = true
            }
        }

    fun shouldEagle(input: DirectionalInput): Boolean {
        if (ScaffoldDownFeature.shouldFallOffBlock()) {
            return false
        }

        if (!player.isOnGround) {
            return false
        }

        val shouldBeActive = !player.abilities.flying && placedBlocks == 0

        return shouldBeActive && player.isCloseToEdge(input, edgeDistance.toDouble())
    }

    fun onBlockPlacement() {
        if (!enabled) {
            return
        }

        placedBlocks += 1

        if (placedBlocks > blocksToEagle) {
            placedBlocks = 0
        }
    }

}
