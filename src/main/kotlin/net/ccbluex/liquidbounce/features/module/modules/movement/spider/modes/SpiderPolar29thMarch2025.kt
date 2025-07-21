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
package net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider
import net.ccbluex.liquidbounce.utils.block.shrink

/**
 * Shrinks the block collision shape and allows you to walk on it.
 * Might not work on every surface.
 *
 * @testedOn pika.host
 * @anticheat Polar
 */
internal object SpiderPolar29thMarch2025 : Choice("Polar-29.03.2025") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpider.modes

    /**
     * Polar allows jumping 0.6 high, but it's faster to use 0.55 to hit the
     * block collision shape.
     */
    private val jumpHeight by float("JumpHeight", 0.55f, 0.42f..0.6f)

    @Suppress("unused")
    private val boxHandler = handler<BlockShapeEvent> { event ->
        if (event.pos.y >= player.pos.y || player.isSneaking && player.isOnGround) {
            event.shape = event.shape.shrink(
                x = 0.0001,
                z = 0.0001
            )
        }
    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> { event ->
        var highJump = jumpHeight
        if (player.horizontalCollision && highJump > 0.42f) {
            event.motion = highJump
        }
    }

}
