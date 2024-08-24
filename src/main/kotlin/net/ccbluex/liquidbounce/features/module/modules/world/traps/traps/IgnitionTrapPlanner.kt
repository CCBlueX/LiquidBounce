package net.ccbluex.liquidbounce.features.module.modules.world.traps.traps

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeInfo
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeIntent
import net.ccbluex.liquidbounce.features.module.modules.world.traps.IntentTiming
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap
import net.ccbluex.liquidbounce.utils.block.forEachBlockPosBetween
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.PositionFactoryConfiguration
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.inventory.Hotbar
import net.ccbluex.liquidbounce.utils.math.size
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3i
import net.minecraft.block.Blocks
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
import net.minecraft.entity.LivingEntity
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

class IgnitionTrapPlanner(parent: Listenable) : TrapPlanner<IgnitionTrapPlanner.IgnitionIntentData>(
    parent,
    "Ignite",
    true
) {
    private val targetTracker = tree(TargetTracker())

    private val trapItems = arrayOf(Items.LAVA_BUCKET, Items.FLINT_AND_STEEL)
    private val trapWorthyBlocks = arrayOf(Blocks.LAVA, Blocks.FIRE)


    override fun plan(): BlockChangeIntent<IgnitionIntentData>? {
        targetTracker.validateLock { it.shouldBeAttacked() && it.boxedDistanceTo(player) in ModuleAutoTrap.range }

        val slot = findItemToIgnite() ?: return null

        val enemies = targetTracker.enemies()

        TrapPlayerSimulation.runSimulations(enemies)

        for (target in enemies) {
            if (!shouldTarget(target)) {
                continue
            }
            val targetPos = TrapPlayerSimulation.findPosForTrap(
                target,
                isTargetLocked = this.targetTracker.lockedOnTarget == target
            ) ?: continue

            val placementTarget = generatePlacementInfo(targetPos, target, slot) ?: continue

            targetTracker.lock(target)

            return BlockChangeIntent<IgnitionIntentData>(
                BlockChangeInfo.PlaceBlock(placementTarget ),
                slot,
                IntentTiming.NEXT_PROPITIOUS_MOMENT,
                IgnitionIntentData(target, target.getDimensions(EntityPose.STANDING).getBoxAt(targetPos)),
                this
            )
        }

        return null
    }

    private fun shouldTarget(target: LivingEntity): Boolean {
        return !target.isOnFire && target.boxedDistanceTo(player) in ModuleAutoTrap.range
    }

    private fun generatePlacementInfo(
        targetPos: Vec3d,
        target: LivingEntity,
        slot: HotbarItemSlot,
    ): BlockPlacementTarget? {
        val blockPos = targetPos.toBlockPos()

        if (blockPos.getState()?.block in trapWorthyBlocks) {
            return null
        }

        val offsetsForTargets = findOffsetsForTarget(
            targetPos,
            target.getDimensions(EntityPose.STANDING),
            target.pos.subtract(target.prevPos),
            slot.itemStack.item == Items.FLINT_AND_STEEL
        )

        val options = BlockPlacementTargetFindingOptions(
            offsetsForTargets,
            slot.itemStack,
            NearestRotationTargetPositionFactory(PositionFactoryConfiguration(player.eyePos, 0.5)),
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            player.pos
        )

        return findBestBlockPlacementTarget(blockPos, options)
    }

    private fun findOffsetsForTarget(
        pos: Vec3d,
        dims: EntityDimensions,
        velocity: Vec3d,
        mustBeOnGround: Boolean
    ): List<Vec3i> {
        val ticksToLookAhead = 5
        val blockPos = pos.toBlockPos()
        val normalizedStartBB =
            dims.getBoxAt(pos).offset(-blockPos.x.toDouble(), -blockPos.y.toDouble(), -pos.z.toInt().toDouble())
        val normalizedEnddBB = normalizedStartBB.offset(
            velocity.x * ticksToLookAhead,
            0.0,
            velocity.z * ticksToLookAhead
        )

        val searchBB = normalizedEnddBB

        if (searchBB.size > 30) {
            return listOf(Vec3i(0, 0, 0))
        }

        return findOffsetsBetween(normalizedStartBB, normalizedEnddBB, blockPos, mustBeOnGround)
    }

    private fun findOffsetsBetween(
        startBox: Box,
        endBox: Box,
        offsetPos: BlockPos,
        mustBeOnGround: Boolean
    ): List<Vec3i> {
        val offsets = mutableListOf<Pair<Vec3i, Double>>()

        forEachBlockPosBetween(startBox.minPos.toVec3i(), startBox.maxPos.toVec3i()) { offset ->
            val bp = offsetPos.add(offset)

            val bb = Box(BlockPos(offset))

            if (!startBox.intersects(bb) && !endBox.intersects(bb)) {
                return@forEachBlockPosBetween
            }

            val currentState = bp.getState()?.block

            if (currentState in trapWorthyBlocks || currentState != Blocks.AIR) {
                return@forEachBlockPosBetween
            }

            // !(x == true)? I need it for null checking purposes
            if (mustBeOnGround && ((bp.down().getState()?.isAir ?: true) == true)) {
                return@forEachBlockPosBetween
            }

            val intersect =
                startBox.intersection(bb).size + endBox.intersection(bb).size * 0.5

            offsets.add(offset to intersect)
        }

        offsets.sortByDescending { it.second }

        return offsets.map { it.first }
    }

    override fun validate(plan: BlockChangeIntent<IgnitionIntentData>, raycast: BlockHitResult): Boolean {
        if (raycast.type != HitResult.Type.BLOCK) {
            return false
        }

        val actualPos = raycast.blockPos.add(raycast.side.vector)

        if (!Box(actualPos).intersects(plan.planningInfo.targetBB)) {
            return false
        }

        return plan.slot.itemStack.item in trapItems
    }

    override fun onIntentFullfilled(intent: BlockChangeIntent<IgnitionIntentData>) {
        this.targetTracker.lock(intent.planningInfo.target, reportToUI = false)
    }

    private fun findItemToIgnite(): HotbarItemSlot? {
        return Hotbar.findClosestItem(trapItems)
    }

    class IgnitionIntentData(
        val target: LivingEntity,
        val targetBB: Box
    )

}
