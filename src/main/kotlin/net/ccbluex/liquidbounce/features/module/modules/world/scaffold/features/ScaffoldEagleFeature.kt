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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

object ScaffoldEagleFeature : ToggleableConfigurable(ModuleScaffold, "Eagle", false) {

    private val blocksToEagle by int("BlocksToEagle", 0, 0..10)
    private val edgeDistance by float("EagleEdgeDistance", 0.01f, 0.01f..1.3f)

    // Makes you sneak until first block placed, so with eagle enabled you won't fall off, when enabled
    private var placedBlocks = 0

    val stateUpdateHandler =
        handler<MovementInputEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
            val player = player

            if (ScaffoldDownFeature.shouldFallOffBlock()) {
                return@handler
            }
            if (!player.isOnGround) {
                return@handler
            }

            val shouldBeActive = !player.abilities.flying && placedBlocks == 0

            if (shouldBeActive && player.isCloseToEdge(it.directionalInput, edgeDistance.toDouble())) {
                it.sneaking = true
            }
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
