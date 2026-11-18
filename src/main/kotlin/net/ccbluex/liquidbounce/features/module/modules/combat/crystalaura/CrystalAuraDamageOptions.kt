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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import it.unimi.dsi.fastutil.floats.FloatFloatImmutablePair
import it.unimi.dsi.fastutil.floats.FloatFloatPair
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura.player
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.ModuleCrystalAura.world
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer
import net.ccbluex.liquidbounce.utils.collection.LruCache
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.entity.armorItems
import net.ccbluex.liquidbounce.utils.entity.getActualHealth
import net.ccbluex.liquidbounce.utils.entity.getDamageFromExplosion
import net.ccbluex.liquidbounce.utils.input.isPressed
import net.ccbluex.liquidbounce.utils.item.durability
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3

object CrystalAuraDamageOptions : ValueGroup("Damage") {

    private const val MAX_DAMAGE_SETTING = 36.0F

    private val maxSelfDamage by float("MaxSelfDamage", 12.0F, 0.0F..MAX_DAMAGE_SETTING)
    private val maxFriendDamage by float("MaxFriendDamage", 6.0F, 0.0F..MAX_DAMAGE_SETTING)
    private val minEnemyDamage by float("MinEnemyDamage", 4.5F, 0.0F..MAX_DAMAGE_SETTING)

    /**
     * Won't place / break crystals that would kill us.
     */
    private val antiSuicide by boolean("AntiSuicide", true)

    /**
     * Always allows crystals that would pop or kill the target, provided self-damage constraints pass.
     */
    private val forceLethal by boolean("ForceLethal", true)

    /**
     * Only places / breaks crystals that deal more damage to the enemy than to us.
     */
    val efficient by boolean("Efficient", true)

    /**
     * Doesn't include blocks that will get blown away in the exposure calculation used for the damage calculation.
     */
    val terrain by boolean("Terrain", true)

    /**
     * Lowers minimum required damage when the enemy is low, has low armor durability, or a bind is held.
     */
    object FacePlace : ToggleableValueGroup(ModuleCrystalAura, "FacePlace", true) {
        val health by float("Health", 8.0F, 0.0F..MAX_DAMAGE_SETTING)
        val armorDurability by int("ArmorDurability", 15, 0..100, "%")
        val noArmor by boolean("NoArmor", false)
        val minDamage by float("MinDamage", 1.5F, 0.1F..10.0F)
        val forceBind by key("ForceBind")

        fun shouldFacePlace(target: LivingEntity): Boolean {
            if (!enabled) {
                return false
            }

            if (forceBind.isPressed || target.getActualHealth() + target.absorptionAmount <= health) {
                return true
            }

            val armor = target.armorItems
            if (noArmor && armor.any { it.isEmpty }) {
                return true
            }

            return armorDurability > 0 && armor.any { stack ->
                !stack.isEmpty && stack.maxDamage > 0 && (stack.durability * 100) / stack.maxDamage <= armorDurability
            }
        }
    }

    init {
        tree(FacePlace)
    }

    val cacheMap = LruCache<DamageConstellation, DamageProvider>(64)

    /**
     * Approximates how favorable an explosion of a crystal at [pos] in a given [world] would be.
     *
     * The first float is the self-damage, the second is the enemy damage.
     */
    internal fun approximateExplosionDamage(pos: Vec3, requestingSubmodule: RequestingSubmodule): FloatFloatPair? {
        val target = targetTracker.target ?: return null
        val damageToTarget = target.getDamage(pos, requestingSubmodule, CheckedEntity.TARGET)
        val targetHealth = target.getActualHealth() + target.absorptionAmount
        val isLethal = forceLethal && damageToTarget.isGreaterThanOrEqual(targetHealth)

        val requiredDamage = if (FacePlace.shouldFacePlace(target)) {
            FacePlace.minDamage.coerceAtMost(minEnemyDamage)
        } else {
            minEnemyDamage
        }

        if (!isLethal && damageToTarget.isSmallerThan(requiredDamage)) {
            return null
        }

        val selfDamage = player.getDamage(pos, requestingSubmodule, CheckedEntity.SELF)
        val willKill = antiSuicide && selfDamage.isAnyGreaterThanOrEqual(player.health + player.absorptionAmount)
        val tooMuchDamage = selfDamage.isAnyGreaterThan(maxSelfDamage)
        if (willKill || tooMuchDamage) {
            return null
        }

        val isNotEfficient = !isLethal && efficient && damageToTarget.isSmallerThanOrEqual(selfDamage)
        if (isNotEfficient || isFriendDamagedTooMuch(pos, requestingSubmodule)) {
            return null
        }

        return FloatFloatImmutablePair(selfDamage.getFixed(), damageToTarget.getFixed())
    }

