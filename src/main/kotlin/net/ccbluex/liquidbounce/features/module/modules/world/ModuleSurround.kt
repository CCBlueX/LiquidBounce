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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandCenter
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandCenter.CenterHandlerState
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.placer.CrystalDestroyFeature
import net.ccbluex.liquidbounce.utils.block.placer.NoRotationMode
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.getSlot
import net.ccbluex.liquidbounce.utils.entity.getFeetBlockPos
import net.ccbluex.liquidbounce.utils.entity.isInHole
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import org.joml.Vector2d
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Surround module
 *
 * Builds safe holes.
 *
 * @author ccetl
 */
object ModuleSurround : ClientModule("Surround", Category.WORLD, disableOnQuit = true) {

    /**
     * The blocks the surround normal utilizes.
     */
    private val DEFAULT_BLOCKS = hashSetOf(Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.CRYING_OBSIDIAN)

    /**
     * Runs [CommandCenter] when the module is enabled.
     */
    private val center by boolean("Center", false)

    /**
     * Extends when entities block placement spots.
     */
    private val extend by boolean("Extend", true)

    /**
     * When enabled, the surround won't build 2x1 or 2x2 holes if we already are in a completed 1x1 hole, even if
     * we block replacements.
     *
     * This should only be enabled if no wall placements are possible, or we have a significantly lower ping
     * than our opponent.
     */
    private val noWaste by boolean("NoWaste", false)

    /**
     * Places blocks below the surround so that enemies can't mine the block bellow you making you fall down.
     */
    private val down by boolean("Down", true)

    /**
     * Disables the module when the y-coordinate changes.
     */
    private val disableOnYChange by boolean("DisableOnYChange", true)

    /**
     * Disables the module when the player has moved at least 0.5 blocks away from the original center.
     */
    private val disableOnXZMove by boolean("DisableOnXZMove", false)

    /**
     * Disables the module when the player has a speed that is faster than or equal to 5 m/s.
     */
    private val disableOnXZSpeed by boolean("DisableOnXZSpeed", false)

    /**
     * Replaces broken blocks instantly.
     *
     * Note: requires the rotation mode "None" in the block placer
     */
    private val instant by boolean("Instant", true)

    /**
     * Protects the surround against being blocked by crystals on destruction.
     *
     * Destroying requires the crystal destroyer in the placer to be active.
     */
    private object Protect : ToggleableConfigurable(this, "Protect", true) {

        /**
         * At what destroy stage, actions should be taken.
         */
        private val minDestroyProgress by int("MinDestroyProgress", 4, 0..9, "stage")

        /**
         * Builds an extra layer around the surround blocks (slice):
         *     p
         *   x p x
         *     x
         * will become:
         *   x p x
         * x x p x x
         *     x
         *
         * X = obsidian
         * p = the players hitbox
         */
        @Suppress("SpellCheckingInspection", "GrazieInspection")
        object ExtraLayer : ToggleableConfigurable(this, "ExtraLayer", true) {

            /**
             * Will place even more blocks (top view):
             *   x
             * x p x
             *   x
             * will become:
             * x x x
             * x p x
             * x x x
             *
             * X = obsidian
             * p = the players hitbox
             */
            val corners by boolean("Corners", false)

        }

        init {
            tree(ExtraLayer)
        }

        val broken = mutableSetOf<BlockPos>()

