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
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
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
import net.minecraft.world.entity.Attackable
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.GameType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.function.Predicate

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

// Extensions
fun Entity.shouldBeShown(): Boolean {
    if (this === player || this.hasPassenger(player)) {
        return mc.options.cameraType !== CameraType.FIRST_PERSON || ModuleFreeCam.enabled || ModuleFreeLook.enabled
    }

    val info = EntityTaggingManager.getTag(this).targetingInfo
    return !info.isFriend && info.classification !== EntityTargetClassification.IGNORED
}

fun Entity?.shouldBeAttacked(includeFriends: Boolean = false) =
    this is Attackable
        && this !== player
        && !this.hasPassenger(player)
        && EntityTaggingManager.getTag(this).targetingInfo.let { info ->
            (includeFriends || !info.isFriend) && info.classification === EntityTargetClassification.TARGET
        }

fun Entity.matchesTargetState(
    allowInvisible: Boolean = false,
    allowSleeping: Boolean = false,
    allowDead: Boolean = false,
    allowCustomNamed: Boolean = true,
    allowTamed: Boolean = false,
    allowTeamMates: Boolean = false,
    allowFriends: Boolean = false
): Boolean {
    return isAllowedByLifeState(allowDead)
        && isAllowedByVisibility(allowInvisible)
        && isAllowedBySleepingState(allowSleeping)
        && isAllowedByCustomName(allowCustomNamed)
        && isAllowedByTeam(allowTeamMates)
        && isAllowedByTamed(allowTamed)
        && isAllowedByFriends(allowFriends)
}

private fun Entity.isAllowedByLifeState(allowDead: Boolean) =
    !(this is LivingEntity && !allowDead && !this.isAlive)

private fun Entity.isAllowedByVisibility(allowInvisible: Boolean) =
    allowInvisible || !this.isInvisible

private fun Entity.isAllowedBySleepingState(allowSleeping: Boolean) =
    allowSleeping || this !is Player || !this.isSleeping

private fun Entity.isAllowedByCustomName(allowCustomNamed: Boolean) =
    allowCustomNamed || this.customName == null

private fun Entity.isAllowedByTeam(allowTeamMates: Boolean) =
    allowTeamMates || !this.isAlliedTo(player)

private fun Entity.isAllowedByTamed(allowTamed: Boolean): Boolean {
    if (allowTamed || this !is TamableAnimal) {
        return true
    }

    val owner = this.ownerReference?.uuid
    return owner == null || owner == player.uuid
}

private fun Entity.isAllowedByFriends(allowFriends: Boolean): Boolean {
    if (allowFriends) {
        return true
    }

    val info = EntityTaggingManager.getTag(this).targetingInfo
    return !info.isFriend
}

/**
 * Find the best enemy in the current world in a specific range.
 */
fun ClientLevel.findEnemy(
    range: ClosedFloatingPointRange<Float>,
    predicate: (Entity) -> Boolean = { it.shouldBeAttacked() }
) = findEnemies(range, predicate).minByOrNull { (_, distance) -> distance }?.key()

fun ClientLevel.findEnemies(
    range: ClosedFloatingPointRange<Float>,
    predicate: (Entity) -> Boolean = { it.shouldBeAttacked() }
): List<ObjectDoublePair<Entity>> {
    val squaredRange = (range.start * range.start..range.endInclusive * range.endInclusive).toDouble()

    return getEntitiesInCuboid(player.eyePosition, squaredRange.endInclusive)
        .filter(predicate)
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