    private fun isFriendDamagedTooMuch(pos: Vec3, requestingSubmodule: RequestingSubmodule): Boolean {
        if (maxFriendDamage >= MAX_DAMAGE_SETTING) { // MAX_DAMAGE_SETTING is the maximum allowed by the setting
            return false
        }

        val friends = world.getEntitiesBoxInRange(pos, 12.0) {
            FriendManager.isFriend(it) && it.boundingBox.maxY > pos.y
        }

        return friends.any {
            it is LivingEntity &&
                it.getDamage(pos, requestingSubmodule, CheckedEntity.OTHER).isAnyGreaterThan(maxFriendDamage)
        }
    }

    private fun LivingEntity.getDamage(
        crystal: Vec3,
        requestingSubmodule: RequestingSubmodule,
        checkedEntity: CheckedEntity
    ): DamageProvider {
        val damageConstellation = DamageConstellation(this, blockPosition(), crystal, requestingSubmodule)
        val calc: (DamageConstellation) -> DamageProvider = {
            val excludeNotBlastResistant = terrain &&
                (!requestingSubmodule.basePlace || SubmoduleBasePlace.terrain)
            checkedEntity.getDamage(
                entity = this,
                requestingSubmodule = requestingSubmodule,
                crystal = crystal,
                maxBlastResistance = if (excludeNotBlastResistant) 9f else null,
                include = if (requestingSubmodule.basePlace) BlockPos.containing(crystal).below() else null
            )
        }

        return if (CrystalAuraTriggerer.canCache()) {
            cacheMap.computeIfAbsent(damageConstellation, calc)
        } else {
            calc(damageConstellation)
        }
    }

    @JvmRecord
    data class DamageConstellation(
        val entity: LivingEntity,
        val pos: BlockPos,
        val crystal: Vec3,
        val requestingSubmodule: RequestingSubmodule // TODO optimize the cache, so that it caches the damage values
        // itself
    )

    enum class RequestingSubmodule(val basePlace: Boolean) {
        PLACE(false),
        DESTROY(false),
        BASE_PLACE(true);
    }

    private enum class CheckedEntity {

        SELF {
            override fun getDamage(
                entity: LivingEntity,
                requestingSubmodule: RequestingSubmodule,
                crystal: Vec3,
                maxBlastResistance: Float?,
                include: BlockPos?
            ): DamageProvider {
                val ticks = when (requestingSubmodule) {
                    RequestingSubmodule.PLACE -> SelfPredict.placeTicks
                    RequestingSubmodule.DESTROY -> SelfPredict.destroyTicks
                    RequestingSubmodule.BASE_PLACE -> SelfPredict.basePlaceTicks
                }

                return SelfPredict.getDamage(
                    player = entity as Player,
                    ticks = ticks,
                    crystal = crystal,
                    maxBlastResistance = maxBlastResistance,
                    include = include
                )
            }
        },
        TARGET {
            override fun getDamage(
                entity: LivingEntity,
                requestingSubmodule: RequestingSubmodule,
                crystal: Vec3,
                maxBlastResistance: Float?,
                include: BlockPos?
            ): DamageProvider {
                if (entity !is Player) {
                    return OTHER.getDamage(
                        entity = entity,
                        requestingSubmodule = requestingSubmodule,
                        crystal = crystal,
                        maxBlastResistance = maxBlastResistance,
                        include = include
                    )
                }

                val ticks = when (requestingSubmodule) {
                    RequestingSubmodule.PLACE -> TargetPredict.placeTicks
                    RequestingSubmodule.DESTROY -> TargetPredict.destroyTicks
                    RequestingSubmodule.BASE_PLACE -> TargetPredict.basePlaceTicks
                }

                return TargetPredict.getDamage(
                    player = entity,
                    ticks = ticks,
                    crystal = crystal,
                    maxBlastResistance = maxBlastResistance,
                    include = include
                )
            }
        },
        OTHER {
            override fun getDamage(
                entity: LivingEntity,
                requestingSubmodule: RequestingSubmodule,
                crystal: Vec3,
                maxBlastResistance: Float?,
                include: BlockPos?
            ): DamageProvider {
                return NormalDamageProvider(entity.getDamageFromExplosion(
                    pos = crystal,
                    include = include,
                    maxBlastResistance = maxBlastResistance
                ))
            }
        };

        abstract fun getDamage(
            entity: LivingEntity,
            requestingSubmodule: RequestingSubmodule,
            crystal: Vec3,
            maxBlastResistance: Float? = null,
            include: BlockPos? = null
        ): DamageProvider

    }

}
