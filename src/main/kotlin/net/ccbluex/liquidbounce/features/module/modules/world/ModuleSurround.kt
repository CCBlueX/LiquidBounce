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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.fastutil.fastIterator
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandCenter
import net.ccbluex.liquidbounce.features.command.commands.ingame.CommandCenter.CenterHandlerState
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_EXCLUDING_UP
import net.ccbluex.liquidbounce.utils.block.getBlockingEntities
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntitiesReturnCrystal
import net.ccbluex.liquidbounce.utils.block.placer.BlockPlacer
import net.ccbluex.liquidbounce.utils.block.placer.CrystalDestroyFeature
import net.ccbluex.liquidbounce.utils.block.placer.placeInstantOnBlockUpdate
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.collection.getSlot
import net.ccbluex.liquidbounce.utils.entity.getFeetBlockPos
import net.ccbluex.liquidbounce.utils.entity.isInHole
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
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
object ModuleSurround : ClientModule("Surround", ModuleCategories.WORLD, disableOnQuit = true) {

    /**
     * The blocks the surround normal utilizes.
     */
    private val DEFAULT_BLOCKS = arrayOf(Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.CRYING_OBSIDIAN)

    private val features by multiEnumChoice("Features",
        Features.EXTEND,
        Features.DOWN,
    )

    /**
     * Disables the module when the y-coordinate changes.
     * Or when the player has moved at least 0.5 blocks away from the original center.
     * Or when the player has a speed that is faster than or equal to 5 m/s.
     */
    private val disableOn by multiEnumChoice("DisableOn", DisableOn.Y_CHANGE)

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
    private object Protect : ToggleableValueGroup(this, "Protect", true) {

