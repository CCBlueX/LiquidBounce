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
package net.ccbluex.liquidbounce.features.module.modules.world.crystalAura

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.getEffectiveDamage
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameRules
import net.minecraft.world.explosion.Explosion
import kotlin.math.floor
import kotlin.math.sqrt

object ModuleCrystalAura : Module("CrystalAura", Category.WORLD) {
    val swing by boolean("Swing", true)

    internal object PlaceOptions : ToggleableConfigurable(this, "Place", true) {
        val range by float("Range", 4.5F, 1.0F..5.0F)
        val minEfficiency by float("MinEfficiency", 0.1F, 0.0F..5.0F)
    }

    internal object DestroyOptions : ToggleableConfigurable(this, "Destroy", true) {
        val range by float("Range", 4.5F, 1.0F..5.0F)
        val minEfficiency by float("MinEfficiency", 0.1F, 0.0F..5.0F)
    }

    internal object SelfPreservationOptions : ToggleableConfigurable(this, "SelfPreservation", true) {
        val selfDamageWeight by float("SelfDamageWeight", 2.0F, 0.0F..10.0F)
        val friendDamageWeight by float("FriendDamageWeight", 1.0F, 0.0F..10.0F)
    }

    // Rotation
    internal val rotations = tree(RotationsConfigurable())

    init {
        tree(PlaceOptions)
        tree(DestroyOptions)
        tree(SelfPreservationOptions)
    }

    val networkTickHandler =
        repeatable {
            // Make the crystal placer run
            SubmoduleCrystalPlacer.tick()
            // Make the crystal destroyer run
            SubmoduleCrystalDestroyer.tick()
        }

    /**
     * Approximates how favorable an explosion of a crystal at [pos] in a given [world] would be
     */
    internal fun approximateExplosionDamage(
        world: ClientWorld,
        pos: Vec3d,
    ): Double {
        val possibleVictims =
            world
                .getEntitiesBoxInRange(pos, 6.0) { shouldTakeIntoAccount(it) && it.boundingBox.maxY > pos.y }
                .filterIsInstance<LivingEntity>()

        var totalGood = 0.0
        var totalHarm = 0.0

        for (possibleVictim in possibleVictims) {
            val dmg = getDamageFromExplosion(pos, possibleVictim) * entityDamageWeight(possibleVictim)

            if (dmg > 0) {
                totalGood += dmg
            } else {
                totalHarm += dmg
            }
        }

        return totalGood + totalHarm
    }

    private fun shouldTakeIntoAccount(entity: Entity): Boolean {
        return entity.shouldBeAttacked() || entity == player || FriendManager.isFriend(entity)
    }

    private fun getDamageFromExplosion(
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
                mc.world!!.getDestructionType(GameRules.BLOCK_EXPLOSION_DROP_DECAY),
            )

        return possibleVictim.getEffectiveDamage(mc.world!!.damageSources.explosion(explosion), preprocessedDamage)
    }

    private fun entityDamageWeight(entity: Entity): Double {
        if (!SelfPreservationOptions.enabled) {
            return 1.0
        }

        return when {
            entity == player -> -SelfPreservationOptions.selfDamageWeight.toDouble()
            FriendManager.isFriend(entity) -> -SelfPreservationOptions.friendDamageWeight.toDouble()
            else -> 1.0
        }
    }
}
