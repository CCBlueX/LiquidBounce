package net.ccbluex.liquidbounce.features.module.modules.world.traps.traps

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeInfo
import net.ccbluex.liquidbounce.features.module.modules.world.traps.BlockChangeIntent
import net.ccbluex.liquidbounce.features.module.modules.world.traps.IntentTiming
import net.ccbluex.liquidbounce.features.module.modules.world.traps.ModuleAutoTrap
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.PositionFactoryConfiguration
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
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
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import java.util.HashMap
import kotlin.math.pow

class IgnitionTrapPlanner(parent: Listenable) : TrapPlanner<IgnitionTrapPlanner.IgnitionIntentData>(
    parent,
    "Ignite",
    true
) {
    private val delay by int("Delay", 20, 0..400, "ticks")
    private val targetTracker = tree(TargetTracker())

    private val trapItems = arrayOf(Items.LAVA_BUCKET, Items.FLINT_AND_STEEL)
    private val trapWorthyBlocks = arrayOf(Blocks.LAVA, Blocks.FIRE)

    private val predictedPlayerStatesCache = HashMap<PlayerEntity, ArrayDeque<PredictedPlayerPos>>()

    private val SIMULATION_DISTANCE: Double = 10.0

    override fun plan(): BlockChangeIntent<IgnitionIntentData>? {
        targetTracker.validateLock { it.shouldBeAttacked() && it.boxedDistanceTo(player) in ModuleAutoTrap.range }

        val slot = findItemToIgnite() ?: return null

        val enemies = targetTracker.enemies()

        runSimulations(enemies)

        for (target in enemies) {
             if (target.isOnFire) {
                continue
            }
            if (target.boxedDistanceTo(player) !in ModuleAutoTrap.range) {
                continue
            }

            val targetPos = findPosForTrap(target) ?: continue

            val debuggedBox = target.getDimensions(EntityPose.STANDING).getBoxAt(targetPos)

            val pos = targetPos.toBlockPos()
            val state = pos.getState() ?: continue

            if (state.block in trapWorthyBlocks) {
                continue
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

            val currentTarget = findBestBlockPlacementTarget(pos, options) ?: continue

            targetTracker.lock(target)

            return BlockChangeIntent<IgnitionIntentData>(
                BlockChangeInfo.PlaceBlock(currentTarget),
                slot,
                IntentTiming.NEXT_PROPITIOUS_MOMENT,
                IgnitionIntentData(target, debuggedBox),
                this
            )
        }

        return null
    }

    private fun runSimulations(enemies: List<LivingEntity>) {
        val seenPlayers = HashSet<PlayerEntity>()

        for (enemy in enemies) {
            if (enemy !is PlayerEntity || enemy.squaredDistanceTo(player) > SIMULATION_DISTANCE.pow(2)) {
                continue
            }

            val simulation = PlayerSimulationCache.getSimulationForOtherPlayers(enemy)

            val predictedState = simulation.simulateBetween(0..25)

            var wasAirborne = !enemy.isOnGround

            var ticks = 1

            val predictedPos = predictedState.firstNotNullOfOrNull {
                if (wasAirborne && it.onGround) {
                    return@firstNotNullOfOrNull PredictedPlayerPos(it.pos, ticks, enemy.pos, false)
                }

                wasAirborne = !enemy.isOnGround
                ticks++

                null
            } ?: PredictedPlayerPos(null, null, enemy.pos, enemy.velocity.lengthSquared() < 0.05)

            seenPlayers.add(enemy)

            val simulationCache = this.predictedPlayerStatesCache.computeIfAbsent(enemy) { ArrayDeque() }

            simulationCache.addLast(predictedPos)

            while (simulationCache.size > 10) {
                simulationCache.removeFirst()
            }
        }

        this.predictedPlayerStatesCache.entries.removeIf { it.key !in seenPlayers }
    }

    private fun findPosForTrap(target: LivingEntity): Vec3d? {
        if (target !is PlayerEntity)
            return target.pos

        val simulationCache = this.predictedPlayerStatesCache[target] ?: return null

        val positions = simulationCache.mapNotNull {
            when {
                it.nextOnGround != null -> it.nextOnGround
                else -> null
            }
        }

        if (positions.size < 5 || simulationCache.last().ticksToGround ?: 0 < 8 && this.targetTracker.lockedOnTarget != target) {
            return null
        }

        val avg = positions.fold(Vec3d.ZERO) { acc, vec -> acc.add(vec) }.multiply(1.0 / positions.size)
        val std = positions.fold(0.0) { acc, vec -> acc + vec.subtract(avg).lengthSquared() }.let { Math.sqrt(it / positions.size) }

        ModuleDebug.debugGeometry(ModuleAutoTrap, "PredictedPlayerPos", ModuleDebug.DebuggedBox(target.dimensions.getBoxAt(positions.last()), Color4b.RED.alpha(127)))
        ModuleDebug.debugGeometry(ModuleAutoTrap, "PredictedPlayerPosStd", ModuleDebug.DebuggedBox(EntityDimensions.fixed((target.dimensions.width * std).toFloat(), 0.5F).getBoxAt(avg), Color4b.BLUE.alpha(127)))

        if (std < 1.9) {
            return positions.last()
        }

        return null
    }

    private fun findOffsetsForTarget(pos: Vec3d, dims: EntityDimensions, velocity: Vec3d, mustBeOnGround: Boolean): List<Vec3i> {
        val ticksToLookAhead = 5
        val blockPos = pos.toBlockPos()
        val normalizedStartBB = dims.getBoxAt(pos).offset(-blockPos.x.toDouble(), -blockPos.y.toDouble(), -pos.z.toInt().toDouble())
        val normalizedEnddBB = normalizedStartBB.offset(
            velocity.x * ticksToLookAhead,
            0.0,
            velocity.z * ticksToLookAhead
        )

        val searchBB = normalizedEnddBB

        if (searchBB.lengthX * searchBB.lengthY * searchBB.lengthZ > 30) {
            return listOf(Vec3i(0, 0, 0))
        }

        val from = normalizedStartBB.minPos.toVec3i()
        val to = normalizedStartBB.maxPos.toVec3i()

        val offsets = mutableListOf<Pair<Vec3i, Double>>()

        for (x in from.x..to.x) {
            for (y in from.y..to.y) {
                for (z in from.z..to.z) {
                    val offset = Vec3i(x, y, z)
                    val bp = blockPos.add(offset)

                    val bb = Box(BlockPos(offset))

                    if (!normalizedStartBB.intersects(bb) && !normalizedEnddBB.intersects(bb)) {
                        continue
                    }

                    val currentState = bp.getState()?.block

                    if (currentState in trapWorthyBlocks || currentState != Blocks.AIR) {
                        continue
                    }

                    // !(x == true)? I need it for null checking purposes
                    if (mustBeOnGround && ((bp.down().getState()?.isAir ?: true) == true)) {
                        continue
                    }

                    val intersect =
                        normalizedStartBB.intersection(bb).size + normalizedEnddBB.intersection(bb).size * 0.5

                    offsets.add(offset to intersect)
                }
            }
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

    private class PredictedPlayerPos(
        val nextOnGround: Vec3d?,
        val ticksToGround: Int?,
        val currPos: Vec3d,
        val isStationary: Boolean
    )

}
