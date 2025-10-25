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
 *
 */

package net.ccbluex.liquidbounce.utils.block.placer

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.immutable
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntities
import net.ccbluex.liquidbounce.utils.block.isInteractable
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.block.BlockState
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

fun BlockPlacer.placeInstantOnBlockUpdate(event: PacketEvent) {
    when (val packet = event.packet) {
        is BlockUpdateS2CPacket -> placeInstant(packet.pos, packet.state)
        is ChunkDeltaUpdateS2CPacket -> {
            packet.visitUpdates { pos, state -> placeInstant(pos, state) }
        }
    }
}

private fun BlockPlacer.placeInstant(pos: BlockPos, state: BlockState) {
    val irrelevantPacket = !state.isReplaceable || pos.asLong() !in blocks

    val rotationMode = rotationMode.activeChoice
    if (irrelevantPacket || rotationMode !is NoRotationMode || pos.isBlockedByEntities()) {
        return
    }

    val searchOptions = BlockPlacementTargetFindingOptions(
        BlockOffsetOptions(
            listOf(Vec3i.ZERO),
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
        ),
        FaceHandlingOptions(CenterTargetPositionFactory, considerFacingAwayFaces = wallRange > 0),
        stackToPlaceWith = Items.SANDSTONE.defaultStack,
        PlayerLocationOnPlacement(position = player.pos, pose = player.pose),
    )

    val placementTarget = findBestBlockPlacementTarget(pos, searchOptions) ?: return

    // Check if we can reach the target
    if (!canReach(placementTarget.interactedBlockPos, placementTarget.rotation)) {
        return
    }

    if (placementTarget.interactedBlockPos.getState().isInteractable) {
        return
    }

    if (rotationMode.send) {
        val rotation = placementTarget.rotation.normalize()
        network.sendPacket(
            PlayerMoveC2SPacket.LookAndOnGround(rotation.yaw, rotation.pitch, player.isOnGround,
                player.horizontalCollision)
        )
    }

    doPlacement(false, pos.immutable, placementTarget)
}
