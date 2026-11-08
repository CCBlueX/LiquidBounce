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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.utils.percentageChance
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.combat.findEnemy

object ModuleNoMissCooldown : ClientModule("NoMissCooldown", ModuleCategories.COMBAT) {

    /**
     * Disables the miss-cooldown of 10 ticks when missing an attack.
     */
    val removeAttackCooldown by boolean("RemoveAttackCooldown", true)

    /**
     * Cancels the attack when missing an attack.
     */
    val cancelAttackOnMiss by boolean("CancelAttackOnMiss", false)

    /**
     * Hides the swing of a missed attack from the client, the server or both.
     */
    val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)

    /**
     * Chance of an attack being changed. The other attacks stay vanilla.
     */
    private val chance = percentageChance("Chance", 100f)
    private var chancePassed = true

    /**
     * Only changes attacks while an enemy is within the range.
     */
    private object OnlyNearEnemy : ToggleableValueGroup(this, "OnlyNearEnemy", false) {
        val range by float("Range", 6f, 0f..10f, "blocks")
    }

    init {
        tree(OnlyNearEnemy)
    }

    private var enemyNear = true

    val changesAttack
        get() = running && chancePassed && enemyNear

    /**
     * Rolls the [chance] and looks for an enemy for the attack that is about to start.
     */
    fun startAttack() {
        // A miss that stayed vanilla keeps its cooldown, a new roll would let the next click skip it
        if (chancePassed || mc.missTime <= 0) {
            chancePassed = chance.asBoolean
        }
        enemyNear = !OnlyNearEnemy.running || world.findEnemy(0f, OnlyNearEnemy.range) != null
    }

}
