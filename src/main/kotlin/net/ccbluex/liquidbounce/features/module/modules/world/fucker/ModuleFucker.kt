/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world.fucker

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.CancelBlockBreakingEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.world.packetmine.ModulePacketMine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findBlocksEndingWith
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.block.BedBlock
import net.minecraft.client.gui.screen.ingame.HandledScreen
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
object ModuleFucker : ClientModule("Fucker", Category.WORLD, aliases = arrayOf("BedBreaker", "IdNuker")) {

    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 0F..6F).onChange {
        minOf(range, it)
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
    private val action by enumChoice("Action", DestroyAction.DESTROY).apply(::tagBy)
    private val forceImmediateBreak by boolean("ForceImmediateBreak", false)

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
    private val ignoreUsingItem by boolean("IgnoreUsingItem", true)
    private val prioritizeOverKillAura by boolean("PrioritizeOverKillAura", false)

    private val isSelfBedMode = choices("SelfBed", 0, ::isSelfBedChoices)

    // Rotation
    private val rotations = tree(RotationsConfigurable(this))
    private val targetRenderer = tree(
        PlacementRenderer("TargetRendering", true, this,
            defaultColor = Color4b(255, 0, 0, 90)
        )
    )

    private var currentTarget: DestroyerTarget? = null
        set(value) {
            field?.let { targetRenderer.removeBlock(it.pos) }
            value?.let { targetRenderer.addBlock(it.pos) }

            field = value
        }
    private var wasTarget: DestroyerTarget? = null

    override fun onDisabled() {
        if (currentTarget != null) {
            interaction.cancelBlockBreaking()
        }

        currentTarget = null
        wasTarget = null
        targetRenderer.clearSilently()
    }

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@handler
        }

        if (!ignoreUsingItem && player.isUsingItem) {
            return@handler
        }

        wasTarget = currentTarget
        updateTarget()
    }

    @Suppress("unused")
    private val breaker = tickHandler {
        if (!ignoreOpenInventory && mc.currentScreen is HandledScreen<*>) {
            return@tickHandler
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
        if (ModuleBlink.running) {
            return@tickHandler
        }

        val destroyerTarget = currentTarget ?: return@tickHandler
        val currentRotation = RotationManager.serverRotation

        if (ModulePacketMine.running && destroyerTarget.action == DestroyAction.DESTROY) {
            ModulePacketMine.setTarget(destroyerTarget.pos)
            return@tickHandler
        }

        // Check if we are already looking at the block
        val rayTraceResult = raytraceBlock(
            max(range, wallRange).toDouble(),
            currentRotation,
            destroyerTarget.pos,
            destroyerTarget.pos.getState() ?: return@tickHandler
        ) ?: return@tickHandler

        val raytracePos = rayTraceResult.blockPos

        // Check if the raytrace result includes a block, if not we don't want to deal with it.
        if (rayTraceResult.type != HitResult.Type.BLOCK ||
            raytracePos != destroyerTarget.pos || raytracePos.getState()!!.isNotBreakable(raytracePos)
        ) {
            return@tickHandler
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
    private val cancelBlockBreakingHandler = handler<CancelBlockBreakingEvent> { event ->
        if (currentTarget != null && !ModulePacketMine.running) {
            event.cancelEvent()
        }
    }

    private fun updateTarget() {
        val eyesPos = player.eyePos

        val rangeSq = range.sq()

        val possibleBlocks = eyesPos.searchBlocksInCuboid(range + 1) { pos, state ->
            val block = state.block
            when {
                block !in targets -> false
                block is BedBlock && isSelfBedMode.activeChoice.isSelfBed(block, pos) -> false
                else -> getNearestPoint(eyesPos, Box(pos)).squaredDistanceTo(eyesPos) <= rangeSq
            }
        }.mapTo(hashSetOf()) { it.first }

        validateCurrentTarget(possibleBlocks)

        // Find the nearest block
        val pos = possibleBlocks.minByOrNull { pos -> pos.getCenterDistanceSquared() } ?: return

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

    private fun validateCurrentTarget(possibleBlocks: Set<BlockPos>) {
        val currentTarget = currentTarget

        if (currentTarget != null) {
            if (currentTarget.pos !in possibleBlocks) {
                ModuleFucker.currentTarget = null
            }
            if (currentTarget.isTarget && currentTarget.action != action) {
                ModuleFucker.currentTarget = null
            }

            // Stick with the current target because it's still valid.
            val validationResult =
                considerAsTarget(currentTarget, range.toDouble(), wallRange.toDouble(), isCurrentTarget = true)

            if (validationResult == false) {
                ModuleFucker.currentTarget = null
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
            player.eyePos,
            target.pos,
            target.pos.getState()!!,
            range = range,
            wallsRange = throughWallsRange
        ) ?: return false

        val currentTarget = currentTarget

        if (!isCurrentTarget && currentTarget != null && !isBetterTarget(target, currentTarget)) {
            return null
        }

        if (!ModulePacketMine.running) {
            val (rotation, _) = raytrace
            RotationManager.setRotationTarget(
                rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotations,
                if (prioritizeOverKillAura) Priority.IMPORTANT_FOR_USAGE_3 else Priority.IMPORTANT_FOR_USAGE_1,
                this@ModuleFucker
            )
        }

        ModuleFucker.currentTarget = target

        return true
    }

    private fun updateSurroundings(initialPosition: BlockPos) {
        val raytraceResult = world.raycast(
            RaycastContext(
                player.eyePos,
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

        traceWayToTarget(initialPosition, player.eyePos, blockPos, HashSet(), arr)

        val hotbarItems = Slots.Hotbar.map { it.itemStack }

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
