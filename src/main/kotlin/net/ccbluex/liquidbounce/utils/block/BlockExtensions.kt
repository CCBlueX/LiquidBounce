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
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BlockBreakingProgressEvent
import net.ccbluex.liquidbounce.render.EMPTY_BOX
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.minecraft.block.*
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.*
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

fun Vec3i.toBlockPos() = BlockPos(this)

fun BlockPos.getState() = mc.world?.getBlockState(this)

fun BlockPos.getBlock() = getState()?.block

fun BlockPos.getCenterDistanceSquared() = player.squaredDistanceTo(this.x + 0.5, this.y + 0.5, this.z + 0.5)

fun BlockPos.getCenterDistanceSquaredEyes() = player.eyes.squaredDistanceTo(this.x + 0.5, this.y + 0.5, this.z + 0.5)

/**
 * Some blocks like slabs or stairs must be placed on upper side in order to be placed correctly.
 */
val Block.mustBePlacedOnUpperSide: Boolean
    get() {
        return this is SlabBlock || this is StairsBlock
    }

val BlockPos.hasEntrance: Boolean
    get() {
        val positionsAround = arrayOf(
            this.offset(Direction.NORTH),
            this.offset(Direction.SOUTH),
            this.offset(Direction.EAST),
            this.offset(Direction.WEST),
            this.offset(Direction.UP)
        )

        val block = this.getBlock()
        return positionsAround.any { it.getState()?.isAir == true && it.getBlock() != block }
    }

val BlockPos.weakestBlock: BlockPos?
    get() {
        val positionsAround = arrayOf(
            this.offset(Direction.NORTH),
            this.offset(Direction.SOUTH),
            this.offset(Direction.EAST),
            this.offset(Direction.WEST),
            this.offset(Direction.UP)
        )

        val block = this.getBlock()
        return positionsAround
            .filter { it.getBlock() != block && it.getState()?.isAir == false }
            .sortedBy { player.pos.squaredDistanceTo(it.toCenterPos()) }
            .minByOrNull { it.getBlock()?.hardness ?: 0f }
    }

/**
 * Search blocks around a position in a cuboid.
 */
@Suppress("NestedBlockDepth")
inline fun searchBlocksInCuboid(
    radius: Float,
    center: Vec3d,
    filter: (BlockPos, BlockState) -> Boolean
): List<Pair<BlockPos, BlockState>> {
    val blocks = mutableListOf<Pair<BlockPos, BlockState>>()

    val xRange = floor(center.x + radius).toInt() downTo floor(center.x - radius).toInt()
    val yRange = floor(center.y + radius).toInt() downTo floor(center.y - radius).toInt()
    val zRange = floor(center.z + radius).toInt() downTo floor(center.z - radius).toInt()

    for (x in xRange) {
        for (y in yRange) {
            for (z in zRange) {
                val blockPos = BlockPos(x, y, z)
                val state = blockPos.getState() ?: continue
                if (!filter(blockPos, state)) {
                    continue
                }

                blocks.add(Pair(blockPos, state))
            }
        }
    }

    return blocks
}

/**
 * Scan blocks around a position in a cuboid.
 */
@Suppress("NestedBlockDepth")
inline fun scanBlocksInCuboid(radius: Int, center: BlockPos, function: (pos: BlockPos) -> Boolean) {
    for (x in center.x - radius..center.x + radius) {
        for (y in center.y - radius..center.y + radius) {
            for (z in center.z - radius..center.z + radius) {
                val blockPos = BlockPos(x, y, z)
                if (function(blockPos)) {
                    return
                }
            }
        }
    }
}

@Suppress("NestedBlockDepth")
inline fun forEachBlockPosBetween(
    from: Vec3i,
    to: Vec3i,
    action: (Vec3i) -> Unit,
) {
    for (x in from.x..to.x) {
        for (y in from.y..to.y) {
            for (z in from.z..to.z) {
                action(Vec3i(x, y, z))
            }
        }
    }
}

/**
 * Search blocks around the player in a specific [radius]
 */
@Suppress("NestedBlockDepth")
inline fun searchBlocksInRadius(
    radius: Float,
    filter: (BlockPos, BlockState) -> Boolean,
): List<Pair<BlockPos, BlockState>> {
    val blocks = mutableListOf<Pair<BlockPos, BlockState>>()

    val thePlayer = mc.player ?: return blocks

    val playerPos = thePlayer.pos
    val radiusSquared = radius * radius
    val radiusInt = radius.toInt()

    for (x in radiusInt downTo -radiusInt) {
        for (y in radiusInt downTo -radiusInt) {
            for (z in radiusInt downTo -radiusInt) {
                val blockPos = BlockPos(thePlayer.x.toInt() + x, thePlayer.y.toInt() + y, thePlayer.z.toInt() + z)
                val state = blockPos.getState() ?: continue

                if (!filter(blockPos, state)) {
                    continue
                }
                if (Vec3d.of(blockPos).squaredDistanceTo(playerPos) > radiusSquared) {
                    continue
                }

                blocks.add(Pair(blockPos, state))
            }
        }
    }

    return blocks
}

