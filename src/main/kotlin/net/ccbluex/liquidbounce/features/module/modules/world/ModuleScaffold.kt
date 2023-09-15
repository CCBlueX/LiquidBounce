/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold.Eagle.blocksToEagle
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold.Eagle.edgeDistance
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold.Slow.slowSpeed
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.targetFinding.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.block.SideShapeType
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Scaffold module
 *
 * Places blocks under you.
 */
object ModuleScaffold : Module("Scaffold", Category.WORLD) {

    private val silent by boolean("Silent", true)
    private var delay by intRange("Delay", 3..5, 0..40)

    private val swing by boolean("Swing", true)

    object Eagle : ToggleableConfigurable(this, "Eagle", false) {
        val blocksToEagle by int("BlocksToEagle", 1, 1..10)
        val edgeDistance by float("EagleEdgeDistance", 0.01f, 0.01f..0.5f)
    }

    val down by boolean("Down", false)

    // Rotation
    private val rotationsConfigurable = tree(RotationsConfigurable())
    private val aimMode = enumChoice("RotationMode", AimMode.STABILIZED, AimMode.values())

    object AdvancedRotation : ToggleableConfigurable(this, "AdvancedRotation", false) {
        val DEFAULT_XZ_RANGE = 0.1f..0.9f
        val DEFAULT_Y_RANGE = 0.33f..0.95f

        val xRange by floatRange("XRange", DEFAULT_XZ_RANGE, 0.0f..1.0f)
        val yRange by floatRange("YRange", DEFAULT_Y_RANGE, 0.0f..1.0f)
        val zRange by floatRange("ZRange", DEFAULT_XZ_RANGE, 0.0f..1.0f)
        val step by float("Step", 0.1f, 0f..1f)
    }

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private val minDist by float("MinDist", 0.0f, 0.0f..0.25f)
    private val zitterModes = choices(
        "ZitterMode",
        Off, arrayOf(
            Off,
            Teleport,
            Smooth
        )
    )
    private val timer by float("Timer", 1f, 0.01f..10f)
    private val speedModifier by float("SpeedModifier", 1f, 0f..3f)

    object Slow : ToggleableConfigurable(this, "Slow", false) {
        val slowSpeed by float("SlowSpeed", 0.6f, 0.1f..3f)
    }

    private val safeWalk by boolean("SafeWalk", true)
    private val sameY by boolean("SameY", false)
    private var currentTarget: BlockPlacementTarget? = null


