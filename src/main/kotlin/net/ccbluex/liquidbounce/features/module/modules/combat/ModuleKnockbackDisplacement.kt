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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PostAttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.math.allEmpty
import net.ccbluex.liquidbounce.utils.network.sendStartSprinting
import net.ccbluex.liquidbounce.utils.network.sendStopSprinting
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * Knockback Displacement module.
 *
 * Manipulates the rotation sent before an attack to control the direction of
 * sprint-knockback dealt to the target. The server calculates sprint-KB as:
 *   knockback(0.5, -sin(attackerYaw * DEG_TO_RAD), cos(attackerYaw * DEG_TO_RAD))
 *
 * Works standalone or with KillAura. Can optionally detect void
 * behind the target to only activate in bridge fight scenarios.
 *
 * Reference: https://www.youtube.com/watch?v=7t0PyqYsac8
 */
@Suppress("MagicNumber")
object ModuleKnockbackDisplacement : ClientModule(
    "KnockbackDisplacement",
    ModuleCategories.COMBAT,
    aliases = listOf("KBDisplacement", "KnockbackDirection")
) {

    private val modes = choices<DisplacementMode>("Mode", 0) {
        arrayOf(
            DisplacementMode.Push(it),
            DisplacementMode.Pull(it),
            DisplacementMode.Upward(it),
            DisplacementMode.Horizontal(it),
            DisplacementMode.Custom(it)
        )
    }.apply(::tagBy)

    private val cooldownTicks by int("Cooldown", 0, 0..40, "ticks")

    /**
     * Sends sprint packets before each attack to ensure the server considers the player
     * as sprinting. Required when using Sprint module in Legit mode, which only sets
     * client-side sprint state without notifying the server after sprint-knockback resets it.
     */
    private val forceSprint by boolean("ForceSprint", false)

    /**
     * Only apply displacement when attacking via KillAura.
     * When disabled, works with manual attacks too.
     */
    private val onlyKillAura by boolean("OnlyKillAura", false)

    /**
     * Only apply displacement when using a stick (blaze rod, stick).
     * Useful for BedWars KB sticks.
     */
    private val onlyStick by boolean("OnlyStick", false)

    // Void detection sub-group
    private object VoidDetection : ToggleableValueGroup(ModuleKnockbackDisplacement, "VoidDetection", false) {
        /**
         * Maximum FOV angle (in degrees) for void detection.
         * Only activates when the void direction is within this angle from player's view.
         */
        val maxFov by float("MaxFOV", 90f, 0f..180f, "°")

        /**
         * How far ahead (in blocks) to check for void behind the target.
         */
        val checkDistance by float("CheckDistance", 3f, 1f..10f, "blocks")

        /**
         * The Y level below which is considered void.
         */
        val voidLevel by int("VoidLevel", 0, -64..0)
    }

    init {
        tree(VoidDetection)
    }

    private var ticksSinceLastUse = 0
    
    // Flag to track if we need to send return rotation after attack
    private var shouldSendReturnRotation = false

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (ticksSinceLastUse > 0) {
            ticksSinceLastUse--
        }
    }

    /**
     * Handle attack event - inject displacement rotation BEFORE the attack packet is sent.
     * This runs at HEAD of MultiPlayerGameMode.attack() via Mixin.
     */
    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent>(priority = CRITICAL_MODIFICATION) { event ->
        val target = event.entity

        // Check if we should operate
        if (!shouldOperate(target)) {
            return@handler
        }

        val displacementRotation = getDisplacementRotation(target) ?: return@handler

        // Send the displacement rotation packet BEFORE the attack
        val fixedRotation = displacementRotation.normalize()
        network.send(
            ServerboundMovePlayerPacket.Rot(
                fixedRotation.yaw,
                fixedRotation.pitch,
                player.onGround(),
                player.horizontalCollision
            )
        )

        // Mark that we need to send return rotation after attack
        shouldSendReturnRotation = true

        // Start cooldown
        ticksSinceLastUse = cooldownTicks
    }

    /**
     * Handle post-attack event - send return rotation AFTER the attack packet was sent.
     * Fired from CombatExtensions.attackEntity() after ServerboundAttackPacket.
     */
    @Suppress("unused")
    private val postAttackHandler = handler<PostAttackEntityEvent> {
        if (!shouldSendReturnRotation) return@handler
        
        shouldSendReturnRotation = false
        
        // Send player's actual rotation back to avoid detection
        network.send(
            ServerboundMovePlayerPacket.Rot(
                player.yRot,
                player.xRot,
                player.onGround(),
                player.horizontalCollision
            )
        )
    }

    /**
     * Computes the displacement rotation for the given target.
     * Returns a GCD-normalized [Rotation] to send before the attack, or null if
     * displacement should not be applied.
     */
    private fun getDisplacementRotation(target: Entity): Rotation? {
        if (ticksSinceLastUse > 0) return null

        // Sprint-knockback only applies when sprinting
        if (!player.isSprinting) return null

        // Void detection check
        if (VoidDetection.enabled && !isVoidBehindTarget(target)) {
            return null
        }

        val dx = target.x - player.x
        val dz = target.z - player.z
        // Yaw pointing FROM player TO target (vanilla convention: atan2(-dx, dz))
        val yawToTarget = Math.toDegrees(atan2(-dx, dz)).toFloat()

        val rawRotation = modes.activeMode.computeRawRotation(yawToTarget)

        // Normalize to GCD to avoid anticheat detection
        val gcd = RotationUtil.gcd.toFloat().coerceAtLeast(0.001f)
        val yaw = Mth.wrapDegrees((rawRotation.x / gcd).roundToInt() * gcd)
        val pitch = Mth.clamp((rawRotation.y / gcd).roundToInt() * gcd, -90f, 90f)

        // Force sprint packets if enabled (needed for Sprint module Legit mode)
        if (forceSprint && player.isSprinting) {
            network.sendStopSprinting()
            network.sendStartSprinting()
        }

        return Rotation(yaw, pitch)
    }

    /**
     * Checks if there's void behind the target in the knockback direction.
     * Also validates that the void direction is within the configured FOV.
     */
    private fun isVoidBehindTarget(target: Entity): Boolean {
        val dx = target.x - player.x
        val dz = target.z - player.z

        // Direction FROM player TO target (knockback push direction)
        val distance = Mth.sqrt((dx * dx + dz * dz).toFloat()).toDouble()
        if (distance < 0.01) return false

        val dirX = dx / distance
        val dirZ = dz / distance

        // Check the FOV constraint
        val yawToVoid = Math.toDegrees(atan2(-dirX, dirZ)).toFloat()
        val voidRotation = Rotation(yawToVoid, 0f)
        val angleToVoid = player.rotation.angleTo(voidRotation)

        if (angleToVoid > VoidDetection.maxFov) {
            return false
        }

        // Check for void behind the target
        val checkDist = VoidDetection.checkDistance.toDouble()
        val checkX = target.x + dirX * checkDist
        val checkZ = target.z + dirZ * checkDist
        val checkY = target.y

        // Create a bounding box from target position extending down to void level
        val checkBox = AABB(
            checkX - 0.3, VoidDetection.voidLevel.toDouble(), checkZ - 0.3,
            checkX + 0.3, checkY, checkZ + 0.3
        )

        // If no collisions found between check position and void level, there's void
        return world.getBlockCollisions(player, checkBox).allEmpty()
    }

    private fun shouldOperate(target: Entity): Boolean {
        if (target !is LivingEntity) return false

        // Check OnlyKillAura setting
        if (onlyKillAura && !ModuleKillAura.running) {
            return false
        }

        // Check OnlyStick setting (blaze rod, stick items for BedWars KB sticks)
        if (onlyStick) {
            val item = player.mainHandItem.item
            val isStick = item == Items.BLAZE_ROD || item == Items.STICK
            if (!isStick) return false
        }

        return true
    }

    override fun onEnabled() {
        ticksSinceLastUse = 0
        shouldSendReturnRotation = false
    }

    override fun onDisabled() {
        shouldSendReturnRotation = false
    }

    private sealed class DisplacementMode(name: String, override val parent: ModeValueGroup<*>) : Mode(name) {

        /**
         * Computes the raw (un-normalized) rotation for the displacement.
         * @param yawToTarget yaw from player to target in degrees
         * @return Vec2(yaw, pitch) for the displacement rotation
         */
        abstract fun computeRawRotation(yawToTarget: Float): Vec2

        /** Push target away from player (normal direction, useful as baseline). */
        class Push(parent: ModeValueGroup<*>) : DisplacementMode("Push", parent) {
            override fun computeRawRotation(yawToTarget: Float) = Vec2(yawToTarget, player.xRot)
        }

        /** Pull target toward player (vacuum effect). */
        class Pull(parent: ModeValueGroup<*>) : DisplacementMode("Pull", parent) {
            override fun computeRawRotation(yawToTarget: Float) = Vec2(yawToTarget + 180f, player.xRot)
        }

        /** Launch target upward. */
        class Upward(parent: ModeValueGroup<*>) : DisplacementMode("Upward", parent) {
            override fun computeRawRotation(yawToTarget: Float) = Vec2(yawToTarget, -70f)
        }

        /** Push target sideways (perpendicular to player-target line). */
        class Horizontal(parent: ModeValueGroup<*>) : DisplacementMode("Horizontal", parent) {
            override fun computeRawRotation(yawToTarget: Float) = Vec2(yawToTarget + 90f, player.xRot)
        }

        /** Custom yaw/pitch offset from player-to-target direction. */
        class Custom(parent: ModeValueGroup<*>) : DisplacementMode("Custom", parent) {
            private val customYaw by float("CustomYaw", 0f, -180f..180f, "°")
            private val customPitch by float("CustomPitch", 0f, -90f..90f, "°")

            override fun computeRawRotation(yawToTarget: Float) =
                Vec2(yawToTarget + customYaw, player.xRot + customPitch)
        }
    }
}
