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
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold.AimMode.*
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold.Eagle.blocksToEagle
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold.Eagle.edgeDistance
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleScaffold.Slow.slowSpeed
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.extensions.getFace
import net.ccbluex.liquidbounce.utils.item.notABlock
import net.ccbluex.liquidbounce.utils.kotlin.step
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.block.ShapeContext
import net.minecraft.block.SideShapeType
import net.minecraft.block.SlabBlock
import net.minecraft.block.StairsBlock
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.*
import kotlin.random.Random

/**
 * Scaffold module
 *
 * Places blocks under you.
 */
object ModuleScaffold : Module("Scaffold", Category.WORLD) {

    enum class AimMode(override val choiceName: String) : NamedChoice {
        CENTER("Center"),
        RANDOM("Random"),
        STABILIZED("Stabilized"),
        CLOSE_ROTATION("CloseRotation");
    }

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
    private val aimMode = enumChoice("RotationMode", STABILIZED, AimMode.values())

    object AdvancedRotation : ToggleableConfigurable(this, "AdvancedRotation", false) {
        val xRange by floatRange("XRange", 0f..1f, 0f..1f)
        val yRange by floatRange("YRange", 0f..1f, 0f..1f)
        val zRange by floatRange("ZRange", 0f..1f, 0f..1f)
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
    private var currentTarget: Target? = null

    init {
        tree(Eagle)
        tree(Slow)
        tree(AdvancedRotation)
    }

    var randomization = Random.nextDouble(-0.01, 0.01)
    private var startY = 0
    private var placedBlocks = 0
    private val BLOCK_COMPARATOR = ComparatorChain<ItemStack>(
        { o1, o2 ->
            compareByCondition(
                o1,
                o2
            ) { (it.item as BlockItem).block.defaultState.isSolid }
        },
        { o1, o2 ->
            compareByCondition(
                o1,
                o2
            ) {
                (it.item as BlockItem).block.defaultState.isFullCube(
                    world,
                    BlockPos(
                        0,
                        0,
                        0
                    )
                )
            }
        },
        { o1, o2 ->
            (o2.item as BlockItem).block.slipperiness.compareTo((o1.item as BlockItem).block.slipperiness)
        },
        Comparator.comparingDouble {
            (
                    1.5 - (it.item as BlockItem).block.defaultState.getHardness(
                        world,
                        BlockPos(0, 0, 0)
                    )
                    ).absoluteValue
        },
        { o1, o2 -> o2.count.compareTo(o1.count) }
    )

    private val shouldGoDown: Boolean
        get() = this.down && mc.options.sneakKey.isPressed

    override fun enable() {
        // Chooses a new randomization value
        randomization = Random.nextDouble(-0.01, 0.01)
        startY = player.blockPos.y
        super.enable()
    }

    val rotationUpdateHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state != EventState.PRE) {
            return@handler
        }

        currentTarget = updateTarget(getTargetedPosition())

        val target = currentTarget ?: return@handler

        RotationManager.aimAt(
            target.rotation,
            openInventory = ignoreOpenInventory,
            configurable = rotationsConfigurable
        )
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
        val rayTraceResult = raycast(4.5, currentRotation) ?: return@repeatable

        if (rayTraceResult.type != HitResult.Type.BLOCK || rayTraceResult.blockPos != target.blockPos || rayTraceResult.side != target.direction || rayTraceResult.pos.y < target.minY || !isValidTarget(
                rayTraceResult
            )
        ) {
            return@repeatable
        }

        var hasBlockInMainHand = isValidBlock(player.inventory.getStack(player.inventory.selectedSlot), target)
        val hasBlockInOffHand = isValidBlock(player.offHandStack, target)

        // Handle silent block selection
        if (silent && !hasBlockInMainHand && (!hasBlockInOffHand || !canUseOffHand(player.inventory.mainHandStack))) {
            val mainHandSlot = (0..8).filter { isValidBlock(player.inventory.getStack(it), target) }.mapNotNull {
                val stack = player.inventory.getStack(it)

                if (stack.item is BlockItem) Pair(it, stack)
                else null
            }.maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR.compare(o1.second, o2.second) }?.first

