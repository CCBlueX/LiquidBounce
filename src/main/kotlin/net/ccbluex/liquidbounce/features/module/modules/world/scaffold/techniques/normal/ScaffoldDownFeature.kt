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
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention

object ScaffoldDownFeature : ToggleableConfigurable(ScaffoldNormalTechnique, "Down", false) {

    val handleMoveInput = handler<MovementInputEvent>(priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING) {
        if (shouldFallOffBlock()) {
            it.sneaking = false
        }
    }

    @Suppress("unused")
    val handleSafeWalk = handler<PlayerSafeWalkEvent>(priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING) {
        if (shouldFallOffBlock()) {
            it.isSafeWalk = false
        }
    }

    val shouldGoDown: Boolean
        get() = enabled && mc.options.sneakKey.isPressed

    /**
     * When we are using the down scaffold, we want to jump down on the next block in some situations
     */
    internal fun shouldFallOffBlock() = shouldGoDown && player.blockPos.add(0, -2, 0).canStandOn()
}
