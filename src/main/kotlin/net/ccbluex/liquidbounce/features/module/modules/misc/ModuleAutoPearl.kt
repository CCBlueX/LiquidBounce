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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.projectiles.SituationalProjectileAngleCalculator
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityType
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.projectile.thrown.EnderPearlEntity
import net.minecraft.item.Items
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Vec3d

private const val MAX_SIMULATED_TICKS = 240

/**
 * Auto pearl module
 *
 * AutoPearl aims and throws a pearl at an enemies pearl trajectory
 *
 * @author sqlerrorthing
 */
object ModuleAutoPearl : ClientModule("AutoPearl", Category.COMBAT, aliases = arrayOf("PearlFollower", "PearlTarget")) {

    private val mode by enumChoice("Mode", Modes.TRIGGER)

    private object Limits : ToggleableConfigurable(this, "Limits", true) {
        val angle by int("Angle", 180, 0..180, suffix = "°")
        val activationDistance by float("MinDistance", 8.0f, 0.0f..10.0f, suffix = "m")
        val destDistance by float("DestinationDistance", 8.0f, 0.0f..30.0f, suffix = "m")
    }

    private object Rotate : ToggleableConfigurable(this, "Rotate", true) {
        val rotations = tree(RotationsConfigurable(this))
    }

    init {
        treeAll(Rotate, Limits)
    }

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..40, "ticks")

    private val queue = ArrayDeque<Rotation>()

    @Suppress("unused")
    private val pearlSpawnHandler = handler<PacketEvent> { event ->
        if (event.packet !is EntitySpawnS2CPacket || event.packet.entityType != EntityType.ENDER_PEARL) {
            return@handler
        }

        Slots.OffhandWithHotbar.findSlot(Items.ENDER_PEARL) ?: return@handler

        val data = event.packet
        val entity = data.entityType.create(world, SpawnReason.SPAWN_ITEM_USE) as EnderPearlEntity
        entity.onSpawnPacket(data)

        proceedPearl(
            pearl = entity,
            // entity.velocity & entity.pos doesn't work, don't use it
            velocity = with(data) { Vec3d(velocityX, velocityY, velocityZ) },
            pearlPos = with(data) { Vec3d(x, y, z) }
        )
    }

    @Suppress("unused")
    private val simulatedTickHandler = sequenceHandler<RotationUpdateEvent> {
        val rotation = queue.firstOrNull() ?: return@sequenceHandler

        CombatManager.pauseCombatForAtLeast(combatPauseTime)
        if (Rotate.enabled) {
            RotationManager.setRotationTarget(
                Rotate.rotations.toRotationTarget(rotation),
                Priority.IMPORTANT_FOR_USAGE_3,
                this@ModuleAutoPearl
            )
        }
    }

    @Suppress("unused")
    private val gameTickHandler = tickHandler {
        val rotation = queue.removeFirstOrNull() ?: return@tickHandler
        val itemSlot = Slots.OffhandWithHotbar.findSlot(Items.ENDER_PEARL) ?: return@tickHandler

        if (Rotate.enabled) {
            fun isRotationSufficient(): Boolean {
                return RotationManager.serverRotation.angleTo(rotation) <= 1.0f
            }

            waitConditional(20) {
                RotationManager.setRotationTarget(
                    Rotate.rotations.toRotationTarget(rotation),
                    Priority.IMPORTANT_FOR_USAGE_3,
                    this@ModuleAutoPearl
                )

                isRotationSufficient()
            }

            if (!isRotationSufficient()) {
                return@tickHandler
            }
        }

        val (yaw, pitch) = rotation.normalize()
        useHotbarSlotOrOffhand(itemSlot, slotResetDelay.random(), yaw, pitch)
    }

    private fun proceedPearl(
        pearl: EnderPearlEntity,
        velocity: Vec3d,
        pearlPos: Vec3d
    ) {
        if (!canTrigger(pearl)) {
            return
        }

        val destination = runSimulation(
            owner = pearl.owner ?: player,
            velocity = velocity,
            pos = pearlPos
        )?.pos ?: return

        if (Limits.enabled && Limits.activationDistance > destination.distanceTo(player.pos)) {
            return
        }

        val rotation = SituationalProjectileAngleCalculator.calculateAngleForStaticTarget(
            TrajectoryInfo.GENERIC,
            destination,
            EntityDimensions.fixed(1.0F, 0.0F)
        ) ?: return

        if (!canThrow(rotation, destination)) {
            return
        }

        if (queue.isEmpty()) {
            queue.add(rotation)
        }
    }

    private fun canTrigger(pearl: EnderPearlEntity): Boolean {
        if (Limits.enabled && Limits.angle < RotationUtil.crosshairAngleToEntity(pearl)) {
            return false
        }

        if (pearl.owner == null) {
            return mode == Modes.TRIGGER
        }

        if (pearl.ownerUuid == player.uuid) {
            return false
        }

        return when(mode) {
            Modes.TRIGGER -> pearl.owner!!.shouldBeAttacked()
            Modes.TARGET -> ModuleKillAura.targetTracker.target?.uuid == pearl.ownerUuid
        }
    }

    private fun canThrow(
        angles: Rotation,
        destination: Vec3d
    ): Boolean {
        val simulatedDestination = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            entity = player,
            trajectoryInfo = TrajectoryInfo.GENERIC,
            rotation = angles
        ).runSimulation(MAX_SIMULATED_TICKS)?.pos ?: return false

        return !Limits.enabled || Limits.destDistance > destination.distanceTo(simulatedDestination)
    }

    private fun runSimulation(
        owner: Entity,
        velocity: Vec3d,
        pos: Vec3d,
        trajectoryInfo: TrajectoryInfo = TrajectoryInfo.GENERIC,
        renderOffset: Vec3d = Vec3d.ZERO
    ): HitResult? =
        TrajectoryInfoRenderer(
            owner = owner,
            velocity = velocity,
            pos = pos,
            trajectoryInfo = trajectoryInfo,
            renderOffset = renderOffset
        ).runSimulation(MAX_SIMULATED_TICKS)

    override fun onDisabled() {
        queue.clear()
    }

    private enum class Modes(override val choiceName: String) : NamedChoice {
        TRIGGER("Trigger"),
        TARGET("Target")
    }

}
