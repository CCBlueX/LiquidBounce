package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.inventory.useHotbarSlotOrOffhand
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.getRandomInRange
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryData
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.SpawnEggItem
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.exp
import kotlin.random.Random

object ModuleAntiInstakillAxe : ClientModule("AntiInstakillAxe", Category.PLAYER) {
    private val detectionRange by floatRange("DetectionRange", 0.5f..4.5f, 0.0f..6.0f, "blocks")
    private val maxPearlTicks by int("MaxPearlTicks", 3, 1..3, "ticks")
    private val pitchRange by floatRange("PearlPitchLimit", -90f..0f, -90f..90f)
    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private var currentPlan: Plan? = null
    private sealed class Plan {
        data class SpawnEggPlan(
            val slot: HotbarItemSlot,
            val target: PlayerEntity,
            val rotation: Rotation,
            val targetPos: BlockPos
        ) : Plan()

        data class PearlPlan(
            val slot: HotbarItemSlot,
            val rotation: Rotation,
            val landingPos: Vec3d
        ) : Plan()
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        val player = mc.player ?: return@handler
        val world = mc.world ?: return@handler

        val enemies = world.players.filter {
            it != player && it.isAlive
                && isAimingAtPlayer(it)
                && player.pos.distanceTo(it.pos) <= detectionRange.endInclusive
                && hasInstakillAxe(it)
        }
        if (enemies.isEmpty()) {
            currentPlan = null
            return@handler
        }

        val target = enemies.minByOrNull { player.pos.distanceTo(it.pos) } ?: return@handler
        currentPlan = plan(target)
        currentPlan?.let { plan ->
            val rotation = when (plan) {
                is Plan.SpawnEggPlan -> plan.rotation
                is Plan.PearlPlan -> plan.rotation
            }
            RotationManager.setRotationTarget(
                rotation,
                false,
                rotationsConfigurable,
                Priority.NORMAL,
                this
            )
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val plan = currentPlan ?: return@handler

        val isValidItem = when (plan) {
            is Plan.SpawnEggPlan -> isValidSpawnEgg(plan.slot.itemStack)
            is Plan.PearlPlan -> plan.slot.itemStack.item == Items.ENDER_PEARL
        }
        if (!isValidItem) {
            currentPlan = null
            return@handler
        }

        val target = when (plan) {
            is Plan.SpawnEggPlan -> plan.target
            is Plan.PearlPlan -> null
        }
        if (target != null && !target.isAlive) {
            currentPlan = null
            return@handler
        }

        CombatManager.pauseCombatForAtLeast(1)
        val rotation = when (plan) {
            is Plan.SpawnEggPlan -> plan.rotation
            is Plan.PearlPlan -> plan.rotation
        }
        val slot = when (plan) {
            is Plan.SpawnEggPlan -> plan.slot
            is Plan.PearlPlan -> plan.slot
        }
        useHotbarSlotOrOffhand(slot, 0, rotation.yaw, rotation.pitch)
        currentPlan = null
    }

    private fun findSpawnEggSlot(): HotbarItemSlot? {
        return Slots.OffhandWithHotbar.findClosestSlot { stack ->
            isValidSpawnEgg(stack)
        }
    }

    private fun findPearlSlot(): HotbarItemSlot? {
        return Slots.OffhandWithHotbar.findClosestSlot { stack ->
            stack.item == Items.ENDER_PEARL
        }
    }

    private fun isValidSpawnEgg(stack: ItemStack): Boolean {
        return stack.item is SpawnEggItem
    }

    private fun isAimingAtPlayer(enemy: PlayerEntity): Boolean {
        val player = mc.player ?: return false
        val eyePos = enemy.eyePos
        val direction = enemy.rotationVector.normalize()
        val raycastEnd = eyePos.add(direction.multiply(3.0))
        val box = player.box.expand(0.5)
        return box.raycast(eyePos, raycastEnd).isPresent
    }

    private fun hasInstakillAxe(player: PlayerEntity): Boolean {
        val mainHandStack = player.mainHandStack
        if (mainHandStack.item != Items.GOLDEN_AXE || mainHandStack.isEmpty) {
            return false
        }
        val sharpnessLevel = mainHandStack.getEnchantment(Enchantments.SHARPNESS)
        return sharpnessLevel > 10
    }