            if (mainHandSlot != null) {
                SilentHotbar.selectSlotSilently(this, mainHandSlot)

                hasBlockInMainHand = true
            } else {
                if (hasBlockInOffHand) {
                    // We try to switch to the hotbar slot, which allows using second hand slot
                    val offHandSlot = (0..8).filter { canUseOffHand(player.inventory.getStack(it)) }
                    if (offHandSlot.isNotEmpty()) {
                        SilentHotbar.selectSlotSilently(this, offHandSlot.first())

                        hasBlockInMainHand = true
                    }
                }
            }
        } else {
            SilentHotbar.resetSlot(this)
        }

        if (!hasBlockInMainHand && (!hasBlockInOffHand && !canUseOffHand(player.inventory.mainHandStack))) {
            return@repeatable
        }

        // no need for additional checks
        val handToTry = if (hasBlockInMainHand) Hand.MAIN_HAND else Hand.OFF_HAND
        val result = interaction.interactBlock(
            player,
            handToTry,
            rayTraceResult
        )

        if (result.isAccepted) {
            if (Eagle.enabled)
                placedBlocks = ++placedBlocks % blocksToEagle
            if (player.isOnGround) {
                player.velocity.x *= speedModifier
                player.velocity.z *= speedModifier
            }
            if (result.shouldSwingHand() && swing) {
                player.swingHand(handToTry)
            }

            currentTarget = null
            wait(delay.random())
        }
    }

    private fun isValidTarget(rayTraceResult: BlockHitResult): Boolean {
        val eyesPos = player.eyes
        val hitVec = rayTraceResult.pos

        val diffX = hitVec.x - eyesPos.x
        val diffZ = hitVec.z - eyesPos.z

        val side = rayTraceResult.side

        if (side != Direction.UP && side != Direction.DOWN) {
            val diff: Double = abs(if (side == Direction.NORTH || side == Direction.SOUTH) diffZ else diffX)

            if (diff < minDist) return false
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
            if (player.isOnGround || !groundOnly) {
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

    override fun disable() {
        moveKeys.forEach {
            it.enforced = null
        }
        // Makes you shift until first block placed, so with eagle enabled you won't fall off, when enabled
        placedBlocks = 0
        mc.timer.timerSpeed = 1f
        SilentHotbar.resetSlot(this)
    }

    private fun canUseOffHand(stack: ItemStack?): Boolean {
        stack ?: return true

        val item = stack.item

        return item !is BlockItem
    }

    private fun isValidBlock(stack: ItemStack?, target: Target): Boolean {
        if (stack == null) return false

        val item = stack.item

        if (item !is BlockItem) return false

        val block = item.block

        return block.defaultState.isSideSolid(
            world,
            target.blockPos,
            target.direction,
            SideShapeType.CENTER
        ) && !notABlock.contains(block)
    }

    private fun getTargetedPosition(): BlockPos {
        if (shouldGoDown) {
            return player.blockPos.add(0, -2, 0)
        }
        if (sameY)
            return BlockPos(player.blockPos.x, startY - 1, player.blockPos.z)
        else
            return player.blockPos.add(0, -1, 0)
    }

    fun updateTarget(pos: BlockPos, lavaBucket: Boolean = false): Target? {
        val state = pos.getState()

        if (state!!.isSideSolid(mc.world!!, pos, Direction.UP, SideShapeType.CENTER)) {
            return null
        }

        val offsetsToInvestigate = if (lavaBucket) listOf(
            Vec3i(0, 0, 0)
        )
        else {
            val vec = if (shouldGoDown) {
                listOf(0, -1, 1, -2, 2)
            } else {
                listOf(0, -1, 1)
            }

            vec.flatMap { x ->
                vec.flatMap { z ->
                    (0 downTo -1).flatMap { y ->
                        listOf(Vec3i(x, y, z))
                    }
                }
            }
        }

        for (vec3i in offsetsToInvestigate) {
            val posToInvestigate = pos.add(vec3i)
            val blockStateToInvestigate = posToInvestigate.getState()!!

            if (blockStateToInvestigate.isSideSolid(mc.world!!, posToInvestigate, Direction.UP, SideShapeType.CENTER)) {
                continue
            }

            val first = if (!blockStateToInvestigate.isAir && blockStateToInvestigate.canReplace(
                    ItemPlacementContext(
                        player,
                        Hand.MAIN_HAND,
                        player.inventory.getStack(SilentHotbar.serversideSlot),
                        BlockHitResult(
                            Vec3d.of(posToInvestigate),
                            Direction.UP,
                            posToInvestigate,
                            false
                        )
                    )
                )
            ) {
                Direction.values().mapNotNull { direction ->

                    // distance to the center of the block, multiplied by 0.5 (?)
                    val delta = player.eyes.subtract(
                        posToInvestigate.toCenterPos().add(Vec3d.of(direction.vector).multiply(0.5))
                    )

                    val angle = delta.dotProduct(Vec3d.of(direction.vector)) / delta.length()

                    if (angle < 0) return@mapNotNull null

                    Triple(direction, posToInvestigate, angle)
                }.maxByOrNull { it.second }
            } else {
                Direction.values().mapNotNull { direction ->
                    val currPos = posToInvestigate.add(direction.opposite.vector)
                    val currState = currPos.getState() ?: return@mapNotNull null

                    if (currState.isAir || currState.isReplaceable) {
                        return@mapNotNull null
                    }

                    val delta = player.eyes.subtract(
                        currPos.toCenterPos().add(Vec3d.of(direction.vector).multiply(0.5))
                    )

                    val angle = delta.dotProduct(Vec3d.of(direction.vector)) / delta.length()

                    if (angle < 0) return@mapNotNull null

                    Triple(direction, currPos, angle)
                }.maxByOrNull { it.second }
            }

            if (first != null) {
                val currPos = first.second

                val currState = currPos.getState()!!
                val currBlock = currState.block

                val truncate = currBlock is StairsBlock || currBlock is SlabBlock // TODO Find this out

                val face = currState.getOutlineShape(
                    mc.world,
                    currPos,
                    ShapeContext.of(player)
                ).boundingBoxes.mapNotNull {
                    var face = it.getFace(first.first)

                    if (truncate) {
                        face = face.truncate(0.5) ?: return@mapNotNull null
                    }

                    val rotation = when (aimMode.value) {
                        CENTER -> {
                            face.center
                        }

                        RANDOM -> {
                            face.random
                        }

                        CLOSE_ROTATION -> {
                            face.closeRotation(currPos)
                        }

                        STABILIZED -> {
                            face.stabilized(currPos)
                        }
                    }
                    Pair(
                        face,
                        rotation
                    )
                }.maxWithOrNull(
                    Comparator.comparingDouble<Pair<Face, Vec3d>> {
                        it.second.subtract(
                            Vec3d(
                                0.5,
                                0.5,
                                0.5
                            )
                        ).multiply(Vec3d.of(first.first.vector)).lengthSquared()
                    }.thenComparingDouble { it.second.y }
                ) ?: continue

                val rotation = RotationManager.makeRotation(face.second.add(Vec3d.of(currPos)), player.eyes)

                return Target(
                    currPos,
                    first.first,
                    face.first.from.y + currPos.y,
                    rotation
                )
            }
        }

        return null
    }

    val safeWalkHandler = handler<PlayerSafeWalkEvent> { event ->
        if (safeWalk)
            event.isSafeWalk = !shouldDisableSafeWalk()
    }

    private fun shouldDisableSafeWalk() = shouldGoDown && player.blockPos.add(0, -2, 0).canStandOn()

    data class Face(val from: Vec3d, val to: Vec3d) {
        private val gcd = RotationManager.gcd.coerceIn(0.02, 0.15)
        private val xRange = if (AdvancedRotation.enabled) AdvancedRotation.xRange.toDouble() else gcd..1 - gcd
        private val yRange = if (AdvancedRotation.enabled) AdvancedRotation.yRange.toDouble() else gcd..1 - gcd
        private val zRange = if (AdvancedRotation.enabled) AdvancedRotation.zRange.toDouble() else gcd..1 - gcd
        private val step = if (AdvancedRotation.enabled) AdvancedRotation.step.toDouble() else gcd

        val area: Double
            get() {
                val l = to.x - from.x
                val b = to.y - from.y
                val h = to.z - from.z

                return (l * b + b * h + l * h) * 2.0
            }

        val center: Vec3d
            get() = Vec3d(
                (to.x + from.x) * 0.5,
                (to.y + from.y) * 0.5,
                (to.z + from.z) * 0.5
            )

        fun stabilized(pos: BlockPos, eyes: Vec3d = player.eyes): Vec3d {
            val pitchToCompare =
                if (RotationManager.targetRotation != null) RotationManager.targetRotation!!.yaw else player.yaw
            val x = (player.pos.x - floor(player.pos.x) + randomization).coerceIn(xRange)
            val z = (player.pos.z - floor(player.pos.z) + randomization).coerceIn(zRange)
            return rotationList(x..x, yRange, z..z, step).minBy {
                abs(
                    RotationManager.angleDifference(
                        RotationManager.makeRotation(
                            it.add(
                                Vec3d.of(pos)
                            ), eyes
                        ).pitch,
                        pitchToCompare
                    )
                )
            }
        }

        val random: Vec3d
            get() {
                return Vec3d(
                    if (from.x != to.x) Random.nextDouble(
                        xRange.start,
                        (xRange.endInclusive - step).coerceAtLeast(xRange.start)
                    ) else from.x,
                    if (from.y != to.y) Random.nextDouble(
                        yRange.start,
                        (yRange.endInclusive - step).coerceAtLeast(yRange.start)
                    ) else from.y,
                    if (from.z != to.z) Random.nextDouble(
                        zRange.start,
                        (zRange.endInclusive - step).coerceAtLeast(zRange.start)
                    ) else from.z
                )
            }

        private fun rotationList(
            xRange: ClosedFloatingPointRange<Double>,
            yRange: ClosedFloatingPointRange<Double>,
            zRange: ClosedFloatingPointRange<Double>,
            step: Double
        ): MutableList<Vec3d> {
            // Collects all possible rotations
            val possibleRotations = mutableListOf<Vec3d>()

            val currentRangeX = if (from.x == to.x) from.x..to.x else xRange
            val currentRangeY = if (from.y == to.y) from.y..to.y else yRange
            val currentRangeZ = if (from.z == to.z) from.z..to.z else zRange

            for (x in currentRangeX step step) {
                for (y in currentRangeY step step) {
                    for (z in currentRangeZ step step) {
                        val vec3 = Vec3d(x, y, z)

                        possibleRotations.add(vec3)
                    }
                }
            }
            return possibleRotations
        }

        fun closeRotation(pos: BlockPos, eyes: Vec3d = player.eyes): Vec3d {
            val yawToCompare =
                if (RotationManager.targetRotation != null) RotationManager.targetRotation!!.yaw else player.yaw
            // Sort them by angleDifference between it and currentTarget.rotation
            return rotationList(xRange, yRange, zRange, step).minBy {
                abs(
                    RotationManager.angleDifference(
                        RotationManager.makeRotation(
                            it.add(
                                Vec3d.of(pos)
                            ), eyes
                        ).yaw,
                        yawToCompare
                    )
                )
            }
        }

        fun truncate(minY: Double): Face? {
            val newFace = Face(
                Vec3d(this.from.x, this.from.y.coerceAtLeast(minY), this.from.z),
                Vec3d(this.to.x, this.to.y.coerceAtLeast(minY), this.to.z)
            )

            if (newFace.from.y > newFace.to.y) {
                return null
            }

            return newFace
        }

    }

    data class Target(val blockPos: BlockPos, val direction: Direction, val minY: Double, val rotation: Rotation)
}
