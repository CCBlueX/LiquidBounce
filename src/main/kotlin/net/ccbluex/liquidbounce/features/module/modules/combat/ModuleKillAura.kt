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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.MC_1_8
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.eyesPos
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityGroup
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand
import net.minecraft.world.GameMode

/**
 * KillAura module
 *
 * Automatically attacks enemies
 */
object ModuleKillAura : Module("KillAura", Category.COMBAT) {

    // Attack speed
    private val cps by intRange("CPS", 5..8, 1..20)
    private val cooldown by boolean("Cooldown", true)

    // Range
    private val range by float("Range", 4.2f, 1f..8f)

    private val wallRange by float("WallRange", 3f, 0f..8f) // todo:

    // Target
    private val targetTracker = tree(TargetTracker())

    // Rotation
    private val rotations = tree(RotationsConfigurable())

    // Predict
    private val predict by floatRange("Predict", 0f..0f, 0f..5f) // todo:

    // Bypass techniques
    private val swing by boolean("Swing", true)
    private val keepSprint by boolean("KeepSprint", true)

    private val raycast by enumChoice("Raycast", TRACE_ALL, values())

    private val failRate by int("FailRate", 0, 0..100) // todo:

    private val missSwing by boolean("MissSwing", true) // todo:

    private val checkableInventory by boolean("CheckableInventory", false) // todo:

    private val cpsTimer = CpsScheduler()

    override fun disable() {
        targetTracker.cleanup()
    }

    val repeatable = repeatable {
        update()
    }

    private fun update() {
        if (player.isSpectator) {
            return
        }

        targetTracker.validateLock { it.boxedDistanceTo(player) <= range }

        val eyes = player.eyesPos

        for (target in targetTracker.enemies()) {
            if (target.boxedDistanceTo(player) > range) {
                continue
            }

            val box = target.boundingBox

            // todo: add predict to box and eyes

            // find best spot (and skip if no spot was found)
            val (rotation, _) = RotationManager.raytraceBox(eyes, box, throughWalls = false, range = range.toDouble()) ?: continue

            // lock on target tracker
            targetTracker.lock(target)

            // aim on target
            RotationManager.aimAt(rotation, configurable = rotations)
            break
        }

        val target = targetTracker.lockedOnTarget ?: return
        val rotation = RotationManager.serverRotation ?: return

        if (target.boxedDistanceTo(player) <= range && facingEnemy(target, range.toDouble(), rotation)) {
            // Check if between enemy and player is another entity
            val raycastedEntity = raytraceEntity(
                range.toDouble(),
                rotation,
                filter = {
                    when (raycast) {
                        TRACE_NONE -> false
                        TRACE_ONLYENEMY -> it.shouldBeAttacked()
                        TRACE_ALL -> true
                    }
                }
            ) ?: target

            // Swap enemy if there is a better enemy
            // todo: compare current target to locked target
            if (raycastedEntity.shouldBeAttacked() && raycastedEntity != target) {
                targetTracker.lock(raycastedEntity)
            }

            // Attack enemy according to cps and cooldown
            cpsTimer.tick(
                click = {
                    attackEntity(raycastedEntity)
                },
                condition = {
                    !cooldown || player.getAttackCooldownProgress(0.0f) >= 1.0f
                },
                cps
            )
        }
    }

    private fun attackEntity(entity: Entity) {
        // todo: stop blocking (1.8 support sword / 1.9+ shield)

        EventManager.callEvent(AttackEvent(entity))

        // Swing before attacking (on 1.8)
        if (swing && protocolVersion == MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        network.sendPacket(PlayerInteractEntityC2SPacket(entity, player.isSneaking))

        // Swing after attacking (on 1.9+)
        if (swing && protocolVersion != MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        if (keepSprint) {
            var genericAttackDamage = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE).toFloat()
            var magicAttackDamage = if (entity is LivingEntity) {
                EnchantmentHelper.getAttackDamage(player.mainHandStack, entity.group)
            } else {
                EnchantmentHelper.getAttackDamage(player.mainHandStack, EntityGroup.DEFAULT)
            }

            val cooldownProgress = player.getAttackCooldownProgress(0.5f)
            genericAttackDamage *= 0.2f + cooldownProgress * cooldownProgress * 0.8f
            magicAttackDamage *= cooldownProgress

            if (genericAttackDamage > 0.0f && magicAttackDamage > 0.0f) {
                player.addEnchantedHitParticles(entity)
            }
        } else {
            if (interaction.currentGameMode != GameMode.SPECTATOR) {
                player.attack(entity)
            }
        }

        // reset cooldown
        player.resetLastAttackedTicks()
    }

    enum class RaycastMode(override val choiceName: String) : NamedChoice {
        TRACE_NONE("None"),
        TRACE_ONLYENEMY("Enemy"),
        TRACE_ALL("All")
    }

}
