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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.modes
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.pause

/**
 *
 * Velocity for AAC4.4.2, pretty sure, it works on other versions
 */

internal object VelocityAAC442 : Choice("AAC4.4.2") {

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    val aac442MotionReducer by float("AAC4.4.2MotionReducer", 0.62f, 0f..1f)

    val repeatable = repeatable {
        if (player.hurtTime > 0 && !player.isOnGround) {
            val reduce = aac442MotionReducer
            player.velocity.x *= reduce
            player.velocity.z *= reduce
        }
    }

    override fun handleEvents() = super.handleEvents() && pause == 0

}