fun BlockPos.canStandOn(): Boolean {
    return this.getState()!!.isSideSolid(world, this, Direction.UP, SideShapeType.CENTER)
}

/**
 * Check if [box] is reaching of specified blocks
 */
inline fun isBlockAtPosition(
    box: Box,
    isCorrectBlock: (Block?) -> Boolean,
): Boolean {
    for (x in MathHelper.floor(box.minX) until MathHelper.floor(box.maxX) + 1) {
        for (z in MathHelper.floor(box.minZ) until MathHelper.floor(box.maxZ) + 1) {
            val block = BlockPos.ofFloored(x.toDouble(), box.minY, z.toDouble()).getBlock()

            if (isCorrectBlock(block)) {
                return true
            }
        }
    }

    return false
}

/**
 * Check if [box] intersects with bounding box of specified blocks
 */
@Suppress("detekt:all")
inline fun collideBlockIntersects(
    box: Box,
    checkCollisionShape: Boolean = true,
    isCorrectBlock: (Block?) -> Boolean
): Boolean {
    for (x in MathHelper.floor(box.minX) .. MathHelper.floor(box.maxX)) {
        for (y in MathHelper.floor(box.minY)..MathHelper.floor(box.maxY)) {
            for (z in MathHelper.floor(box.minZ)..MathHelper.floor(box.maxZ)) {
                val blockPos = BlockPos.ofFloored(x.toDouble(), y.toDouble(), z.toDouble())
                val blockState = blockPos.getState() ?: continue
                val block = blockPos.getBlock() ?: continue

                if (!isCorrectBlock(block)) {
                    continue
                }
                if (!checkCollisionShape) {
                    return true
                }

                val shape = blockState.getCollisionShape(mc.world, blockPos)

                if (shape.isEmpty) {
                    continue
                }

                val boundingBox = shape.boundingBox

                if (box.intersects(boundingBox)) {
                    return true
                }
            }
        }
    }

    return false
}

inline fun Box.forEachCollidingBlock(function: (x: Int, y: Int, z: Int) -> Unit) {
    val from = BlockPos(this.minX.toInt(), this.minY.toInt(), this.minZ.toInt())
    val to = BlockPos(ceil(this.maxX).toInt(), ceil(this.maxY).toInt(), ceil(this.maxZ).toInt())

    for (x in from.x until to.x) {
        for (y in from.y until to.y) {
            for (z in from.z until to.z) {
                function(x, y, z)
            }
        }
    }
}

fun BlockState.canBeReplacedWith(
    pos: BlockPos,
    usedStack: ItemStack,
): Boolean {
    val placementContext =
        ItemPlacementContext(
            mc.player,
            Hand.MAIN_HAND,
            usedStack,
            BlockHitResult(Vec3d.of(pos), Direction.UP, pos, false),
        )

    return canReplace(
        placementContext,
    )
}

enum class PlacementSwingMode(
    override val choiceName: String,
    val hideClientSide: Boolean,
    val hideServerSide: Boolean
): NamedChoice {
    DO_NOT_HIDE("DoNotHide", false, false),
    HIDE_BOTH("HideForBoth", true, true),
    HIDE_CLIENT("HideForClient", true, false),
    HIDE_SERVER("HideForServer", false, true),
}

fun doPlacement(
    rayTraceResult: BlockHitResult,
    hand: Hand = Hand.MAIN_HAND,
    onPlacementSuccess: () -> Boolean = { true },
    onItemUseSuccess: () -> Boolean = { true },
    placementSwingMode: PlacementSwingMode = PlacementSwingMode.DO_NOT_HIDE
) {
    val stack = player.mainHandStack
    val count = stack.count

    val interactionResult = interaction.interactBlock(player, hand, rayTraceResult)

    when {
        interactionResult == ActionResult.FAIL -> {
            return
        }

        interactionResult == ActionResult.PASS -> {
            // Ok, we cannot place on the block, so let's just use the item in the direction
            // without targeting a block (for buckets, etc.)
            handlePass(hand, stack, onItemUseSuccess, placementSwingMode)
            return
        }

        interactionResult.isAccepted -> {
            val wasStackUsed = !stack.isEmpty && (stack.count != count || interaction.hasCreativeInventory())

            handleActionsOnAccept(hand, interactionResult, wasStackUsed, onPlacementSuccess, placementSwingMode)
        }
    }
}

/**
 * Swings item, resets equip progress and hand swing progress
 *
 * @param wasStackUsed was an item consumed in order to place the block
 */
