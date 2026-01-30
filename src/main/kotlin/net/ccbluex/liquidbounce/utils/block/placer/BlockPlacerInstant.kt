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

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.immutable
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntities
import net.ccbluex.liquidbounce.utils.block.isInteractable
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.BlockState

fun BlockPlacer.placeInstantOnBlockUpdate(event: PacketEvent) {
    when (val packet = event.packet) {
        is ClientboundBlockUpdatePacket -> placeInstant(packet.pos, packet.blockState)
        is ClientboundSectionBlocksUpdatePacket -> {
            packet.runUpdates { pos, state -> placeInstant(pos, state) }
        }
    }
}

private fun BlockPlacer.placeInstant(pos: BlockPos, state: BlockState) {
    val irrelevantPacket = !state.canBeReplaced() || pos.asLong() !in blocks

    val rotationMode = rotationMode.activeMode
    if (irrelevantPacket || rotationMode !is NoRotationMode || pos.isBlockedByEntities()) {
        return
    }

    val searchOptions = BlockPlacementTargetFindingOptions(
        BlockOffsetOptions.Default,
        FaceHandlingOptions(CenterTargetPositionFactory, considerFacingAwayFaces = wallRange > 0),
        stackToPlaceWith = Items.SANDSTONE.defaultInstance,
        PlayerLocationOnPlacement(position = player.position(), pose = player.pose),
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
        network.send(
            ServerboundMovePlayerPacket.Rot(rotation.yaw, rotation.pitch, player.onGround(),
                player.horizontalCollision)
        )
    }

    doPlacement(false, pos.immutable, placementTarget)
}
