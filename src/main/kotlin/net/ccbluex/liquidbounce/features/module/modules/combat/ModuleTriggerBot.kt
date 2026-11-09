/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.minecraft.world.phys.EntityHitResult

/**
 * TriggerBot module
 *
 * Attacks the enemy under the crosshair without holding the attack button.
 */
object ModuleTriggerBot : ClientModule("TriggerBot", ModuleCategories.COMBAT) {

    val clicker = tree(Clicker(this, mc.options.keyAttack, simulateAttackKeyDown = true))

    private val attacker = CrosshairAttacker(this, clicker) {
        val crosshair = mc.hitResult

        // A second clicker on the same attack key would double the CPS
        crosshair is EntityHitResult && crosshair.entity.shouldBeAttacked()
            && !ModuleAutoClicker.AttackButton.isAttacking
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (attacker.isAttacking) {
            attacker.attack()
        }
    }

}
