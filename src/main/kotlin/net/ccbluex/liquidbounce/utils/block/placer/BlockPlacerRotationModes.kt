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
package net.ccbluex.liquidbounce.utils.block.placer

import kotlinx.coroutines.Job
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.guarded
import net.ccbluex.liquidbounce.event.launchSequence
import net.ccbluex.liquidbounce.event.waitMatches
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.lookAt
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.surface
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.phys.HitResult
import kotlin.math.max

sealed class BlockPlacerRotationMode(
    name: String,
    private val modeValueGroup: ModeValueGroup<BlockPlacerRotationMode>,
    val placer: BlockPlacer
) : Mode(name), MinecraftShortcuts {

    val postMove by boolean("PostMove", false)

    /**
     * Schedules placing a block at [pos] by clicking [placementTarget].
     *
     * @return whether no further placement can be scheduled this tick
     */
    abstract operator fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean

    /**
     * Called at the start of every rotation update, before anything is scheduled.
     */
    open fun onTickStart() {}

    override val parent: ModeValueGroup<*>
        get() = modeValueGroup

    protected class Placement(val isSupport: Boolean, val pos: BlockPos, val target: BlockPlacementTarget)

}

/**
 * Normal rotations.
 * Only one placement per tick is possible, possible less because rotating takes some time.
 */
class NormalRotationMode(modeValueGroup: ModeValueGroup<BlockPlacerRotationMode>, placer: BlockPlacer)
    : BlockPlacerRotationMode("Normal", modeValueGroup, placer) {

    private val rotations = tree(RotationsValueGroup(this))

    /** The placement the rotation is planned for this tick; null on ticks nothing is scheduled. */
    private var planned: Placement? = null
    private var placing: Job? = null

    override fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean {
        planned = Placement(isSupport, pos, placementTarget)
        placing = placer.launchSequence(placing) { placeOnHit() }
        return true
    }

    override fun onTickStart() {
        planned = null
    }

    /**
     * Asks for the rotation of the placement planned each tick and places on the tick the rotation going out hits
     * the block it clicks.
     */
    private suspend fun placeOnHit() {
        guarded({ running }) {
            val placement by tracked { planned }
            val (clicked, going) = placer.parent.lookAt(placer.priority, {
                rotations.toRotationTarget(
                    placement.target.rotation,
                    considerInventory = !placer.ignoreOpenInventory,
                    surface = placement.target.surface(
                        throughWalls = placer.wallRange > 0f || placer.constructFailResult
                    ),
                )
            }) { going ->
                val interactedBlockPos = placement.target.interactedBlockPos
                raytraceBlock(
                    max(placer.range, placer.wallRange).toDouble(),
                    going,
                    interactedBlockPos,
                    interactedBlockPos.stateOrEmpty
                )?.takeIf { it.type == HitResult.Type.BLOCK && it.blockPos == interactedBlockPos }
                    ?.let { placement to going }
            }

            if (postMove) {
                placer.waitMatches<PlayerNetworkMovementTickEvent>(FIRST_PRIORITY) { it.state == EventState.POST }
            }

            if (placer.ticksToWait > 0) {
                return@guarded
            }

            placer.doPlacement(clicked.isSupport, clicked.pos, clicked.target, going)
            placer.ranAction = true
        }
    }

}

/**
 * No rotations, or just a packet containing the rotation target.
 */
class NoRotationMode(modeValueGroup: ModeValueGroup<BlockPlacerRotationMode>, placer: BlockPlacer)
    : BlockPlacerRotationMode("None", modeValueGroup, placer) {

    val send by boolean("SendRotationPacket", false)

    /**
     * Not rotating properly allows doing multiple placements. "b/o" stands for blocker per operation.
     */
    private val placements by int("Placements", 1, 1..10, "b/o")

    private var placementsDone = 0

    /** Placements scheduled since the last ones went down. */
    private val pending = ArrayList<Placement>()
    private var placing: Job? = null

    override fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean {
        pending += Placement(isSupport, pos, placementTarget)
        placing = placer.launchSequence(placing) { placeAfterMovementPacket() }

        placementsDone++
        return placementsDone == placements
    }

    override fun onTickStart() {
        placementsDone = 0
    }

    /**
     * Does the pending placements right after the movement packet when [postMove] is set and nothing else rotates,
     * else at the start of the next tick.
     */
    private suspend fun placeAfterMovementPacket() {
        try {
            if (postMove) {
                placer.waitMatches<PlayerNetworkMovementTickEvent>(FIRST_PRIORITY) { it.state == EventState.POST }
            }

            if (!postMove || RotationManager.currentRotation != null) {
                waitTicks(1)
            }

            if (placer.ticksToWait > 0) {
                return
            }

            for (placement in pending) {
                if (send) {
                    val rotation = placement.target.rotation.normalize()
                    network.send(
                        ServerboundMovePlayerPacket.Rot(rotation.yaw, rotation.pitch, player.onGround(),
                            player.horizontalCollision)
                    )
                }

                placer.doPlacement(placement.isSupport, placement.pos, placement.target)
                placer.ranAction = true
            }
        } finally {
            pending.clear()
        }
    }

}
