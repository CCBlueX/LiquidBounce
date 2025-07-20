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
@file:Suppress("TooManyFunctions")
package net.ccbluex.liquidbounce.utils.combat

import it.unimi.dsi.fastutil.objects.ObjectDoublePair
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.features.module.modules.client.ModuleTargets
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.mob.Angerable
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.mob.WaterCreatureEntity
import net.minecraft.entity.passive.PassiveEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameMode
import java.util.*
import java.util.function.Predicate

/**
 * Global target configurable
 *
 * Modules can have their own enemy configurable if required. If not, they should use this as default.
 * Global enemy configurable can be used to configure which entities should be considered as a target.
 *
 * This can be adjusted by the .target command and the panel inside the ClickGUI.
 */
data class EntityTargetingInfo(val classification: EntityTargetClassification, val isFriend: Boolean) {
    companion object {
        val DEFAULT = EntityTargetingInfo(EntityTargetClassification.TARGET, false)
    }
}

enum class EntityTargetClassification {
    TARGET,
    INTERESTING,
    IGNORED
}

/**
 * Configurable to configure which entities and their state (like being dead) should be considered as a target
 */
enum class Targets(override val choiceName: String) : NamedChoice {
    PLAYERS("Players"),
    HOSTILE("Hostile"),
    ANGERABLE("Angerable"),
    WATER_CREATURE("WaterCreature"),
    PASSIVE("Passive"),
    INVISIBLE("Invisible"),
    DEAD("Dead"),
    SLEEPING("Sleeping"),
    FRIENDS("Friends");
}

fun EnumSet<Targets>.shouldAttack(entity: Entity): Boolean {
    val info = EntityTaggingManager.getTag(entity).targetingInfo

    return when {
        info.isFriend && Targets.FRIENDS !in this -> false
        info.classification === EntityTargetClassification.TARGET -> isInteresting(entity)
        else -> false
    }
}

fun EnumSet<Targets>.shouldShow(entity: Entity): Boolean {
    val info = EntityTaggingManager.getTag(entity).targetingInfo

    return when {
        info.isFriend && Targets.FRIENDS !in this -> false
        info.classification !== EntityTargetClassification.IGNORED -> isInteresting(entity)
        else -> false
    }
}

/**
 * Check if an entity is considered a target
 */
@Suppress("CyclomaticComplexMethod", "ReturnCount")
private fun EnumSet<Targets>.isInteresting(suspect: Entity): Boolean {
    // Check if the enemy is living and not dead (or ignore being dead)
    if (suspect !is LivingEntity || !(Targets.DEAD in this || suspect.isAlive)) {
        return false
    }

    // Check if enemy is invisible (or ignore being invisible)
    if (Targets.INVISIBLE !in this && suspect.isInvisible) {
        return false
    }

    // Check if enemy is a player and should be considered as a target
    return when (suspect) {
        is PlayerEntity -> when {
            suspect == mc.player -> false
            // Check if enemy is sleeping (or ignore being sleeping)
            suspect.isSleeping && Targets.SLEEPING !in this -> false
            else -> Targets.PLAYERS in this
        }
        is WaterCreatureEntity -> Targets.WATER_CREATURE in this
        is PassiveEntity -> Targets.PASSIVE in this
        is HostileEntity, is Monster -> Targets.HOSTILE in this
        is Angerable -> Targets.ANGERABLE in this
        else -> false
    }
}

// Extensions
@JvmOverloads
fun Entity.shouldBeShown(enemyConf: EnumSet<Targets> = ModuleTargets.visual) =
    enemyConf.shouldShow(this)

@JvmOverloads
fun Entity.shouldBeAttacked(enemyConf: EnumSet<Targets> = ModuleTargets.combat) =
    enemyConf.shouldAttack(this)

/**
 * Find the best enemy in the current world in a specific range.
 */
fun ClientWorld.findEnemy(
    range: ClosedFloatingPointRange<Float>,
    enemyConf: EnumSet<Targets> = ModuleTargets.combat
) = findEnemies(range, enemyConf).minByOrNull { (_, distance) -> distance }?.key()

fun ClientWorld.findEnemies(
    range: ClosedFloatingPointRange<Float>,
    enemyConf: EnumSet<Targets> = ModuleTargets.combat
): List<ObjectDoublePair<Entity>> {
    val squaredRange = (range.start * range.start..range.endInclusive * range.endInclusive).toDouble()

    return getEntitiesInCuboid(player.eyePos, squaredRange.endInclusive)
        .filter { it.shouldBeAttacked(enemyConf) }
        .map { ObjectDoublePair.of(it, it.squaredBoxedDistanceTo(player)) }
        .filter { (_, distance) -> distance in squaredRange }
}

fun ClientWorld.getEntitiesInCuboid(
    midPos: Vec3d,
    range: Double,
    predicate: Predicate<Entity> = Predicate { true }
): MutableList<Entity> {
    return getOtherEntities(null, Box(midPos.subtract(range, range, range),
        midPos.add(range, range, range)), predicate)
}

inline fun ClientWorld.getEntitiesBoxInRange(
    midPos: Vec3d,
    range: Double,
    crossinline predicate: (Entity) -> Boolean = { true }
): MutableList<Entity> {
    val rangeSquared = range * range

    return getEntitiesInCuboid(midPos, range) { predicate(it) && it.squaredBoxedDistanceTo(midPos) <= rangeSquared }
}

fun Entity.attack(swing: Boolean, keepSprint: Boolean = false) {
    attack(if (swing) SwingMode.DO_NOT_HIDE else SwingMode.HIDE_BOTH, keepSprint)
}

@Suppress("CognitiveComplexMethod", "NestedBlockDepth", "MagicNumber")
fun Entity.attack(swing: SwingMode, keepSprint: Boolean = false) {
    if (EventManager.callEvent(AttackEntityEvent(this) {
        attack(swing, keepSprint)
    }).isCancelled) {
        return
    }

    with(player) {
        // Swing before attacking (on 1.8)
        if (isOlderThanOrEqual1_8) {
            swing.swing(Hand.MAIN_HAND)
        }

        network.sendPacket(PlayerInteractEntityC2SPacket.attack(this@attack, isSneaking))

        if (keepSprint) {
            var genericAttackDamage =
                if (this.isUsingRiptide) {
                    this.riptideAttackDamage
                } else {
                    getAttributeValue(EntityAttributes.ATTACK_DAMAGE).toFloat()
                }
            val damageSource = this.damageSources.playerAttack(this)
            var enchantAttackDamage = this.getDamageAgainst(this@attack, genericAttackDamage,
                damageSource) - genericAttackDamage

            val attackCooldown = this.getAttackCooldownProgress(0.5f)
            genericAttackDamage *= 0.2f + attackCooldown * attackCooldown * 0.8f
            enchantAttackDamage *= attackCooldown

            if (genericAttackDamage > 0.0f || enchantAttackDamage > 0.0f) {
                if (enchantAttackDamage > 0.0f) {
                    this.addEnchantedHitParticles(this@attack)
                }

                if (ModuleCriticals.wouldDoCriticalHit(true)) {
                    world.playSound(
                        null, x, y, z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT,
                        soundCategory, 1.0f, 1.0f
                    )
                    addCritParticles(this@attack)
                }
            }
        } else {
            if (interaction.currentGameMode != GameMode.SPECTATOR) {
                attack(this@attack)
            }
        }

        // Reset cooldown
        resetLastAttackedTicks()

        // Swing after attacking (on 1.9+)
        if (!isOlderThanOrEqual1_8) {
            swing.swing(Hand.MAIN_HAND)
        }
    }
}
