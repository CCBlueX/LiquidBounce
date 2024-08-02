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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.isBlockBelow
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.towerMode
import net.minecraft.stat.Stats
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlin.math.truncate

object ScaffoldTowerMotion : Choice("Motion") {

    private val motion by float("Motion", 0.42f, 0.0f..1.0f)
    private val triggerHeight by float("TriggerHeight", 0.78f, 0.76f..1.0f)
    private val slow by float("Slow", 1.0f, 0.0f..3.0f)

    /**
     * The position where the player jumped off
     */
    private var jumpOffPosition = Optional.empty<Double>()

    override val parent: ChoiceConfigurable<Choice>
        get() = towerMode

    val jumpEvent = handler<PlayerJumpEvent> {
        jumpOffPosition = Optional.of(player.y)
    }

    val repeatable = repeatable {
        if (!mc.options.jumpKey.isPressed || ModuleScaffold.blockCount <= 0 || !isBlockBelow) {
            jumpOffPosition = Optional.empty()
            return@repeatable
        }

        if (player.y > (jumpOffPosition.getOrNull() ?: return@repeatable) + triggerHeight) {
            player.setPosition(player.x, truncate(player.y), player.z)

            player.velocity.y = motion.toDouble()
            player.velocity = player.velocity.multiply(
                slow.toDouble(),
                1.0,
                slow.toDouble()
            )
            player.incrementStat(Stats.JUMP)

            jumpOffPosition = Optional.of(player.y)
        }
    }

}