    private fun plan(target: PlayerEntity): Plan? {
        val spawnEggPlan = planSpawnEgg(target)
        if (spawnEggPlan != null) {
            return spawnEggPlan
        }
        return planPearl()
    }

    private fun planSpawnEgg(target: PlayerEntity): Plan.SpawnEggPlan? {
        val slot = findSpawnEggSlot() ?: return null
        val playerPos = mc.player?.pos ?: return null
        val world = mc.world ?: return null
        val targetPos = target.pos
        val direction = targetPos.subtract(playerPos).normalize()
        val maxDistance = detectionRange.endInclusive
        val stepSize = 0.5

        var distance = 1.0
        while (distance <= maxDistance) {
            val checkPos = playerPos.add(direction.multiply(distance))
            val targetBlockPos = BlockPos.ofFloored(checkPos)

            val worldState = targetBlockPos.getState() ?: return null
            if (!worldState.isAir) {
                distance += stepSize
                continue
            }

            val belowPos = targetBlockPos.down()
            val belowState = world.getBlockState(belowPos)
            if (belowState.isAir || !belowState.isFullCube(world, belowPos)) {
                distance += stepSize
                continue
            }

            val placementTarget = findBestBlockPlacementTarget(
                targetBlockPos,
                BlockPlacementTargetFindingOptions(
                    BlockOffsetOptions(
                        listOf(Vec3i.ZERO),
                        BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
                    ),
                    FaceHandlingOptions(CenterTargetPositionFactory),
                    stackToPlaceWith = slot.itemStack,
                    PlayerLocationOnPlacement(position = playerPos)
                )
            ) ?: return null

            return Plan.SpawnEggPlan(
                slot = slot,
                target = target,
                rotation = placementTarget.rotation,
                targetPos = targetBlockPos
            )
        }

        return null
    }

    @Suppress("NestedBlockDepth","CognitiveComplexMethod")
    private fun planPearl(): Plan.PearlPlan? {
        val slot = findPearlSlot() ?: return null

        var bestRotation: Rotation? = null
        var bestDistance = 0.0
        var bestLandingPos: Vec3d? = null
        val minTemperature = 0.01f
        val temperatureDecayRate = 0.97f
        var temperature = 20f
        var currentRotation = Rotation(
            getRandomInRange(-180f, 180f),
            getRandomInRange(pitchRange.start, pitchRange.endInclusive))
        var currentDistance = 0.0
        var iterations = 0

        while (iterations < 1000 && temperature >= minTemperature) {

            val newRotation = Rotation(
                (currentRotation.yaw + getRandomInRange(-temperature * 18f, temperature * 18f)),
                (currentRotation.pitch + getRandomInRange(-temperature * 9f, temperature * 9f)).coerceIn(pitchRange)
            )

            val trajectoryRenderer = TrajectoryInfoRenderer.getHypotheticalTrajectory(
                player,
                TrajectoryData.getRenderedTrajectoryInfo(player, Items.ENDER_PEARL, false) ?: return null,
                newRotation
            )

            val simulationResult = trajectoryRenderer.runSimulation(maxPearlTicks)
            val hitResult = simulationResult.hitResult

            if (hitResult is BlockHitResult) {
                val landingPos = hitResult.pos
                val blockPos = BlockPos.ofFloored(landingPos)
                val belowPos = blockPos.down()
                val belowState = mc.world?.getBlockState(belowPos) ?: return null

                if (!belowState.isAir && belowState.isFullCube(mc.world, belowPos)) {
                    val distance = player.pos.distanceTo(landingPos)
                    if (distance > bestDistance) {
                        bestDistance = distance
                        bestRotation = newRotation
                        bestLandingPos = landingPos
                    }
                }
            }

            val delta = bestDistance - currentDistance
            if (delta > 0 || Random.nextDouble() < exp(-delta / temperature)) {
                currentRotation = newRotation
                currentDistance = bestDistance
            }

            temperature *= temperatureDecayRate
            iterations++
        }

        return if (bestRotation != null && bestLandingPos != null) {
            Plan.PearlPlan(
                slot = slot,
                rotation = bestRotation,
                landingPos = bestLandingPos
            )
        } else {
            null
        }
    }

    override fun onEnabled() {
        currentPlan = null
    }

    override fun onDisabled() {
        currentPlan = null
    }
}
