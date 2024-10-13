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
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.modes

object VelocityIntave : Choice("Intave") {

    private class ReduceOnAttack(parent: Listenable?) : ToggleableConfigurable(parent, "ReduceOnAttack",
        true) {

        private val reduceFactor by float("Factor", 0.97f, 0.6f..1f)
        private val hurtTime by int("HurtTime", 9, 1..9)
        var lastAttackTime = 0L

        @Suppress("unused")
        private val attackHandler = handler<AttackEvent> {
            if (player.hurtTime == hurtTime && System.currentTimeMillis() - lastAttackTime <= 8000) {
                player.velocity.x *= reduceFactor
                player.velocity.z *= reduceFactor
            }
            lastAttackTime = System.currentTimeMillis()
        }

    }

    init {
        tree(ReduceOnAttack(this))
    }

    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    private var intaveTick = 0
    private var intaveDamageTick = 0

    @Suppress("unused")
    private val repeatable = repeatable {
        intaveTick++

        if (player.hurtTime == 2) {
            intaveDamageTick++
            if (player.isOnGround && intaveTick % 2 == 0 && intaveDamageTick <= 10) {
                player.jump()
                intaveTick = 0
            }
        }
    }
}
