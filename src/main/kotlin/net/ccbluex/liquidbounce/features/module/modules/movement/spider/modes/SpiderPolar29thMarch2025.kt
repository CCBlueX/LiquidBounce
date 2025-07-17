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

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.BlockShapeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider
import net.ccbluex.liquidbounce.utils.block.shrink
import net.ccbluex.liquidbounce.utils.math.copy

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

    private val fast by boolean("Fast", true)

    @Suppress("unused")
    private val boxHandler = handler<BlockShapeEvent> { event ->
        if (event.pos.y >= player.pos.y || player.isSneaking && player.isOnGround) {
            event.shape = event.shape.shrink(
                x = 0.0001,
                z = 0.0001
            )
        }
    }

    /**
     * Could also count as 2-Block Step. Probably could be abused further
     * to go even faster.
     */
    @Suppress("unused")
    private val fastHandler = tickHandler {
        if (fast && player.horizontalCollision && player.isOnGround) {
            player.velocity = player.velocity.copy(y = 0.6)
        }
    }

}
