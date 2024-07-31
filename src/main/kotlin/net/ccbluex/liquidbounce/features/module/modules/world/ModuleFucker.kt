/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.event.events.CancelBlockBreakingEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.inventory.HOTBAR_SLOTS
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.inventory.getArmorColor
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.BedBlock
import net.minecraft.block.BlockState
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import kotlin.jvm.optionals.getOrNull
import kotlin.math.max

/**
 * Fucker module
 *
 * Destroys/Uses selected blocks around you.
 */
object ModuleFucker : Module("Fucker", Category.WORLD, aliases = arrayOf("BedBreaker")) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        if (it > range) {
            range
        } else {
            it
        }
    }

    /**
     * Entrance requires the target block to have an entrance. It does not matter if we can see it or not.
     * If this condition is true, it will override the wall range to range
     * and act as if we were breaking normally.
     *
     * Useful for Hypixel and CubeCraft
     */
    private object FuckerEntrance : ToggleableConfigurable(this, "Entrance", false) {
        /**
         * Breaks the weakest block around target block and makes an entrance
         */
        val breakFree by boolean("BreakFree", true)
    }

    init {
        tree(FuckerEntrance)
    }

    private val surroundings by boolean("Surroundings", true)
    private val targets by blocks("Targets", findBlocksEndingWith("_BED", "DRAGON_EGG").toHashSet())
    private val delay by int("Delay", 0, 0..20, "ticks")
    private val action by enumChoice("Action", DestroyAction.DESTROY).apply { tagBy(this) }
    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val prioritizeOverKillAura by boolean("PrioritizeOverKillAura", false)

    private val isSelfBedMode = choices<IsSelfBedChoice>("SelfBed", IsSelfBedNoneChoice, arrayOf(
        IsSelfBedNoneChoice,
        IsSelfBedColorChoice,
        IsSelfBedSpawnLocationChoice
    ))

    // Rotation
    private val rotations = tree(RotationsConfigurable(this))

    private object FuckerHighlight : ToggleableConfigurable(this, "Highlight", true) {

        private val color by color("Color", Color4b(255, 0, 0, 50))
        private val outlineColor by color("OutlineColor", Color4b(255, 0, 0, 100))

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val (pos, _) = currentTarget ?: return@handler

            renderEnvironmentForWorld(matrixStack) {
                val blockState = pos.getState() ?: return@renderEnvironmentForWorld
                if (blockState.isAir) {
                    return@renderEnvironmentForWorld
                }

                val outlineShape = blockState.getOutlineShape(world, pos)
                val boundingBox = if (outlineShape.isEmpty) {
                    FULL_BOX
                } else {
                    outlineShape.boundingBox
                }

                withPositionRelativeToCamera(pos.toVec3d()) {
                    withColor(color) {
                        drawSolidBox(boundingBox)
                    }

                    if (outlineColor.a != 0) {
                        withColor(outlineColor) {
                            drawOutlinedBox(boundingBox)
                        }
                    }
                }
            }
        }
    }

    init {
        tree(FuckerHighlight)
    }

    private var currentTarget: DestroyerTarget? = null
    private var wasTarget: DestroyerTarget? = null

    override fun disable() {
        if (currentTarget != null) {
            interaction.cancelBlockBreaking()
        }

        this.currentTarget = null
        this.wasTarget = null
        super.disable()
    }

    @Suppress("unused")
    private val targetUpdater = handler<SimulatedTickEvent> {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@handler
        }

        wasTarget = currentTarget
        updateTarget()
    }

    @Suppress("unused")
    private val breaker = repeatable {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@repeatable
        }

        // Delay if the target changed - this also includes when introducing a new target from null.
        if (wasTarget != currentTarget) {
            if (currentTarget == null || delay > 0) {
                currentTarget = null
                interaction.cancelBlockBreaking()
            }

            waitTicks(delay)
        }

        // Check if blink is enabled - if so, we don't want to do anything.
        if (ModuleBlink.enabled) {
            return@repeatable
        }

        val destroyerTarget = currentTarget ?: return@repeatable
        val currentRotation = RotationManager.serverRotation

        // Check if we are already looking at the block
        val rayTraceResult = raytraceBlock(
            max(range, wallRange).toDouble(),
            currentRotation,
            destroyerTarget.pos,
            destroyerTarget.pos.getState() ?: return@repeatable
        ) ?: return@repeatable

        val raytracePos = rayTraceResult.blockPos

        // Check if the raytrace result includes a block, if not we don't want to deal with it.
        if (rayTraceResult.type != HitResult.Type.BLOCK ||
            raytracePos.getState()?.isAir == true || raytracePos != destroyerTarget.pos
        ) {
            return@repeatable
        }

        // Use action should be used if the block is the same as the current target and the action is set to use.
        if (destroyerTarget.action == DestroyAction.USE) {
            if (interaction.interactBlock(player, Hand.MAIN_HAND, rayTraceResult) == ActionResult.SUCCESS) {
                player.swingHand(Hand.MAIN_HAND)
            }

            waitTicks(delay)
        } else {
            doBreak(rayTraceResult, immediate = forceImmediateBreak)
        }
    }

    @Suppress("unused")
    private val cancelBlockBreakingHandler = handler<CancelBlockBreakingEvent> {
        if (currentTarget != null) {
            it.cancelEvent()
        }
    }

    private var spawnLocation: Vec3d? = null
    private val gameStartHandler = handler<PacketEvent> {
        if (it.packet is PlayerPositionLookS2CPacket) {
            val dist = player.pos.distanceTo(Vec3d(
                it.packet.x,
                it.packet.y,
                it.packet.z
            ))

            if(dist > 16.0) {
                spawnLocation = Vec3d(it.packet.x, it.packet.y, it.packet.z)
            }
        }
    }

    private fun updateTarget() {
        val eyesPos = player.eyes

        val possibleBlocks = searchBlocksInCuboid(range + 1, eyesPos) { pos, state ->
            targets.contains(state.block)
                && !((state.block as? BedBlock)?.let { block -> isSelfBedMode.activeChoice.isSelfBed(block, pos) } ?:
            false)
                && getNearestPoint(eyesPos, Box.enclosing(pos, pos.add(1, 1, 1))).distanceTo(eyesPos) <= range
        }

        validateCurrentTarget(possibleBlocks)

        // Find the nearest block
        val (pos, _) = possibleBlocks.minByOrNull { (pos, _) -> pos.getCenterDistanceSquared() } ?: return

        val range = range.toDouble()
        var wallRange = wallRange.toDouble()

        // If the block has an entrance, we should ignore the wall range and act as if we are breaking normally.
        if (FuckerEntrance.enabled && pos.hasEntrance) {
            wallRange = range
        }

        if (considerAsTarget(DestroyerTarget(pos, action, isTarget = true), range, wallRange) != true) {
            // Is there any block in the way?
            if (FuckerEntrance.enabled && FuckerEntrance.breakFree) {
                val weakBlock = pos.weakestBlock ?: return

                considerAsTarget(DestroyerTarget(weakBlock, DestroyAction.DESTROY), range, range)
            } else if (surroundings) {
                updateSurroundings(pos)
            }
        }
    }

    abstract class IsSelfBedChoice(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = isSelfBedMode
        abstract fun isSelfBed(block: BedBlock, pos: BlockPos): Boolean
    }

    object IsSelfBedNoneChoice : IsSelfBedChoice("None") {
        override fun isSelfBed(block: BedBlock, pos: BlockPos) = false
    }

    object IsSelfBedSpawnLocationChoice : IsSelfBedChoice("SpawnLocation") {

        private val bedDistance by float("BedDistance", 24.0f, 16.0f..48.0f)

        override fun isSelfBed(block: BedBlock, pos: BlockPos) =
            spawnLocation?.isInRange(pos.toVec3d(), bedDistance.toDouble()) ?: false
    }

    object IsSelfBedColorChoice : IsSelfBedChoice("Color") {
        override fun isSelfBed(block: BedBlock, pos: BlockPos): Boolean {
            val color = block.color
            val colorRgb = color.mapColor.color
            val (_, armorColor) = getArmorColor() ?: return false

            return armorColor == colorRgb
        }
    }

    private fun validateCurrentTarget(possibleBlocks: List<Pair<BlockPos, BlockState>>) {
        val currentTarget = this.currentTarget

        if (currentTarget != null) {
            if (possibleBlocks.any { (pos, _) -> pos == currentTarget.pos }) {
                this.currentTarget = null
            }
            if (currentTarget.isTarget && currentTarget.action != action) {
                this.currentTarget = null
            }

            // Stick with the current target because it's still valid.
            val validationResult =
                considerAsTarget(currentTarget, range.toDouble(), wallRange.toDouble(), isCurrentTarget = true)

            if (validationResult == false) {
                this.currentTarget = null
            }
        }
    }

    fun traceWayToTarget(
        target: BlockPos,
        eyePos: Vec3d,
        currBlock: BlockPos,
        visited: HashSet<BlockPos>,
        out: MutableList<Pair<BlockPos, Vec3d>>
    ) {
        val nextPos = arrayOf(
            currBlock.offset(Direction.NORTH),
            currBlock.offset(Direction.SOUTH),
            currBlock.offset(Direction.EAST),
            currBlock.offset(Direction.WEST),
            currBlock.offset(Direction.UP),
            currBlock.offset(Direction.DOWN),
        )

        for (pos in nextPos) {
            if (pos == target || pos in visited) {
                continue
            }

            val rc = Box(pos).raycast(eyePos, target.toCenterPos()).getOrNull() ?: continue

            out.add(pos to rc)
            visited.add(pos)

            traceWayToTarget(target, eyePos, pos, visited, out)
        }
    }

    private fun isBetterTarget(otherTarget: DestroyerTarget, currentTarget: DestroyerTarget): Boolean {
        val currentSurrounding = currentTarget.surroundingInfo
        val otherSurrounding = otherTarget.surroundingInfo

        return when {
            currentTarget.isTarget -> false
            otherTarget.isTarget -> true
            otherSurrounding == null -> true
            currentSurrounding == null -> false
            else -> currentSurrounding.resistance > otherSurrounding.resistance
        }
    }

    /**
     * @return true if it is the best target, false if it's invalid and null if it's not better than the current target
     */
    private fun considerAsTarget(
        target: DestroyerTarget,
        range: Double,
        throughWallsRange: Double,
        isCurrentTarget: Boolean = false
    ): Boolean? {
        val state = target.pos.getState()

        if (state == null || state.isAir) {
            return false
        }

        val raytrace = raytraceBlock(
            player.eyes,
            target.pos,
            target.pos.getState()!!,
            range = range,
            wallsRange = throughWallsRange
        ) ?: return false

        val currentTarget = this.currentTarget

        if (!isCurrentTarget && currentTarget != null && !isBetterTarget(target, currentTarget)) {
            return null
        }

        val (rotation, _) = raytrace
        RotationManager.aimAt(
            rotation,
            considerInventory = !ignoreOpenInventory,
            configurable = rotations,
            if (prioritizeOverKillAura) Priority.IMPORTANT_FOR_USAGE_3 else Priority.IMPORTANT_FOR_USAGE_1,
            this@ModuleFucker
        )

        this.currentTarget = target

        return true
    }

    private fun updateSurroundings(initialPosition: BlockPos) {
        val raytraceResult = world.raycast(
            RaycastContext(
                player.eyes,
                initialPosition.toCenterPos(),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ) ?: return

        if (raytraceResult.type != HitResult.Type.BLOCK) {
            return
        }

        val blockPos = raytraceResult.blockPos

        val arr = ArrayList<Pair<BlockPos, Vec3d>>()

        traceWayToTarget(initialPosition, player.eyes, blockPos, HashSet(), arr)

        val hotbarItems = HOTBAR_SLOTS.map { it.itemStack }

        val resistance = arr.mapNotNull { it.first.getState() }.filter { !it.isAir }
            .sumOf {
                val bestMiningSpeed = hotbarItems.maxOfOrNull { item -> item.getMiningSpeedMultiplier(it) } ?: 1.0F

                it.getHardness(world, BlockPos.ORIGIN).toDouble() / bestMiningSpeed.toDouble()
            }

        considerAsTarget(
            DestroyerTarget(blockPos, DestroyAction.DESTROY, SurroundingInfo(initialPosition, resistance)),
            range.toDouble(),
            wallRange.toDouble(),
        )
    }

    data class DestroyerTarget(
        val pos: BlockPos,
        val action: DestroyAction,
        val surroundingInfo: SurroundingInfo? = null,
        val isTarget: Boolean = false
    )

    /**
     * @param actualTargetPos the parent DestroyerTarget is surrounding this block
     * @param resistance proportional to the time it will take until the actual target is reached
     */
    data class SurroundingInfo(
        val actualTargetPos: BlockPos,
        val resistance: Double
    )

    enum class DestroyAction(override val choiceName: String) : NamedChoice {
        DESTROY("Destroy"), USE("Use")
    }

}
