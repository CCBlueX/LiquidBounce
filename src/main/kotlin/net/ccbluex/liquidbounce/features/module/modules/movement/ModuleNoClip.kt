/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatableSequence

object ModuleNoClip : Module("NoClip", Category.MOVEMENT) {
    override fun disable() {
        player.noClip = false
    }

    val repeatable = repeatableSequence {
        player.noClip = true
        player.fallDistance = 0f
        player.isOnGround = false
        player.abilities.flying = false
        player.setVelocity(0.0, 0.0, 0.0)
        val speed = 0.32f
        player.flyingSpeed = speed

        if(mc.options.keyJump.isPressed) {
            player.setVelocity(player.velocity.x, player.velocity.z + speed, player.velocity.z)
        }
        if(mc.options.keySneak.isPressed) {
            player.setVelocity(player.velocity.x, player.velocity.z - speed, player.velocity.z)
        }
    }
}

