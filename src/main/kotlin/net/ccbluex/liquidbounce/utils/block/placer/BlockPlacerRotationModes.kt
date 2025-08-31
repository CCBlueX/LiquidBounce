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
package net.ccbluex.liquidbounce.utils.block.placer

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.aiming.PostRotationExecutor
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.RestrictedSingleUseAction
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import kotlin.math.max

abstract class BlockPlacerRotationMode(
    name: String,
    private val configurable: ChoiceConfigurable<BlockPlacerRotationMode>,
    val placer: BlockPlacer
) : Choice(name), MinecraftShortcuts {

    val postMove by boolean("PostMove", false)

    abstract operator fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean

    open fun getVerificationRotation(targetedRotation: Rotation) = targetedRotation

    open fun onTickStart() {}

    override val parent: ChoiceConfigurable<*>
        get() = configurable

}

/**
 * Normal rotations.
 * Only one placement per tick is possible, possible less because rotating takes some time.
 */
class NormalRotationMode(configurable: ChoiceConfigurable<BlockPlacerRotationMode>, placer: BlockPlacer)
    : BlockPlacerRotationMode("Normal", configurable, placer) {

    val rotations = tree(RotationsConfigurable(this))

    override fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean {
        val interactedBlockPos = placementTarget.interactedBlockPos
        RotationManager.setRotationTarget(
            placementTarget.rotation,
            considerInventory = !placer.ignoreOpenInventory,
            configurable = rotations,
            provider = placer.module,
            priority = placer.priority,
            whenReached = RestrictedSingleUseAction({
                val raytraceResult = raytraceBlock(
                    max(placer.range, placer.wallRange).toDouble(),
                    RotationManager.currentRotation ?: return@RestrictedSingleUseAction false,
                    interactedBlockPos,
                    interactedBlockPos.getState()!!
                ) ?: return@RestrictedSingleUseAction false

                raytraceResult.type == HitResult.Type.BLOCK && raytraceResult.blockPos == interactedBlockPos
            }, {
                PostRotationExecutor.addTask(placer.module, postMove, priority = true, task = {
                    if (placer.ticksToWait > 0) {
                        return@addTask
                    }

                    placer.doPlacement(isSupport, pos, placementTarget)
                    placer.ranAction = true
                })
            })
        )

        return true
    }

    override fun getVerificationRotation(targetedRotation: Rotation): Rotation = RotationManager.serverRotation

}

/**
 * No rotations, or just a packet containing the rotation target.
 */
class NoRotationMode(configurable: ChoiceConfigurable<BlockPlacerRotationMode>, placer: BlockPlacer)
    : BlockPlacerRotationMode("None", configurable, placer) {

    val send by boolean("SendRotationPacket", false)

    /**
     * Not rotating properly allows doing multiple placements. "b/o" stands for blocker per operation.
     */
    private val placements by int("Placements", 1, 1..10, "b/o")

    private var placementsDone = 0

    override fun invoke(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget): Boolean {
        PostRotationExecutor.addTask(placer.module, postMove, task = {
            if (placer.ticksToWait > 0) {
                return@addTask
            }

            if (send) {
                val rotation = placementTarget.rotation.normalize()
                network.sendPacket(
                    PlayerMoveC2SPacket.LookAndOnGround(rotation.yaw, rotation.pitch, player.isOnGround,
                        player.horizontalCollision)
                )
            }

            placer.doPlacement(isSupport, pos, placementTarget)
            placer.ranAction = true
        })

        placementsDone++
        return placementsDone == placements
    }

    override fun onTickStart() {
        placementsDone = 0
    }

}