    private val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1, -2, 2))
    private val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1))

    init {
        tree(Eagle)
        tree(Slow)
        tree(AdvancedRotation)
    }

    var randomization = Random.nextDouble(-0.02, 0.02)
    private var startY = 0
    private var placedBlocks = 0

    /**
     * This comparator will estimate the value of a block. If this comparator says that Block A > Block B, Scaffold will
     * prefer Block A over Block B.
     * The chain will prefer the block that is solid. If both are solid, it goes to the next criteria
     * (in this case full cube) and so on
     */
    private val BLOCK_COMPARATOR = ComparatorChain(
        PreferSolidBlocks,
        PreferFullCubeBlocks,
        PreferLessSlipperyBlocks,
        PreferAverageHardBlocks,
        PreferHigherStackSize
    )

    private val shouldGoDown: Boolean
        get() = this.down && mc.options.sneakKey.isPressed

    override fun enable() {
        // Chooses a new randomization value
        randomization = Random.nextDouble(-0.01, 0.01)
        startY = player.blockPos.y
        super.enable()
    }

    override fun disable() {
        moveKeys.forEach {
            it.enforced = null
        }
        // Makes you shift until first block placed, so with eagle enabled you won't fall off, when enabled
        placedBlocks = 0
        mc.timer.timerSpeed = 1f
        SilentHotbar.resetSlot(this)
    }

    val rotationUpdateHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state != EventState.PRE) {
            return@handler
        }

        val blockInHotbar = findBestValidHotbarSlotForTarget()

        val bestStick = if (blockInHotbar == null) {
            ItemStack(Items.SANDSTONE, 64)
        } else {
            player.inventory.getStack(blockInHotbar)
        }

        val searchOptions = BlockPlacementTargetFindingOptions(
            if (shouldGoDown) INVESTIGATE_DOWN_OFFSETS else NORMAL_INVESTIGATION_OFFSETS,
            bestStick,
            getFacePositionFactoryForConfig()
        )

        currentTarget = findBestBlockPlacementTarget(getTargetedPosition(), searchOptions)

        val target = currentTarget ?: return@handler

        RotationManager.aimAt(
            target.rotation,

            openInventory = ignoreOpenInventory,
            configurable = rotationsConfigurable
        )
    }

    fun getFacePositionFactoryForConfig(): FaceTargetPositionFactory {
        val config = PositionFactoryConfiguration(
            mc.player!!.eyePos,
            if (AdvancedRotation.enabled) AdvancedRotation.xRange.toDouble() else AdvancedRotation.DEFAULT_XZ_RANGE.toDouble(),
            if (AdvancedRotation.enabled) AdvancedRotation.yRange.toDouble() else AdvancedRotation.DEFAULT_Y_RANGE.toDouble(),
            if (AdvancedRotation.enabled) AdvancedRotation.zRange.toDouble() else AdvancedRotation.DEFAULT_XZ_RANGE.toDouble(),
            AdvancedRotation.step.toDouble(),
            randomization
        )

        return when (this.aimMode.get()) {
            AimMode.CENTER -> CenterTargetPositionFactory
            AimMode.RANDOM -> RandomTargetPositionFactory(config)
            AimMode.STABILIZED -> StabilizedTargetPositionFactory(config)
        }
    }

    val moveHandler = repeatable {
        if (Slow.enabled) {
            player.velocity.x *= slowSpeed
            player.velocity.z *= slowSpeed
        }
        mc.timer.timerSpeed = timer
    }

    val networkTickHandler = repeatable {
        val target = currentTarget ?: return@repeatable
        val currentRotation = RotationManager.currentRotation ?: return@repeatable
        val currentCrosshairdTarget = raycast(4.5, currentRotation) ?: return@repeatable

        // Is the target the crosshair points to well-adjusted to our target?
        if (!target.doesCrosshairTargetFullfitRequirements(currentCrosshairdTarget) || !isValidCrosshairTarget(currentCrosshairdTarget)) {
            return@repeatable
        }

        var hasBlockInMainHand = isValidBlock(player.inventory.getStack(player.inventory.selectedSlot))
        val hasBlockInOffHand = isValidBlock(player.offHandStack)

        // Handle silent block selection
        if (silent && !hasBlockInMainHand && !hasBlockInOffHand) {
            val bestMainHandSlot = findBestValidHotbarSlotForTarget()

            if (bestMainHandSlot != null) {
                SilentHotbar.selectSlotSilently(this, bestMainHandSlot)

                hasBlockInMainHand = true
            } else {
                SilentHotbar.resetSlot(this)
            }
        } else {
            SilentHotbar.resetSlot(this)
        }

        if (!hasBlockInMainHand && !hasBlockInOffHand) {
            return@repeatable
        }

        // no need for additional checks
        val handToInteractWith = if (hasBlockInMainHand) Hand.MAIN_HAND else Hand.OFF_HAND
        val result = interaction.interactBlock(
            player,
            handToInteractWith,
            currentCrosshairdTarget
        )

        if (!result.isAccepted)
            return@repeatable

        if (Eagle.enabled) {
            placedBlocks += 1

            if (placedBlocks > blocksToEagle)
                placedBlocks = 0
        }
        if (player.isOnGround) {
            player.velocity.x *= speedModifier
            player.velocity.z *= speedModifier
        }
        if (result.shouldSwingHand() && swing) {
            player.swingHand(handToInteractWith)
        }

        currentTarget = null

        wait(delay.random())
    }

    private fun findBestValidHotbarSlotForTarget(): Int? {
        val bestSlot = (0..8)
            .filter { isValidBlock(player.inventory.getStack(it)) }
            .mapNotNull {
                val stack = player.inventory.getStack(it)

                if (stack.item is BlockItem) Pair(it, stack)
                else null
            }
            .maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR.compare(o1.second, o2.second) }?.first

        return bestSlot
    }

    private fun isValidCrosshairTarget(rayTraceResult: BlockHitResult): Boolean {
        val eyesPos = player.eyes
        val hitVec = rayTraceResult.pos

        val diffX = hitVec.x - eyesPos.x
        val diffZ = hitVec.z - eyesPos.z

        val side = rayTraceResult.side

        // Apply minDist
        if (side != Direction.UP && side != Direction.DOWN) {
            val diff = if (side == Direction.NORTH || side == Direction.SOUTH) diffZ else diffX

            if (abs(diff) < minDist)
                return false
        }

        return true
    }

    val repeatable = handler<StateUpdateEvent> {
        if (shouldDisableSafeWalk()) {
            it.state.enforceEagle = false
        } else if (!player.abilities.flying && Eagle.enabled && player.isCloseToEdge(edgeDistance.toDouble()) && placedBlocks == 0) {
            it.state.enforceEagle = true
        }
    }

    private object Off : Choice("Off") {
        override val parent: ChoiceConfigurable
            get() = zitterModes
    }

    private object Teleport : Choice("Teleport") {
        override val parent: ChoiceConfigurable
            get() = zitterModes

        private val speed by float("Speed", 0.13f, 0.1f..0.3f)
        private val strength by float("Strength", 0.05f, 0f..0.2f)
        val groundOnly by boolean("GroundOnly", true)
        var zitterDirection = false

        val repeatable = repeatable {
            if (player.isOnGround || !groundOnly) {
                player.strafe(speed = speed.toDouble())
                val yaw = Math.toRadians(player.yaw + if (zitterDirection) 90.0 else -90.0)
                player.velocity.x -= sin(yaw) * strength
                player.velocity.z += cos(yaw) * strength
                zitterDirection = !zitterDirection
            }
        }
    }

    private object Smooth : Choice("Smooth") {

        override val parent: ChoiceConfigurable
            get() = zitterModes

        val zitterDelay by int("Delay", 100, 0..500)
        val groundOnly by boolean("GroundOnly", true)
        val zitterTimer = Chronometer()
        var zitterDirection = false

        val repeatable = repeatable {
            if (!player.isOnGround && groundOnly)
                return@repeatable

            val pressedOnKeyboardKeys = moveKeys.filter { it.pressedOnKeyboard }

            when (pressedOnKeyboardKeys.size) {
                0 -> {
                    moveKeys.forEach {
                        it.enforced = null
                    }
                }

                1 -> {
                    val key = pressedOnKeyboardKeys.first()
                    val possible = moveKeys.filter { it != key && it != key.opposite }
                    zitter(possible)
                    key.opposite!!.enforced = false
                    key.enforced = true
                }

                2 -> {
                    zitter(pressedOnKeyboardKeys)
                    moveKeys.filter { pressedOnKeyboardKeys.contains(it) }.forEach {
                        it.opposite!!.enforced = false
                    }
                }
            }
            if (zitterTimer.hasElapsed(zitterDelay.toLong())) {
                zitterDirection = !zitterDirection
                zitterTimer.reset()
            }

        }

        fun zitter(first: List<KeyBinding>) {
            if (zitterDirection) {
                first.first().enforced = true
                first.last().enforced = false
            } else {
                first.first().enforced = false
                first.last().enforced = true
            }
        }
    }

    val safeWalkHandler = handler<PlayerSafeWalkEvent> { event ->
        if (safeWalk)
            event.isSafeWalk = !shouldDisableSafeWalk()
    }

    private fun isValidBlock(stack: ItemStack?): Boolean {
        if (stack == null) return false

        val item = stack.item

        if (item !is BlockItem)
            return false

        val block = item.block

        if (!block.defaultState.isSideSolid(world, BlockPos.ORIGIN, Direction.UP, SideShapeType.CENTER))
            return false

        return !DISALLOWED_BLOCKS_TO_PLACE.contains(block)
    }

    private fun getTargetedPosition(): BlockPos {
        if (shouldGoDown) {
            return player.blockPos.add(0, -2, 0)
        }

        return if (sameY)
            BlockPos(player.blockPos.x, startY - 1, player.blockPos.z)
        else
            player.blockPos.add(0, -1, 0)
    }

    private fun shouldDisableSafeWalk() = shouldGoDown && player.blockPos.add(0, -2, 0).canStandOn()


    private fun commonOffsetToInvestigate(xzOffsets: List<Int>): List<Vec3i> {
        return xzOffsets.flatMap { x ->
            xzOffsets.flatMap { z ->
                (0 downTo -1).flatMap { y ->
                    listOf(Vec3i(x, y, z))
                }
            }
        }
    }
}