        /**
         * At what destroy stage, actions should be taken.
         */
        @Suppress("MagicNumber")
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
        object ExtraLayer : ToggleableValueGroup(this, "ExtraLayer", true) {

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
        @Suppress("unused", "LoopWithTooManyJumpStatements")
        private val tickHandler = handler<GameTickEvent>(priority = 10) {
            // check if this feature isn't enabled and the extra layer forcefully applied or not enabled ->
            // checks are not needed
            if (!placer.crystalDestroyer.enabled && (addExtraLayerBlocks || !ExtraLayer.enabled)) {
                return@handler
            }

            // clear the map of previously considered blocks
            broken.clear()

            // iterate all surround blocks and check if they're being broken
            for (entry in placer.blocks.fastIterator()) {
                if (entry.booleanValue) continue  // exclude support blocks
                val posAsLong = entry.longKey

                // find the list of current breaking data, or else return
                val breakingProgressions = mc.levelRenderer.destructionProgress[posAsLong] ?: continue

                // find the braking info that doesn't belong to us, if we mine our own surround, it should be ignored
                val breakingInfo = breakingProgressions.lastOrNull { it.id != player.id } ?: continue
                val stage = breakingInfo.progress

                // check if the stage is too low, if so return
                if (stage < minDestroyProgress) {
                    continue
                }

                val pos = BlockPos.of(posAsLong)
                // add the block to the map of blocks that are being broken
                if (ExtraLayer.enabled && stage > 0) {
                    broken.add(pos)
                }

                // skip to the next entry if the crystal destroy feature is disabled
                if (!placer.crystalDestroyer.enabled) {
                    continue
                }

                // destroy crystals that would block replacements
                val blockedResult = pos.isBlockedByEntitiesReturnCrystal()
                val crystal = blockedResult.value() ?: continue

                // try to replace the current target
                placer.crystalDestroyer.currentTarget = crystal

                // we could target the blocking crystal, now we have to wait a tick before it has been destroyed
                // anyway, so we can return here
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
    private val blocks by blocks("Blocks", blockSortedSetOf(blocks = DEFAULT_BLOCKS))
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

    override fun onEnabled() {
        if (Features.CENTER in features) {
            CommandCenter.state = CenterHandlerState.APPLY_ON_NEXT_EVENT
        }

        startY = player.position().y
        val centerBlockPos = player.blockPosition().center
        centerPos = Vector2d(centerBlockPos.x, centerBlockPos.z)
    }

    override fun onDisabled() {
        placer.disable()
        addExtraLayerBlocks = false
    }

    @Suppress("unused")
    val keyHandler = handler<KeyboardKeyEvent> {
        addExtraLayerBlocks = addExtraLayer.getNewState(it, addExtraLayerBlocks)
    }

    @Suppress("unused", "MagicNumber")
    private val tickMoveHandler = handler<PlayerNetworkMovementTickEvent> {
        if (it.state == EventState.PRE) {
            return@handler
        }

        val yChange = DisableOn.Y_CHANGE in disableOn && it.y != startY
        val dx = abs(player.x - (centerPos?.x ?: 0.0))
        val dz = abs(player.z - (centerPos?.y ?: 0.0))
        val xzChange = DisableOn.XZ_MOVE in disableOn && (dx > 0.5 || dz > 0.5)
        val speed = player.position().subtract(player.xo, player.yo, player.zo).lengthSqr() * 20.0
        val highSpeed = DisableOn.XZ_SPEED in disableOn && speed >= 5.0
        if (yChange || xzChange || highSpeed) {
            enabled = false
        }
    }

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent> {
        if (DisableOn.Y_CHANGE in disableOn && player.position().y != startY) {
            enabled = false
            return@handler
        }

        val bb = player.boundingBox
        val y = ceil(bb.minY)

        val feetBlockPos = player.getFeetBlockPos()
        val hole = if (Features.NO_WASTE in features && player.isInHole(feetBlockPos)) {
            setOf(feetBlockPos)
        } else {
            val maxX = getMax(bb, Direction.Axis.X)
            val maxZ = getMax(bb, Direction.Axis.Z)
            setOf(
                BlockPos.containing(bb.minX, y, bb.minZ),
                BlockPos.containing(bb.minX, y, maxZ),
                BlockPos.containing(maxX, y, bb.minZ),
                BlockPos.containing(maxX, y, maxZ),
            )
        }

        val holeBlocks = hashSetOf<BlockPos>()
        val blocked = hashSetOf<BlockPos>()
        blocked.addAll(hole)

        for (holePos in hole) {
            DIRECTIONS_EXCLUDING_UP.forEach { direction ->
                val pos = holePos.relative(direction)
                if (pos in hole || !holeBlocks.add(pos)) {
                    return@forEach
                }

                val isDown = direction == Direction.DOWN
                if (isDown && Features.DOWN in features) {
                    holeBlocks.add(holePos.relative(direction, 2))
                }

                if (!isDown && (addExtraLayerBlocks || Protect.broken.contains(pos))) {
                    holeBlocks.add(pos.relative(direction))
                    holeBlocks.add(pos.above())
                    if (Protect.ExtraLayer.corners) {
                        holeBlocks.add(pos.relative(direction.clockWise))
                    }
                }

                if (!isDown && Features.EXTEND in features) {
                    pos.getBlockingEntities { it !is EndCrystal && it != player }.forEach {
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

        placer.placeInstantOnBlockUpdate(it)
    }

    private fun getEntitySurround(
        entity: Entity,
        list: HashSet<BlockPos>,
        blocked: HashSet<BlockPos>,
        y: Double,
        down: Boolean = false
    ) {
        val bb = entity.boundingBox

        val maxX = getMax(bb, Direction.Axis.X)
        val maxZ = getMax(bb, Direction.Axis.Z)
        val hole = listOf(
            BlockPos.containing(bb.minX, y, bb.minZ),
            BlockPos.containing(bb.minX, y, maxZ),
            BlockPos.containing(maxX, y, bb.minZ),
            BlockPos.containing(maxX, y, maxZ),
        )

        blocked.addAll(hole)

        val directions = if (down) DIRECTIONS_EXCLUDING_UP else Direction.BY_2D_DATA
        hole.forEach {
            for (direction in directions) {
                val pos = it.relative(direction)

                if (it !in blocked) {
                    list += pos
                }
            }
        }
    }

    private fun getMax(boundingBox: AABB, axis: Direction.Axis): Double {
        val max = boundingBox.max(axis)
        val min = boundingBox.min(axis)

        return if (max == floor(min) + 1.0) {
            min
        } else {
            max
        }
    }

    private enum class DisableOn(
        override val tag: String
    ) : Tagged {
        Y_CHANGE("YChange"),
        XZ_MOVE("XZMove"),
        XZ_SPEED("XZSpeed");
    }

    private enum class Features(
        override val tag: String
    ) : Tagged {
        /**
         * Runs [CommandCenter] when the module is enabled.
         */
        CENTER("Center"),

        /**
         * Extends when entities block placement spots.
         */
        EXTEND("Extend"),

        /**
         * When enabled, the surround won't build 2x1 or 2x2 holes if we already are in a completed 1x1 hole, even if
         * we block replacements.
         *
         * This should only be enabled if no wall placements are possible, or we have a significantly lower ping
         * than our opponent.
         */
        NO_WASTE("NoWaste"),

        /**
         * Places blocks below the surround so that enemies can't mine the block bellow you making you fall down.
         */
        DOWN("Down");
    }
}
