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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoWeapon
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.wouldBlockHit
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

object KillAuraTargetTracker : TargetTracker() {

    /**
     * What to do with a target whose shield would block the attack: attack it anyway, do not target it,
     * or keep it as the target and hold the attacks back until the shield is down.
     */
    private val shield by enumChoice("Shield", ShieldHandling.IGNORE, aliases = listOf("IgnoreShield"))

    override fun validate(entity: LivingEntity): Boolean {
        return super.validate(entity) && (shield != ShieldHandling.AVOID || !wouldBlockAttack(entity))
    }

    /**
     * Whether attacks on [entity] are held back until its shield is down.
     */
    fun skipsAttack(entity: Entity) = shield == ShieldHandling.SKIP && wouldBlockAttack(entity)

    /**
     * Check if the entity is holding a shield and if the shield would block the attack.
     */
    private fun wouldBlockAttack(entity: Entity): Boolean {
        if (entity !is Player || isOlderThanOrEqual1_8) {
            return false
        }

        if (player.mainHandItem.isAxe || ModuleAutoWeapon.willShieldBreak) {
            return false
        }

        return entity.wouldBlockHit
    }

    /**
     * The aliases read the boolean `IgnoreShield` of older configs.
     */
    private enum class ShieldHandling(override val tag: String, override val tagAliases: List<String>) : Tagged {
        IGNORE("Ignore", listOf("true")),
        AVOID("Avoid", listOf("false")),
        SKIP("Skip", emptyList())
    }

}
