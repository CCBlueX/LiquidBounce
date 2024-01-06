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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.TickJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.player

object ScaffoldTowerFeature : ToggleableConfigurable(ModuleScaffold, "Tower", false) {
    private val mode by enumChoice("Mode", TowerModes.JUMP, TowerModes.values())

    private val jumpTimer = Chronometer()

    private var shouldJump = false

    val doShit = handler<MovementInputEvent> { event ->
        shouldJump = false

        if (ModuleScaffold.sameY) {
            return@handler
        }

        if (player.isOnGround) {
            shouldJump = event.jumping
        }

        event.jumping = false
    }

    val jumpHandler = handler<TickJumpEvent> {
        if (!this.shouldJump) {
            return@handler
        }

        if ((mode == TowerModes.JUMP || mode == TowerModes.MOTION) && player.isOnGround && player.velocity.y <= 0.0) {
            player.jump()

            this.jumpTimer.reset()
        }
    }

    val tickHandler = handler<GameTickEvent> {
        if (mode == TowerModes.MOTION && player.velocity.y < 0.1 && !this.jumpTimer.hasElapsed(500)) {
            player.velocity.y = -0.3
        }
    }

    enum class TowerModes(override val choiceName: String) : NamedChoice {
        JUMP("Jump"), MOTION("Motion")
    }

}
