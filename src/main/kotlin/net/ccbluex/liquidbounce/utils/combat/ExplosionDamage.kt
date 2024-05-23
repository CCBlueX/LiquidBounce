/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.combat

import net.ccbluex.liquidbounce.features.module.modules.combat.crystalAura.ModuleCrystalAura.mc
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalAura.ModuleCrystalAura.world
import net.ccbluex.liquidbounce.utils.entity.getEffectiveDamage
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.explosion.Explosion
import kotlin.math.floor
import kotlin.math.sqrt

fun getDamageFromExplosion(
    pos: Vec3d,
    possibleVictim: LivingEntity,
    power: Float = 6.0F,
): Float {
    val explosionRange = power * 2.0F

    val distanceDecay = 1.0F - sqrt(possibleVictim.squaredDistanceTo(pos).toFloat()) / explosionRange
    val pre1 = Explosion.getExposure(pos, possibleVictim) * distanceDecay

    val preprocessedDamage = floor((pre1 * pre1 + pre1) / 2.0F * 7.0F * explosionRange + 1.0F)

    val explosion =
        Explosion(
            possibleVictim.world,
            null,
            pos.x,
            pos.y,
            pos.z,
            power,
            false,
            world.getDestructionType(GameRules.BLOCK_EXPLOSION_DROP_DECAY),
        )

    return possibleVictim.getEffectiveDamage(mc.world!!.damageSources.explosion(explosion), preprocessedDamage)
}
