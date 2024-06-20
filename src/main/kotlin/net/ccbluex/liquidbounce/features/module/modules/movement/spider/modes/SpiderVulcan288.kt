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
package net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider
import net.minecraft.block.Blocks

/**
 * Spider Bypass for Vulcan 2.8.8
 *
 * Sneaking seems to reduce flags a bit for some reason.
 *
 * @anticheat Vulcan 2.8.8
 * @testedOn eu.loyisa.cn
 * @see net.ccbluex.liquidbounce.features.module.modules.movement.spider.ModuleSpider
 *
 * TODO: Detection for how many blocks you've gone up. Anything over 40ish seems to flag for Invalid (C)
 *   Proper implementation if there's something above you needs to be added.
 */
internal object SpiderVulcan288 : Choice("Vulcan288") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpider.modes

    private var pause = false

    val repeatable = repeatable {
        // todo: can we use player.isClimbing instead?
        val isOnLadder = player.blockPos.let { pos ->
            val block = world.getBlockState(pos).block
            block == Blocks.LADDER || block == Blocks.VINE
        }

        if (player.horizontalCollision && !isOnLadder) {
            pause = true
            waitTicks(2)
            player.velocity.y = 9.6599696
            waitTicks(2)
            player.velocity.y = 0.0001
        }

        if (player.horizontalCollision && !isOnLadder) {
            player.velocity.x = 0.0
            player.velocity.z = 0.0
            player.input.sneaking = true
        }

        if (pause && !player.horizontalCollision) {
            player.velocity.y = 0.0
            pause = false
        }
    }

}
