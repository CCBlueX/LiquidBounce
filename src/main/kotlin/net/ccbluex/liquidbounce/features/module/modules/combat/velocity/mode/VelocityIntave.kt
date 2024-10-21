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
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity.modes
import net.minecraft.client.gui.screen.ingame.InventoryScreen

object VelocityIntave : Choice("Intave") {
    override val parent: ChoiceConfigurable<Choice>
        get() = modes

    private var jumpChance by float("JumpChance", 50f, 0f..100f, "%")

    private class ReduceOnAttack(parent: Listenable?) : ToggleableConfigurable(
        parent, "ReduceOnAttack",
        true
    ) {

        private val reduceFactor by float("Factor", 0.6f, 0.6f..1f)
        private val hurtTime by int("HurtTime", 9, 1..10)
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

    @Suppress("unused")
    private val repeatable = repeatable {
        val shouldJump = Math.random() * 100 < jumpChance && player.hurtTime < 10
        val canJump = player.isOnGround && mc.currentScreen !is InventoryScreen

        if (shouldJump && canJump) {
            player.jump()
        }
    }
}
