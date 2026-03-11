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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickConditional
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
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
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryType
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl
import net.minecraft.world.item.Items
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

private const val MAX_SIMULATED_TICKS = 240

/**
 * Auto pearl module
 *
 * AutoPearl aims and throws a pearl at an enemies pearl trajectory
 *
 * @author sqlerrorthing
 */
object ModuleAutoPearl : ClientModule(
    "AutoPearl",
    ModuleCategories.COMBAT,
    aliases = listOf("PearlFollower", "PearlTarget")
) {

    private val mode by enumChoice("Mode", Modes.TRIGGER)

    private object Limits : ToggleableValueGroup(this, "Limits", true) {
        val angle by int("Angle", 180, 0..180, suffix = "°")
        val activationDistance by float("MinDistance", 8.0f, 0.0f..10.0f, suffix = "m")
        val destDistance by float("DestinationDistance", 8.0f, 0.0f..30.0f, suffix = "m")
    }

    private object Rotate : ToggleableValueGroup(this, "Rotate", true) {
        val rotations = tree(RotationsValueGroup(this))
    }

    init {
        treeAll(Rotate, Limits)
    }

    private val combatPauseTime by int("CombatPauseTime", 0, 0..40, "ticks")
    private val slotResetDelay by intRange("SlotResetDelay", 0..0, 0..40, "ticks")

    private val queue = ArrayDeque<Rotation>()

    @Suppress("unused")
    private val pearlSpawnHandler = handler<PacketEvent> { event ->
        if (event.packet !is ClientboundAddEntityPacket || event.packet.type != EntityType.ENDER_PEARL) {
            return@handler
        }

        Slots.OffhandWithHotbar.findSlot(Items.ENDER_PEARL) ?: return@handler

        val data = event.packet
        val entity = data.type.create(world, EntitySpawnReason.SPAWN_ITEM_USE) as ThrownEnderpearl
        entity.recreateFromPacket(data)

        proceedPearl(
            pearl = entity,
            // entity.velocity & entity.pos doesn't work, don't use it
            velocity = with(data) { Vec3(movement.x, movement.y, movement.z) },
            pearlPos = with(data) { Vec3(x, y, z) }
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

            tickConditional(20) {
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
        pearl: ThrownEnderpearl,
        velocity: Vec3,
        pearlPos: Vec3
    ) {
        if (!canTrigger(pearl)) {
            return
        }

        val destination = runSimulation(
            owner = pearl.owner ?: player,
            velocity = velocity,
            pos = pearlPos
        )?.location ?: return

        if (Limits.enabled && Limits.activationDistance > destination.distanceTo(player.position())) {
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

    private fun canTrigger(pearl: ThrownEnderpearl): Boolean {
        if (Limits.enabled && Limits.angle < RotationUtil.crosshairAngleToEntity(pearl)) {
            return false
        }

        if (pearl.owner == null) {
            return mode == Modes.TRIGGER
        }

        if (pearl.owner === player) {
            return false
        }

        return when(mode) {
            Modes.TRIGGER -> pearl.owner!!.shouldBeAttacked()
            Modes.TARGET -> ModuleKillAura.targetTracker.target === pearl.owner
        }
    }

    private fun canThrow(
        angles: Rotation,
        destination: Vec3
    ): Boolean {
        val simulatedDestination = TrajectoryInfoRenderer.getHypotheticalTrajectory(
            owner = player,
            trajectoryInfo = TrajectoryInfo.GENERIC,
            trajectoryType = TrajectoryType.EnderPearl,
            rotation = angles
        ).runSimulation(MAX_SIMULATED_TICKS).hitResult?.location ?: return false

        return !Limits.enabled || Limits.destDistance > destination.distanceTo(simulatedDestination)
    }

    private fun runSimulation(
        owner: Entity,
        velocity: Vec3,
        pos: Vec3,
        trajectoryInfo: TrajectoryInfo = TrajectoryInfo.GENERIC,
        renderOffset: Vec3 = Vec3.ZERO
    ): HitResult? =
        TrajectoryInfoRenderer(
            owner = owner,
            icon = Items.ENDER_PEARL.defaultInstance,
            velocity = velocity,
            pos = pos,
            trajectoryInfo = trajectoryInfo,
            trajectoryType = TrajectoryType.EnderPearl,
            type = TrajectoryInfoRenderer.Type.REAL,
            renderOffset = renderOffset
        ).runSimulation(MAX_SIMULATED_TICKS).hitResult

    override fun onDisabled() {
        queue.clear()
    }

    private enum class Modes(override val tag: String) : Tagged {
        TRIGGER("Trigger"),
        TARGET("Target")
    }

}
