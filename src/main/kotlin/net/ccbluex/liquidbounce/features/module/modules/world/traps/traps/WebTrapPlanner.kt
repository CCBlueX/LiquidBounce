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
package net.ccbluex.liquidbounce.features.module.modules.world.traps.traps

import it.unimi.dsi.fastutil.objects.ReferenceSet
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeInfo
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeIntent
import net.ccbluex.liquidbounce.features.module.modules.world.traps.IntentTiming
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap.targetTracker
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.PositionFactoryConfiguration
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

class WebTrapPlanner(parent: EventListener) : TrapPlanner<WebTrapPlanner.WebIntentData>(
    parent,
    "AutoWeb",
    true
) {

    override val trapItems: Set<Item> = ReferenceSet.of(Items.COBWEB)
    override val trapWorthyBlocks: Set<Block> = ReferenceSet.of(Blocks.COBWEB)

    override fun plan(enemies: List<LivingEntity>): BlockChangeIntent<WebIntentData>? {
        val slot = findSlotForTrap() ?: return null

        for (target in enemies) {
            val targetPos = TrapPlayerSimulation.findPosForTrap(
                target, isTargetLocked = targetTracker.target == target
            ) ?: continue

            val placementTarget = generatePlacementInfo(targetPos, target, slot) ?: continue

            targetTracker.target = target
            return BlockChangeIntent(
                BlockChangeInfo.PlaceBlock(placementTarget),
                slot,
                IntentTiming.NEXT_PROPITIOUS_MOMENT,
                WebIntentData(target, target.getDimensions(Pose.STANDING).makeBoundingBox(targetPos)),
                this
            )
        }

        return null
    }

    private fun generatePlacementInfo(
        targetPos: Vec3,
        target: LivingEntity,
        slot: HotbarItemSlot,
    ): BlockPlacementTarget? {
        val blockPos = targetPos.toBlockPos()

        if (blockPos.getState()?.block in trapWorthyBlocks) {
            return null
        }

        val offsetsForTargets = findOffsetsForTarget(
            targetPos,
            target.getDimensions(Pose.STANDING),
            target.position().subtract(target.lastPos),
            slot.itemStack.item == Items.COBWEB
        )

        val options = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                offsetsForTargets,
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            ),
            FaceHandlingOptions(
                NearestRotationTargetPositionFactory(PositionFactoryConfiguration(player.eyePosition, 0.5))
            ),
            stackToPlaceWith = slot.itemStack,
            PlayerLocationOnPlacement(position = player.position()),
        )

        return findBestBlockPlacementTarget(blockPos, options)
    }

    override fun validate(plan: BlockChangeIntent<WebIntentData>, raycast: BlockHitResult): Boolean {
        if (raycast.type != HitResult.Type.BLOCK) {
            return false
        }

        val actualPos = raycast.blockPos.offset(raycast.direction.unitVec3i)

        if (!AABB(actualPos).intersects(plan.planningInfo.targetBB)) {
            return false
        }

        return plan.slot.itemStack.item in trapItems
    }

    override fun onIntentFulfilled(intent: BlockChangeIntent<WebIntentData>) {
        targetTracker.target = intent.planningInfo.target
    }

    class WebIntentData(
        val target: LivingEntity,
        val targetBB: AABB
    )

}