private fun handleActionsOnAccept(
    hand: Hand,
    interactionResult: ActionResult,
    wasStackUsed: Boolean,
    onPlacementSuccess: () -> Boolean,
    placementSwingMode: PlacementSwingMode = PlacementSwingMode.DO_NOT_HIDE,
) {
    if (!interactionResult.shouldSwingHand()) {
        return
    }

    if (onPlacementSuccess()) {
        when (placementSwingMode) {
            PlacementSwingMode.DO_NOT_HIDE -> {
                player.swingHand(hand)
            }
            PlacementSwingMode.HIDE_BOTH -> { }
            PlacementSwingMode.HIDE_CLIENT -> {
                network.sendPacket(HandSwingC2SPacket(hand))
            }
            PlacementSwingMode.HIDE_SERVER -> {
                player.swingHand(hand, false)
            }
        }
    }

    if (wasStackUsed) {
        mc.gameRenderer.firstPersonRenderer.resetEquipProgress(hand)
    }

    return
}

/**
 * Just interacts with the item in the hand instead of using it on the block
 */
private fun handlePass(
    hand: Hand,
    stack: ItemStack,
    onItemUseSuccess: () -> Boolean,
    placementSwingMode: PlacementSwingMode
) {
    if (stack.isEmpty) {
        return
    }

    val actionResult = interaction.interactItem(player, hand)

    handleActionsOnAccept(hand, actionResult, true, onItemUseSuccess, placementSwingMode)
}

/**
 * Breaks the block
 */
fun doBreak(rayTraceResult: BlockHitResult, immediate: Boolean = false) {
    val direction = rayTraceResult.side
    val blockPos = rayTraceResult.blockPos

    if (immediate) {
        EventManager.callEvent(BlockBreakingProgressEvent(blockPos))

        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, direction
            )
        )
        player.swingHand(Hand.MAIN_HAND)
        network.sendPacket(
            PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction
            )
        )
        return
    }

    if (player.isCreative) {
        if (interaction.attackBlock(blockPos, rayTraceResult.side)) {
            player.swingHand(Hand.MAIN_HAND)
            return
        }
    }

    if (interaction.updateBlockBreakingProgress(blockPos, direction)) {
        player.swingHand(Hand.MAIN_HAND)
        mc.particleManager.addBlockBreakingParticles(blockPos, direction)
    }
}

fun BlockPos.manhattanDistanceTo(other: BlockPos): Int {
    return abs(x - other.x) + abs(y - other.y) + abs(z - other.z)
}

// TODO replace this by an approach that automatically collects the blocks, this would create better mod compatibility
/**
 * Checks if the block can be interacted with, null will be returned as not interactable.
 * The [blockState] is optional but can make the result more accurate, if not provided
 * it will just assume the block is interactable.
 *
 * Note: The player is required to NOT be `null`.
 *
 * This data has been collected by looking at the implementations of [AbstractBlock.onUse].
 */
fun Block?.isInteractable(blockState: BlockState?): Boolean {
    if (this == null) {
        return false
    }

    return this is BedBlock || this is AbstractChestBlock<*> || this is AbstractFurnaceBlock || this is AnvilBlock
        || this is BarrelBlock || this is BeaconBlock || this is BellBlock || this is BrewingStandBlock
        || this is ButtonBlock || this is CakeBlock && player.hungerManager.isNotFull || this is CandleCakeBlock
        || this is CartographyTableBlock || this is CaveVinesBodyBlock && blockState?.get(CaveVines.BERRIES) ?: true
        || this is CaveVinesHeadBlock && blockState?.get(CaveVines.BERRIES) ?: true
        || this is ComparatorBlock || this is ComposterBlock && (blockState?.get(ComposterBlock.LEVEL) ?: 8) == 8
        || this is CrafterBlock || this is CraftingTableBlock || this is DaylightDetectorBlock
        || this is DecoratedPotBlock || this is DispenserBlock || this is DoorBlock || this is DragonEggBlock
        || this is EnchantingTableBlock || this is FenceGateBlock || this is FlowerPotBlock
        || this is GrindstoneBlock || this is HopperBlock || this is OperatorBlock && player.isCreativeLevelTwoOp
        || this is JukeboxBlock && blockState?.get(JukeboxBlock.HAS_RECORD) == true || this is LecternBlock
        || this is LeverBlock || this is LightBlock && player.isCreativeLevelTwoOp || this is NoteBlock
        || this is RedstoneWireBlock || this is RepeaterBlock || this is RespawnAnchorBlock // this only works
        // when we hold glow stone or are not in the nether and the anchor is charged, but it'd be too error-prone when
        // it would be checked as the player can quickly switch to glow stone
        || this is ShulkerBoxBlock || this is StonecutterBlock
        || this is SweetBerryBushBlock && (blockState?.get(SweetBerryBushBlock.AGE) ?: 2) > 1 || this is TrapdoorBlock
}

/**
 * Returns the shape of the block as box, if it can't get the actual shape, it will return [FULL_BOX].
 */
fun BlockPos.getShape(): Box {
    val outlineShape = this.getState()?.getOutlineShape(world, this) ?: return FULL_BOX
    if (outlineShape.isEmpty) {
        return EMPTY_BOX
    }

    return outlineShape.boundingBox
}

fun BlockPos.isBlockedByEntities(): Boolean {
    return world.entities.any {
        it.boundingBox.intersects(FULL_BOX.offset(this.x.toDouble(), this.y.toDouble(), this.z.toDouble()))
    }
}
