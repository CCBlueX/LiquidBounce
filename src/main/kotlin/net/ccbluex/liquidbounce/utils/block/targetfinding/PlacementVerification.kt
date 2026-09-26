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
package net.ccbluex.liquidbounce.utils.block.targetfinding

import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.stateOrEmpty
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.raytracing.raytraceBlock
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.phys.BlockHitResult
import kotlin.math.max

/**
 * How the click of a [BlockPlacementTarget] is traced.
 */
enum class ClickTrace {
    /**
     * Raycasts the whole world, so any block between the eye and the target rejects the click.
     */
    WORLD,

    /**
     * Clips only the target block's shape, accepting a click through blocks in front of it.
     */
    TARGET_SHAPE,
}

/**
 * What a failed click verification reports.
 */
enum class FailedClick {
    /**
     * Report that the click is not possible.
     */
    NOTHING,

    /**
     * Fall back to [BlockPlacementTarget.blockHitResult], the click as target finding planned it.
     */
    PLANNED_HIT,
}

/**
 * Verifies that [this] target can be clicked right now, i.e. that the interaction would be accepted.
 *
 * The click is traced from the player's current eye, which is the eye the server uses when it processes the
 * interaction. [BlockPlacementTarget.rotation] may have been aimed from a predicted position instead, so this is
 * what decides whether the click actually happens.
 *
 * @param rotation the rotation the click is traced with
 * @param range the maximum distance of the trace
 * @param trace how the click is traced
 * @param onFailure what a failed verification reports
 * @return the hit to place with, or null if there is none
 */
fun BlockPlacementTarget.verifyClick(
    rotation: Rotation = this.rotation,
    range: Double = max(player.blockInteractionRange(), player.entityInteractionRange()),
    trace: ClickTrace = ClickTrace.WORLD,
    onFailure: FailedClick = FailedClick.NOTHING,
): BlockHitResult? {
    val hit = when (trace) {
        ClickTrace.WORLD -> traceFromPlayer(range = range, rotation = rotation)
        ClickTrace.TARGET_SHAPE -> raytraceBlock(
            range = range,
            rotation = rotation,
            pos = interactedBlockPos,
            state = interactedBlockPos.stateOrEmpty,
        )
    }

    if (hit != null && doesCrosshairTargetMatchRequirements(hit)) {
        return hit
    }

    return when (onFailure) {
        FailedClick.NOTHING -> null
        FailedClick.PLANNED_HIT -> blockHitResult
    }
}

/**
 * The context vanilla would place with when [this] is the context of clicking [pos], or null when that click does not
 * put the stack at [pos].
 *
 * [BlockItem.updatePlacementContext] runs first — scaffolding walks up to 7 blocks off the clicked one — and
 * [BlockPlaceContext.clickedPos] of that result is where the stack ends up.
 */
internal fun BlockPlaceContext.resolvePlacementAt(pos: BlockPos): BlockPlaceContext? {
    val blockItem = itemInHand.item as? BlockItem
        ?: return if (clickedPos == pos && canPlace()) this else null

    if (!blockItem.block.isEnabled(level.enabledFeatures())) {
        return null
    }

    val resolved = blockItem.updatePlacementContext(this) ?: return null
    return resolved.takeIf { it.clickedPos == pos && it.canPlace() }
}
