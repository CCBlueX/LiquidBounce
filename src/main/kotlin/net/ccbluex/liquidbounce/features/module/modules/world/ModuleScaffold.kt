/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.item.notABlock
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.block.SideShapeType
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

/**
 * Scaffold module
 *
 * Places blocks under you.
 */
object ModuleScaffold : Module("Scaffold", Category.WORLD) {

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

    private val silent by boolean("Silent", true)
    var delay by intRange("Delay", 3..5, 0..40)

    private val swing by boolean("Swing", true)
    private val eagle by boolean("Eagle", true)
    private val blocksToEagle by int("BlocksToEagle", 1, 1..10)
    private val edgeDistance by float("EagleEdgeDistance", 0f, 0f..0.5f)
    val down by boolean("Down", false)

    // Rotation
    private val rotationsConfigurable = tree(RotationsConfigurable())
    private val stabilizedRotation by boolean("StabilizedRotation", false)
    private val minDist by float("MinDist", 0.0f, 0.0f..0.25f)
    private val zitterModes = choices(
        "ZitterMode",
        Off, arrayOf(
            Off,
            Teleport,
            Smooth
        )
    )
    val timer by float("Timer", 1f, 0.01f..10f)
    private val speedModifier by float("SpeedModifier", 1f, 0f..3f)

    object Slow : ToggleableConfigurable(this, "Slow", false) {
        val slowSpeed by float("SlowSpeed", 0.6f, 0.1f..3f)
    }

    private val safeWalk by boolean("SafeWalk", true)
    private val sameY by boolean("SameY", false)
    private var currentTarget: Target? = null


    init {
        tree(Slow)
    }

    private var startY = 0
    private var placedBlocks = 0
    private val shouldGoDown: Boolean
        get() = this.down && mc.options.sneakKey.isPressed

    override fun enable() {
        startY = player.blockPos.y
        super.enable()
    }

    val rotationUpdateHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state != EventState.PRE) {
            return@handler
        }

        currentTarget = updateTarget(getTargetedPosition())

        val target = currentTarget

        if (target != null) {
            if (stabilizedRotation) {
                RotationManager.aimAt(
                    Rotation((target.rotation.yaw / 45f).roundToInt() * 45f, target.rotation.pitch),
                    ticks = 30,
                    configurable = rotationsConfigurable
                )
            } else
                RotationManager.aimAt(target.rotation, ticks = 30, configurable = rotationsConfigurable)
        }
    }

    val moveHandler = repeatable {
        if (Slow.enabled) {
            player.velocity.x *= Slow.slowSpeed
            player.velocity.z *= Slow.slowSpeed
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
            chat("blocked")
            return@repeatable
        }

        var hasBlockInHand = isValidBlock(player.inventory.getStack(player.inventory.selectedSlot), target)

        // Handle silent block selection
        if (silent && !hasBlockInHand) {
            val slot = (0..8).filter { isValidBlock(player.inventory.getStack(it), target) }.mapNotNull {
                val stack = player.inventory.getStack(it)

                if (stack.item is BlockItem) Pair(it, stack)
                else null
            }.maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR.compare(o1.second, o2.second) }?.first

            if (slot != null) {
                SilentHotbar.selectSlotSilently(this, slot, 20)

                hasBlockInHand = true
            }
        } else {
            SilentHotbar.resetSlot(this)
        }

        if (!hasBlockInHand) {
            return@repeatable
        }

        val result = interaction.interactBlock(
            player,
            Hand.MAIN_HAND,
            rayTraceResult
        )

        if (result.isAccepted) {
            placedBlocks = placedBlocks++ % blocksToEagle
            if (player.isOnGround) {
                player.velocity.x *= speedModifier
                player.velocity.z *= speedModifier
            }
            if (result.shouldSwingHand() && swing) {
                player.swingHand(Hand.MAIN_HAND)
            }

            chat("placed")
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
        // Gets player distance to the edge
        var dif = 0.5

        for (side in Direction.values().drop(2)) {
            val neighbor = player.blockPos.down().offset(side)

            if (neighbor.getState()!!.isReplaceable) {
                val calcDif = abs(
                    0.5 + (if (side.axis == Direction.Axis.Z) {
                        neighbor.z - player.pos.z
                    } else {
                        neighbor.x - player.pos.x
                    })
                ) - 0.5

                dif = min(dif, calcDif)
            }
        }
        if (shouldDisableSafeWalk()) {
            it.state.enforceEagle = false
        } else if (!player.abilities.flying && dif <= edgeDistance && eagle && blocksToEagle == 0) {
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
        SilentHotbar.resetSlot(this)
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
                    val delta = player.eyes.subtract(
                        Vec3d.of(posToInvestigate).add(0.5, 0.5, 0.5).add(Vec3d.of(direction.vector).multiply(0.5))
                    )

                    val angle = delta.dotProduct(Vec3d.of(direction.vector)) / delta.distanceTo(Vec3d.ZERO)

                    if (angle < 0) return@mapNotNull null

                    Triple(direction, posToInvestigate, angle)
                }.maxByOrNull { it.second }
            } else {
                val directionsToInvestigate = arrayOf(
                    Direction.UP,
                    Direction.NORTH,
                    Direction.EAST,
                    Direction.SOUTH,
                    Direction.WEST,
                    Direction.DOWN
                )

                directionsToInvestigate.mapNotNull { direction ->
                    val normalVector = direction.vector
                    val currPos = posToInvestigate.add(direction.opposite.vector)
                    val currState = currPos.getState() ?: return@mapNotNull null

                    if (currState.isAir || currState.isReplaceable) {
                        return@mapNotNull null
                    }

                    val delta = player.eyes.subtract(
                        Vec3d.of(currPos).add(0.5, 0.5, 0.5).add(Vec3d.of(normalVector).multiply(0.5))
                    )

                    val angle = delta.dotProduct(Vec3d.of(normalVector)) / delta.distanceTo(Vec3d.ZERO)

                    if (angle < 0) return@mapNotNull null
                    Triple(direction, currPos, angle)
                }.maxByOrNull { it.second }
            }

            if (first == null) {
                continue
            }
            val currPos = first.second
            val rotation = raycast(4.5, RotationManager.aimToBlock(player.eyes, currPos, 4.5, first.first)!!) ?: continue
            val rotationRaycasted = RotationManager.aimToBlock(player.eyes, rotation.blockPos, 4.5, rotation.side) ?: continue
            return Target(
                rotation.blockPos,
                rotation.side,
                rotation.blockPos.y.toDouble(),
                rotationRaycasted
            )
        }
        return null
    }

    val safeWalkHandler = handler<PlayerSafeWalkEvent> { event ->
        if (safeWalk)
            event.isSafeWalk = !shouldDisableSafeWalk()
    }

    private fun shouldDisableSafeWalk() = shouldGoDown && player.blockPos.add(0, -2, 0).canStandOn()

    data class Face(val from: Vec3d, val to: Vec3d) {

        val area: Double
            get() {
                val l = to.x - from.x
                val b = to.y - from.y
                val h = to.z - from.z

                return (l * b + b * h + l * h) * 2.0
            }

        val center: Vec3d
            get() = Vec3d(
                from.x + (to.x - from.x) * 0.5,
                from.y + (to.y - from.y) * 0.5,
                from.z + (to.z - from.z) * 0.5
            )

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
