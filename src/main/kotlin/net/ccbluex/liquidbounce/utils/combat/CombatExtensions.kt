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
@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.combat

import it.unimi.dsi.fastutil.objects.ObjectDoublePair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeLook
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.minecraft.client.CameraType
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Attackable
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.NeutralMob
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.animal.allay.Allay
import net.minecraft.world.entity.animal.fish.WaterAnimal
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.EnumSet
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
        @JvmField
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
enum class Targets(override val tag: String) : Tagged {
    SELF("Self"),
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

private fun EnumSet<Targets>.shouldAttack(entity: Entity): Boolean {
    val info = EntityTaggingManager.getTag(entity).targetingInfo

    return when {
        info.isFriend && Targets.FRIENDS !in this -> false
        info.classification === EntityTargetClassification.TARGET -> isInteresting(entity)
        else -> false
    }
}

private fun EnumSet<Targets>.shouldShow(entity: Entity): Boolean {
    if (entity === player) {
        return Targets.SELF in this &&
            (mc.options.cameraType !== CameraType.FIRST_PERSON || ModuleFreeCam.enabled || ModuleFreeLook.enabled)
    }

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
        is Player -> when {
            suspect === mc.player -> false
            // Check if enemy is sleeping (or ignore being sleeping)
            suspect.isSleeping && Targets.SLEEPING !in this -> false
            else -> Targets.PLAYERS in this
        }
        is WaterAnimal -> Targets.WATER_CREATURE in this
        is AgeableMob, is Bat, is Allay -> Targets.PASSIVE in this
        is Monster, is Enemy -> Targets.HOSTILE in this
        is NeutralMob -> Targets.ANGERABLE in this

        else -> false
    }
}

// Extensions
@JvmOverloads
fun Entity.shouldBeShown(enemyConf: EnumSet<Targets> = GlobalSettingsTarget.visual) =
    enemyConf.shouldShow(this)

@JvmOverloads
fun Entity?.shouldBeAttacked(enemyConf: EnumSet<Targets> = GlobalSettingsTarget.combat) =
    this is Attackable && enemyConf.shouldAttack(this)

/**
 * Find the best enemy in the current world in a specific range.
 */
fun ClientLevel.findEnemy(
    range: ClosedFloatingPointRange<Float>,
    enemyConf: EnumSet<Targets> = GlobalSettingsTarget.combat
) = findEnemies(range, enemyConf).minByOrNull { (_, distance) -> distance }?.key()

fun ClientLevel.findEnemies(
    range: ClosedFloatingPointRange<Float>,
    enemyConf: EnumSet<Targets> = GlobalSettingsTarget.combat
): List<ObjectDoublePair<Entity>> {
    val squaredRange = (range.start * range.start..range.endInclusive * range.endInclusive).toDouble()

    return getEntitiesInCuboid(player.eyePosition, squaredRange.endInclusive)
        .filter { it.shouldBeAttacked(enemyConf) }
        .mapToArray { ObjectDoublePair.of(it, it.squaredBoxedDistanceTo(player)) }
        .filter { (_, distance) -> distance in squaredRange }
}

fun ClientLevel.getEntitiesInCuboid(
    midPos: Vec3,
    range: Double,
    predicate: Predicate<Entity> = Predicate { true }
): MutableList<Entity> {
    return getEntities(null, AABB(midPos.subtract(range, range, range),
        midPos.add(range, range, range)), predicate)
}

inline fun ClientLevel.getEntitiesBoxInRange(
    midPos: Vec3,
    range: Double,
    crossinline predicate: (Entity) -> Boolean = { true }
): MutableList<Entity> {
    val rangeSquared = range * range

    return getEntitiesInCuboid(midPos, range) { predicate(it) && it.squaredBoxedDistanceTo(midPos) <= rangeSquared }
}

/**
 * @see net.minecraft.client.Minecraft.startAttack
 */
@Suppress("CognitiveComplexMethod", "NestedBlockDepth", "MagicNumber")
fun attackEntity(entity: Entity, swing: SwingMode, keepSprint: Boolean = false) {
    val itemStack = player.getItemInHand(InteractionHand.MAIN_HAND)
    val piercingWeapon = itemStack.get(DataComponents.PIERCING_WEAPON)

    // Minecraft introduced piercing weapons that have their own attack method.
    // You HAVE to look at the entity before attacking it.
    if (piercingWeapon != null && !interaction.isSpectator) {
        interaction.piercingAttack(piercingWeapon)
        swing.swing(InteractionHand.MAIN_HAND)
        return
    }

    if (EventManager.callEvent(AttackEntityEvent(entity)).isCancelled) {
        return
    }

    with(player) {
        // Swing before attacking (on 1.8)
        if (isOlderThanOrEqual1_8) {
            swing.swing(InteractionHand.MAIN_HAND)
        }

        interaction.ensureHasSentCarriedItem()
        network.send(ServerboundInteractPacket.createAttackPacket(entity, isShiftKeyDown))

        if (keepSprint) {
            var genericAttackDamage =
                if (this.isAutoSpinAttack) {
                    this.autoSpinAttackDmg
                } else {
                    getAttributeValue(Attributes.ATTACK_DAMAGE).toFloat()
                }
            val damageSource = this.damageSources().playerAttack(this)
            var enchantAttackDamage = this.getEnchantedDamage(entity, genericAttackDamage,
                damageSource) - genericAttackDamage

            val attackCooldown = this.getAttackStrengthScale(0.5f)
            genericAttackDamage *= 0.2f + attackCooldown * attackCooldown * 0.8f
            enchantAttackDamage *= attackCooldown

            if (genericAttackDamage > 0.0f || enchantAttackDamage > 0.0f) {
                if (enchantAttackDamage > 0.0f) {
                    this.magicCrit(entity)
                }

                if (ModuleCriticals.wouldDoCriticalHit(true)) {
                    world.playSound(
                        null, x, y, z, SoundEvents.PLAYER_ATTACK_CRIT,
                        soundSource, 1.0f, 1.0f
                    )
                    crit(entity)
                }
            }
        } else {
            if (interaction.playerMode != GameType.SPECTATOR) {
                attack(entity)
            }
        }

        // Reset cooldown
        this.attackStrengthTicker = 0

        // Swing after attacking (on 1.9+)
        if (!isOlderThanOrEqual1_8) {
            swing.swing(InteractionHand.MAIN_HAND)
        }
    }
}