        /**
         * With a higher priority so that it runs before [CrystalDestroyFeature].
         */
        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent>(priority = 10) {
            // check if this feature isn't enabled and the extra layer forcefully applied or not enabled ->
            // checks are not needed
            if (!placer.crystalDestroyer.enabled && (addExtraLayerBlocks || !ExtraLayer.enabled)) {
                return@handler
            }

            // clear the map of previously considered blocks
            broken.clear()

            // iterate all surround blocks and check if they're being broken
            placer.blocks.filter {
                !it.value  // exclude support blocks
            }.keys.forEach { pos ->
                // find the list of current breaking data, or else return
                val breakingProgressions = mc.worldRenderer.blockBreakingProgressions[pos.asLong()] ?: return@forEach

                // find the braking info that doesn't belong to us, if we mine our own surround, it should be ignored
                val breakingInfo = breakingProgressions.lastOrNull { it.actorId != player.id } ?: return@forEach
                val stage = breakingInfo.stage

                // check if the stage is too low, if so return
                if (stage < minDestroyProgress) {
                    return@forEach
                }

                // add the block to the map of blocks that are being broken
                if (ExtraLayer.enabled && stage > 0) {
                    broken.add(pos)
                }

                // skip to the next entry if the crystal destroy feature is disabled
                if (!placer.crystalDestroyer.enabled) {
                    return@forEach
                }

                // destroy crystals that would block replacements
                val blockedResult = pos.isBlockedByEntitiesReturnCrystal()
                val crystal = blockedResult.value() ?: return@forEach

                // try to replace the current target
                placer.crystalDestroyer.currentTarget = crystal

                // we could target the blocking crystal, now we have to wait a tick before it has been destroyed
                // anyways, so we can return here
                if (placer.crystalDestroyer.currentTarget == crystal) {
                    return@handler
                }
            }
        }

    }

    /**
     * Manually triggers the protection mechanism [Protect.ExtraLayer].
     */
    private val addExtraLayer by bind("AddExtraLayer")

    init {
        tree(Protect)
    }

    private val filter by enumChoice("Filter", Filter.WHITELIST)
    private val blocks by blocks("Blocks", DEFAULT_BLOCKS)
    private val placer = tree(BlockPlacer(
        "Placing",
        this,
        Priority.IMPORTANT_FOR_PLAYER_LIFE,
        { filter.getSlot(blocks) }
    ))

    private var addExtraLayerBlocks = false
    private var startY = 0.0
    private var centerPos: Vector2d? = null

    init {
        // for this module, support should by default be able to use obsidian
        placer.support.blocks.addAll(DEFAULT_BLOCKS)
    }

    override fun enable() {
        if (center) {
            CommandCenter.state = CenterHandlerState.APPLY_ON_NEXT_EVENT
        }

        startY = player.pos.y
        val centerBlockPos = player.blockPos.toCenterPos()
        centerPos = Vector2d(centerBlockPos.x, centerBlockPos.z)
    }

    override fun disable() {
        placer.disable()
        addExtraLayerBlocks = false
    }

    @Suppress("unused")
    val keyHandler = handler<KeyboardKeyEvent> {
        addExtraLayerBlocks = addExtraLayer.getNewState(it, addExtraLayerBlocks)
    }

