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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.AxeItem

object KillAuraTargetTracker : TargetTracker() {

    /**
     * Allows to ignore when the target is holding a shield,
     * which would normally block attacks.
     */
    private val ignoreShield by boolean("IgnoreShield", true)

    override fun validate(entity: LivingEntity): Boolean {
        return super.validate(entity) && validateShield(entity)
    }

    /**
     * Check if the entity is holding a shield and if the shield would block the attack.
     */
    private fun validateShield(entity: LivingEntity): Boolean {
        if (ignoreShield || entity !is PlayerEntity || isOlderThanOrEqual1_8) {
            return true
        }

        if (player.mainHandStack.item is AxeItem || ModuleAutoWeapon.willBreakShield()) {
            return true
        }

        return !entity.blockedByShield(world.damageSources.playerAttack(player)) || !entity.wouldBlockHit(player)
    }

}