    @Suppress("unused")
    private val tickMoveHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state == EventState.PRE) {
            return@handler
        }

        val yChange = disableOnYChange && it.y != startY
        val dx = abs(player.x - (centerPos?.x ?: 0.0))
        val dz = abs(player.z - (centerPos?.y ?: 0.0))
        val xzChange = disableOnXZMove && (dx > 0.5 || dz > 0.5)
        val speed = player.pos.subtract(player.prevX, player.prevY, player.prevZ).lengthSquared() * 20.0
        val highSpeed = disableOnXZSpeed && speed >= 5.0
        if (yChange || xzChange || highSpeed) {
            enabled = false
        }
    }

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (disableOnYChange && player.pos.y != startY) {
            enabled = false
            return@handler
        }

        val bb = player.boundingBox
        val y = ceil(bb.minY)

        val feetBlockPos = player.getFeetBlockPos()
        val hole = if (noWaste && player.isInHole(feetBlockPos)) {
            setOf(feetBlockPos)
        } else {
            val maxX = getMax(bb, Direction.Axis.X)
            val maxZ = getMax(bb, Direction.Axis.Z)
            setOf(
                BlockPos.ofFloored(bb.minX, y, bb.minZ),
                BlockPos.ofFloored(bb.minX, y, maxZ),
                BlockPos.ofFloored(maxX, y, bb.minZ),
                BlockPos.ofFloored(maxX, y, maxZ),
            )
        }

        val holeBlocks = hashSetOf<BlockPos>()
        val blocked = hashSetOf<BlockPos>()
        blocked.addAll(hole)

        for (holePos in hole) {
            DIRECTIONS_EXCLUDING_UP.forEach { direction ->
                val pos = holePos.offset(direction)
                if (pos in hole || !holeBlocks.add(pos)) {
                    return@forEach
                }

                val isDown = direction == Direction.DOWN
                if (isDown && down) {
                    holeBlocks.add(holePos.offset(direction, 2))
                }

                if (!isDown && (addExtraLayerBlocks || Protect.broken.contains(pos))) {
                    holeBlocks.add(pos.offset(direction))
                    holeBlocks.add(pos.up())
                    if (Protect.ExtraLayer.corners) {
                        holeBlocks.add(pos.offset(direction.rotateYClockwise()))
                    }
                }

                if (!isDown && extend) {
                    pos.getBlockingEntities { it !is EndCrystalEntity && it != player }.forEach {
                        getEntitySurround(it, holeBlocks, blocked, y)
                    }
                }
            }
        }

        placer.update(holeBlocks)
    }

    @Suppress("unused")
    private val blockUpdateHandler = handler<PacketEvent> {
        if (!instant) {
            return@handler
        }

        when (val packet = it.packet) {
            is BlockUpdateS2CPacket -> placeInstant(packet.pos, packet.state)
            is ChunkDeltaUpdateS2CPacket -> {
                packet.visitUpdates { pos, state -> placeInstant(null, state, pos as BlockPos.Mutable) }
            }
        }
    }

    private fun placeInstant(blockPos: BlockPos?, state: BlockState, blockPos1: BlockPos.Mutable? = null) {
        val pos = blockPos ?: blockPos1!!
        val irrelevantPacket = !state.isReplaceable || pos !in placer.blocks

        val rotationMode = placer.rotationMode.activeChoice
        if (irrelevantPacket || rotationMode !is NoRotationMode || pos.isBlockedByEntities()) {
            return
        }

        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                listOf(Vec3i.ZERO),
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            ),
            FaceHandlingOptions(CenterTargetPositionFactory, considerFacingAwayFaces = placer.wallRange > 0),
            stackToPlaceWith = ItemStack(Items.SANDSTONE),
            PlayerLocationOnPlacement(position = player.pos, pose = player.pose),
        )

        val placementTarget = findBestBlockPlacementTarget(pos, searchOptions) ?: return

        // Check if we can reach the target
        if (!placer.canReach(placementTarget.interactedBlockPos, placementTarget.rotation)) {
            return
        }

        if (placementTarget.interactedBlockPos.getBlock().isInteractable(
                placementTarget.interactedBlockPos.getState()
            )
        ) {
            return
        }

        if (rotationMode.send) {
            val rotation = placementTarget.rotation.normalize()
            network.sendPacket(
                PlayerMoveC2SPacket.LookAndOnGround(rotation.yaw, rotation.pitch, player.isOnGround,
                    player.horizontalCollision)
            )
        }

        placer.doPlacement(false, blockPos ?: blockPos1!!.toImmutable(), placementTarget)
    }

    private fun getEntitySurround(entity: Entity, list: HashSet<BlockPos>, blocked: HashSet<BlockPos>, y: Double) {
        val bb = entity.boundingBox

        val maxX = getMax(bb, Direction.Axis.X)
        val maxZ = getMax(bb, Direction.Axis.Z)
        val hole = setOf(
            BlockPos.ofFloored(bb.minX, y, bb.minZ),
            BlockPos.ofFloored(bb.minX, y, maxZ),
            BlockPos.ofFloored(maxX, y, bb.minZ),
            BlockPos.ofFloored(maxX, y, maxZ),
        )

        blocked.addAll(hole)

        hole.forEach {
            Direction.HORIZONTAL.forEach { direction ->
                val pos = it.offset(direction)

                if (it !in blocked) {
                    list += pos
                }
            }
        }
    }

    private fun getMax(boundingBox: Box, axis: Direction.Axis): Double {
        val max = boundingBox.getMax(axis)
        val min = boundingBox.getMin(axis)

        return if (max == floor(min) + 1.0) {
            min
        } else {
            max
        }
    }

}
