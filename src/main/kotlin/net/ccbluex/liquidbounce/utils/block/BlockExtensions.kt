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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.utils.block

import it.unimi.dsi.fastutil.booleans.BooleanObjectPair
import it.unimi.dsi.fastutil.ints.IntLongPair
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BlockBreakingProgressEvent
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.network
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.math.expendToBlockBox
import net.ccbluex.liquidbounce.utils.math.iterator
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Position
import net.minecraft.core.Vec3i
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.AbstractChestBlock
import net.minecraft.world.level.block.AbstractFurnaceBlock
import net.minecraft.world.level.block.AnvilBlock
import net.minecraft.world.level.block.BarrelBlock
import net.minecraft.world.level.block.BeaconBlock
import net.minecraft.world.level.block.BedBlock
import net.minecraft.world.level.block.BellBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BrewingStandBlock
import net.minecraft.world.level.block.ButtonBlock
import net.minecraft.world.level.block.CakeBlock
import net.minecraft.world.level.block.CandleCakeBlock
import net.minecraft.world.level.block.CartographyTableBlock
import net.minecraft.world.level.block.CaveVines
import net.minecraft.world.level.block.CaveVinesBlock
import net.minecraft.world.level.block.CaveVinesPlantBlock
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.ComparatorBlock
import net.minecraft.world.level.block.ComposterBlock
import net.minecraft.world.level.block.CrafterBlock
import net.minecraft.world.level.block.CraftingTableBlock
import net.minecraft.world.level.block.DaylightDetectorBlock
import net.minecraft.world.level.block.DecoratedPotBlock
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.DoubleBlockCombiner
import net.minecraft.world.level.block.DragonEggBlock
import net.minecraft.world.level.block.EnchantingTableBlock
import net.minecraft.world.level.block.FenceGateBlock
import net.minecraft.world.level.block.FlowerPotBlock
import net.minecraft.world.level.block.GameMasterBlock
import net.minecraft.world.level.block.GrindstoneBlock
import net.minecraft.world.level.block.HopperBlock
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.JukeboxBlock
import net.minecraft.world.level.block.LecternBlock
import net.minecraft.world.level.block.LeverBlock
import net.minecraft.world.level.block.LightBlock
import net.minecraft.world.level.block.NoteBlock
import net.minecraft.world.level.block.RedStoneWireBlock
import net.minecraft.world.level.block.RepeaterBlock
import net.minecraft.world.level.block.RespawnAnchorBlock
import net.minecraft.world.level.block.ShulkerBoxBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.StonecutterBlock
import net.minecraft.world.level.block.SupportType
import net.minecraft.world.level.block.SweetBerryBushBlock
import net.minecraft.world.level.block.TrapDoorBlock
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.function.Consumer
import kotlin.math.ceil
import kotlin.math.floor

fun Vec3i.toBlockPos() = BlockPos(this)

fun BlockPos.getState() = mc.level?.getBlockState(this)

fun BlockPos.getBlock() = getState()?.block

fun BlockPos.getCenterDistanceSquared() = player.distanceToSqr(this.x + 0.5, this.y + 0.5, this.z + 0.5)

fun BlockPos.getCenterDistanceSquaredEyes() = player.eyePosition.distanceToSqr(this.x + 0.5, this.y + 0.5, this.z + 0.5)

val BlockState.isBed: Boolean
    get() = block is BedBlock

/**
 * Converts this [BlockPos] to an immutable one if needed.
 */
val BlockPos.immutable: BlockPos get() = if (this is BlockPos.MutableBlockPos) this.immutable() else this

/**
 * Returns the block box outline of the block at the position. If the block is air, it will return an empty box.
 * Outline Box should be used for rendering purposes only.
 *
 * Returns [FULL_BOX] when block is air or does not exist.
 */
val BlockPos.outlineBox: AABB
    get() {
        val blockState = getState() ?: return FULL_BOX
        if (blockState.isAir) {
            return FULL_BOX
        }

        val outlineShape = blockState.getShape(world, this)
        return if (outlineShape.isEmpty) {
            FULL_BOX
        } else {
            outlineShape.bounds()
        }
    }

val BlockPos.collisionShape: VoxelShape
    get() = this.getState()!!.getCollisionShape(world, this)

fun BlockState.outlineBox(blockPos: BlockPos): AABB {
    val outlineShape = this.getShape(world, blockPos)

    return if (outlineShape.isEmpty) {
        FULL_BOX
    } else {
        outlineShape.bounds()
    }
}

fun VoxelShape.getClosestSquaredDistanceTo(position: Position): Double {
    var minDistanceSq = Double.MAX_VALUE
    forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
        val nearestX = position.x().coerceIn(minX, maxX)
        val nearestY = position.y().coerceIn(minY, maxY)
        val nearestZ = position.z().coerceIn(minZ, maxZ)
        val distanceSq = (position.x() - nearestX).sq() +
            (position.y() - nearestY).sq() + (position.z() - nearestZ).sq()
        if (distanceSq < minDistanceSq) {
            minDistanceSq = distanceSq
        }
    }
    return minDistanceSq
}

/**
 * Shrinks a VoxelShape by the specified amounts on selected axes.
 */
@Suppress("CognitiveComplexMethod")
fun VoxelShape.shrink(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): VoxelShape {
    return when {
        this.isEmpty -> Shapes.empty()
        this == Shapes.block() -> Shapes.box(
            x, y, z,
            1.0 - x, 1.0 - y, 1.0 - z
        )

        else -> {
            var shape = Shapes.empty()

            this.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
                val width = maxX - minX
                val height = maxY - minY
                val depth = maxZ - minZ

                val canShrinkX = x == 0.0 || width > x * 2
                val canShrinkY = y == 0.0 || height > y * 2
                val canShrinkZ = z == 0.0 || depth > z * 2

                if (canShrinkX && canShrinkY && canShrinkZ) {
                    val shrunkBox = Shapes.box(
                        minX + (if (x > 0) x else 0.0),
                        minY + (if (y > 0) y else 0.0),
                        minZ + (if (z > 0) z else 0.0),
                        maxX - (if (x > 0) x else 0.0),
                        maxY - (if (y > 0) y else 0.0),
                        maxZ - (if (z > 0) z else 0.0)
                    )

                    shape = Shapes.joinUnoptimized(shape, shrunkBox, BooleanOp.OR)
                }
            }

            shape
        }
    }
}


/**
 * Some blocks like slabs or stairs must be placed on upper side in order to be placed correctly.
 */
val Block.mustBePlacedOnUpperSide: Boolean
    get() {
        return this is SlabBlock || this is StairBlock
    }

/**
 * Scan blocks around the position in a cuboid.
 */
fun Vec3.searchBlocksInCuboid(radius: Float): BoundingBox =
    BoundingBox(
        floor(x - radius).toInt(),
        floor(y - radius).toInt(),
        floor(z - radius).toInt(),
        ceil(x + radius).toInt(),
        ceil(y + radius).toInt(),
        ceil(z + radius).toInt(),
    )

/**
 * Scan blocks around the position in a cuboid with filtering.
 */
inline fun Vec3.searchBlocksInCuboid(
    radius: Float,
    crossinline filter: (BlockPos, BlockState) -> Boolean
): Sequence<Pair<BlockPos, BlockState>> =
    searchBlocksInCuboid(radius).iterator().asSequence().mapNotNull {
        val state = it.getState() ?: return@mapNotNull null

        if (filter(it, state)) {
            it.immutable() to state
        } else {
            null
        }
    }

/**
 * Search blocks around the position in a specific [radius]
 */
inline fun Vec3.searchBlocksInRadius(
    radius: Float,
    crossinline filter: (BlockPos, BlockState) -> Boolean,
): Sequence<Pair<BlockPos, BlockState>> =
    searchBlocksInCuboid(radius).iterator().asSequence().mapNotNull {
        val state = it.getState() ?: return@mapNotNull null

        if (it.distToCenterSqr(this@searchBlocksInRadius) <= radius.sq() && filter(it, state)) {
            it.immutable() to state
        } else {
            null
        }
    }

/**
 * Scan blocks around the position in a cuboid.
 */
fun BlockPos.searchBlocksInCuboid(radius: Int): BoundingBox =
    this.expendToBlockBox(radius, radius, radius)

/**
 * Scan blocks outwards from a bed
 */
fun BlockPos.searchBedLayer(state: BlockState, layers: Int): Sequence<IntLongPair> {
    check(state.isBed) { "This function is only available for Beds" }

    val anotherPartDirection = state.anotherBedPartDirection()!!
    val bedDirection = anotherPartDirection.opposite

    val left: Direction
    val right: Direction
    if (bedDirection.axis == Direction.Axis.X) {
        left = Direction.SOUTH
        right = Direction.NORTH
    } else {
        left = Direction.WEST
        right = Direction.EAST
    }

    return searchLayer(layers, bedDirection, Direction.UP, left, right) +
        relative(anotherPartDirection).searchLayer(layers, anotherPartDirection, Direction.UP, left, right)
}

/**
 * Scan blocks outwards from center along given [directions], up to [layers]
 *
 * @return The layer to the BlockPos (long value)
 */
@Suppress("detekt:CognitiveComplexMethod")
fun BlockPos.searchLayer(layers: Int, vararg directions: Direction): Sequence<IntLongPair> =
    sequence {
        val longValueOfThis = this@searchLayer.asLong()
        val initialCapacity = layers * layers * directions.size / 2

        val queue = ArrayDeque<IntLongPair>(initialCapacity).apply { add(IntLongPair.of(0, longValueOfThis)) }
        val visited = LongOpenHashSet(initialCapacity).apply { add(longValueOfThis) }

        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            val layer = next.leftInt()
            val pos = next.rightLong()

            if (layer > 0) {
                yield(next)
            }

            if (layer >= layers) continue

            // Search next layer
            for (direction in directions) {
                val newLong = BlockPos.offset(pos, direction)
                if (visited.add(newLong)) {
                    queue.add(IntLongPair.of(layer + 1, newLong))
                }
            }
        }
    }

fun BlockPos.getSortedSphere(radius: Float): Array<BlockPos> {
    val longs = CachedBlockPosSpheres.rangeLong(0, ceil(radius).toInt())
    val mutable = BlockPos.MutableBlockPos()
    return Array(longs.size) {
        mutable.set(longs.getLong(it))
        this.offset(mutable)
    }
}

/**
 * Basically [BlockGetter.raycast] but this method allows us to exclude blocks using [exclude].
 */
@Suppress("SpellCheckingInspection", "CognitiveComplexMethod")
fun BlockGetter.raycast(
    context: ClipContext,
    exclude: Array<BlockPos>?,
    include: BlockPos?,
    maxBlastResistance: Float?
): BlockHitResult {
    return BlockGetter.traverseBlocks(
        context.from, context.to, context,
        { raycastContext, pos ->
            val excluded = exclude?.let { pos in it } ?: false

            val blockState = if (excluded) {
                Blocks.VOID_AIR.defaultBlockState()
            } else if (include != null && pos == include) {
                Blocks.OBSIDIAN.defaultBlockState()
            } else {
                var state = getBlockState(pos)
                maxBlastResistance?.let {
                    if (state.block.explosionResistance < it) {
                        state = Blocks.VOID_AIR.defaultBlockState()
                    }
                }
                state
            }

            val fluidState = if (excluded) {
                Fluids.EMPTY.defaultFluidState()
            } else {
                var state = getFluidState(pos)
                maxBlastResistance?.let {
                    if (state.explosionResistance < it) {
                        state = Fluids.EMPTY.defaultFluidState()
                    }
                }
                state
            }

            val vec = raycastContext.from
            val vec2 = raycastContext.to

            val blockShape = raycastContext.getBlockShape(blockState, this, pos)
            val blockHitResult = clipWithInteractionOverride(vec, vec2, pos, blockShape, blockState)

            val fluidShape = raycastContext.getFluidShape(fluidState, this, pos)
            val fluidHitResult = fluidShape.clip(vec, vec2, pos)

            val blockHitDistance = blockHitResult?.let {
                raycastContext.from.distanceToSqr(blockHitResult.location)
            } ?: Double.MAX_VALUE
            val fluidHitDistance = fluidHitResult?.let {
                raycastContext.from.distanceToSqr(fluidHitResult.location)
            } ?: Double.MAX_VALUE

            if (blockHitDistance <= fluidHitDistance) blockHitResult else fluidHitResult
        },
        { raycastContext ->
            val vec = raycastContext.from.subtract(raycastContext.to)
            BlockHitResult.miss(
                raycastContext.to,
                Direction.getApproximateNearest(vec.x, vec.y, vec.z),
                BlockPos.containing(raycastContext.to)
            )
        })
}

fun BlockPos.canStandOn(): Boolean {
    return this.getState()!!.isFaceSturdy(world, this, Direction.UP, SupportType.CENTER)
}

fun BlockState?.anotherChestPartDirection(): Direction? {
    if (this?.block !is ChestBlock) return null

    if (ChestBlock.getBlockType(this) === DoubleBlockCombiner.BlockType.SINGLE) {
        return null
    }

    return ChestBlock.getConnectedDirection(this)
}

fun BlockState?.anotherBedPartDirection(): Direction? {
    if (this?.block !is BedBlock) return null

    // [body|head] -> (facing)
    val bedFacing = this.getValue(BedBlock.FACING)

    return if (BedBlock.getBlockType(this) == DoubleBlockCombiner.BlockType.FIRST) {
        bedFacing.opposite
    } else {
        bedFacing
    }
}

/**
 * Check if box is reaching of specified blocks
 */
inline fun AABB.isBlockAtPosition(
    isCorrectBlock: (Block?) -> Boolean,
): Boolean {
    val blockPos = BlockPos.MutableBlockPos(0, floor(minY).toInt(), 0)

    for (x in floor(minX).toInt()..ceil(maxX).toInt()) {
        for (z in floor(minZ).toInt()..ceil(maxZ).toInt()) {
            blockPos.x = x
            blockPos.z = z

            if (isCorrectBlock(blockPos.getBlock())) {
                return true
            }
        }
    }

    return false
}

/**
 * Check if box intersects with bounding box of specified blocks
 */
inline fun AABB.collideBlockIntersects(
    checkCollisionShape: Boolean = true,
    isCorrectBlock: (Block) -> Boolean
): Boolean {
    for (blockPos in collidingRegion) {
        val blockState = blockPos.getState()

        if (blockState == null || !isCorrectBlock(blockState.block)) {
            continue
        }

        if (!checkCollisionShape) {
            return true
        }

        val shape = blockState.getCollisionShape(mc.level!!, blockPos)

        if (shape.isEmpty) {
            continue
        }

        if (intersects(shape.bounds())) {
            return true
        }
    }

    return false
}

val AABB.collidingRegion: BoundingBox
    get() = BoundingBox(
        this.minX.toInt(), this.minY.toInt(), this.minZ.toInt(),
        ceil(this.maxX).toInt(), ceil(this.maxY).toInt(), ceil(this.maxZ).toInt(),
    )

fun BlockState.canBeReplacedWith(
    pos: BlockPos,
    usedStack: ItemStack,
): Boolean {
    val placementContext =
        BlockPlaceContext(
            mc.player!!,
            InteractionHand.MAIN_HAND,
            usedStack,
            BlockHitResult(Vec3.atLowerCornerOf(pos), Direction.UP, pos, false),
        )

    return canBeReplaced(
        placementContext,
    )
}

@Suppress("unused")
enum class SwingMode(
    override val tag: String,
    val serverSwing: Boolean,
) : Tagged, Consumer<InteractionHand> {

    DO_NOT_HIDE("DoNotHide", true),
    HIDE_BOTH("HideForBoth", false),
    HIDE_CLIENT("HideForClient", true),
    HIDE_SERVER("HideForServer", false);

    fun swing(hand: InteractionHand) = accept(hand)

    override fun accept(hand: InteractionHand) {
        when (this) {
            DO_NOT_HIDE -> player.swing(hand)
            HIDE_BOTH -> {}
            HIDE_CLIENT -> network.send(ServerboundSwingPacket(hand))
            HIDE_SERVER -> player.swing(hand, false)
        }
    }
}

val BlockHitResult.targetBlockPos: BlockPos get() = this.blockPos.relative(this.direction)

fun doPlacement(
    rayTraceResult: BlockHitResult,
    hand: InteractionHand = InteractionHand.MAIN_HAND,
    onPlacementSuccess: () -> Boolean = { true },
    onItemUseSuccess: () -> Boolean = { true },
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE
) {
    val stack = player.getItemInHand(hand)
    val count = stack.count

    val interactionResult = interaction.useItemOn(player, hand, rayTraceResult)

    when {
        interactionResult == InteractionResult.FAIL -> {
            return
        }

        interactionResult == InteractionResult.PASS -> {
            // Ok, we cannot place on the block, so let's just use the item in the direction
            // without targeting a block (for buckets, etc.)
            handlePass(hand, stack, onItemUseSuccess, swingMode)
            return
        }

        interactionResult.consumesAction() -> {
            val wasStackUsed = !stack.isEmpty && (stack.count != count || player.isCreative)

            handleActionsOnAccept(hand, interactionResult, wasStackUsed, onPlacementSuccess, swingMode)
        }
    }
}

/**
 * Swings item, resets equip progress and hand swing progress
 *
 * @param wasStackUsed was an item consumed in order to place the block
 * @param onPlacementSuccess if result of the lambda is true, swing hand with [swingMode]
 */
private inline fun handleActionsOnAccept(
    hand: InteractionHand,
    interactionResult: InteractionResult,
    wasStackUsed: Boolean,
    onPlacementSuccess: () -> Boolean,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE,
) {
    if (!interactionResult.shouldSwingHand()) {
        return
    }

    if (onPlacementSuccess()) {
        swingMode.swing(hand)
    }

    if (wasStackUsed) {
        mc.gameRenderer.itemInHandRenderer.itemUsed(hand)
    }

    return
}

private fun InteractionResult.shouldSwingHand(): Boolean {
    return this !is InteractionResult.Success ||
        this.swingSource != InteractionResult.SwingSource.SERVER
}


/**
 * Just interacts with the item in the hand instead of using it on the block
 */
private inline fun handlePass(
    hand: InteractionHand,
    stack: ItemStack,
    onItemUseSuccess: () -> Boolean,
    swingMode: SwingMode
) {
    if (stack.isEmpty) {
        return
    }

    val actionResult = interaction.useItem(player, hand)

    handleActionsOnAccept(hand, actionResult, true, onItemUseSuccess, swingMode)
}

/**
 * Breaks the block
 */
fun doBreak(
    rayTraceResult: BlockHitResult,
    immediate: Boolean = false,
    swingMode: SwingMode = SwingMode.DO_NOT_HIDE
) {
    val direction = rayTraceResult.direction
    val blockPos = rayTraceResult.blockPos

    if (player.isCreative) {
        if (interaction.startDestroyBlock(blockPos, rayTraceResult.direction)) {
            swingMode.swing(InteractionHand.MAIN_HAND)
            return
        }
    }

    if (immediate) {
        EventManager.callEvent(BlockBreakingProgressEvent(blockPos))

        interaction.startPrediction(world) { sequence ->
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, direction, sequence
            )
        }
        swingMode.swing(InteractionHand.MAIN_HAND)
        interaction.startPrediction(world) { sequence ->
            ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, blockPos, direction, sequence
            )
        }
        return
    }

    if (interaction.continueDestroyBlock(blockPos, direction)) {
        swingMode.swing(InteractionHand.MAIN_HAND)
        world.addBreakingBlockEffect(blockPos, direction)
    }
}

fun BlockState.isNotBreakable(pos: BlockPos) = !isBreakable(pos)

fun BlockState.isBreakable(pos: BlockPos): Boolean {
    return !isAir && (player.isCreative || getDestroySpeed(world, pos) >= 0f)
}

val FALL_DAMAGE_BLOCKING_BLOCKS = arrayOf(
    Blocks.WATER, Blocks.COBWEB, Blocks.POWDER_SNOW, Blocks.HAY_BLOCK, Blocks.SLIME_BLOCK
)

fun BlockPos?.isFallDamageBlocking(): Boolean {
    if (this == null) {
        return false
    }

    return getBlock() in FALL_DAMAGE_BLOCKING_BLOCKS
}

fun BlockPos.isBlastResistant(): Boolean {
    return getBlock()!!.explosionResistance >= 600f
}

@Suppress("UnusedReceiverParameter")
fun RespawnAnchorBlock.isCharged(state: BlockState): Boolean {
    return state.getValue(RespawnAnchorBlock.CHARGE) > 0
}

/**
 * Returns the second bed block position that might not exist (normally beds are two blocks long tho).
 */
@Suppress("UnusedReceiverParameter")
fun BedBlock.getPotentialSecondBedBlock(state: BlockState, pos: BlockPos): BlockPos {
    return pos.relative((state.getValue(HorizontalDirectionalBlock.FACING)).opposite)
}

// TODO replace this by an approach that automatically collects the blocks, this would create better mod compatibility
/**
 * Checks if the block can be interacted with, null will be returned as not interactable.
 * The [blockState] is optional but can make the result more accurate, if not provided
 * it will just assume the block is interactable.
 *
 * Note: The player is required to NOT be `null`.
 *
 * This data has been collected by looking at the implementations of [BlockBehaviour.useWithoutItem].
 */
fun Block?.isInteractable(blockState: BlockState?): Boolean {
    if (this == null) {
        return false
    }

    return this is BedBlock || this is AbstractChestBlock<*> || this is AbstractFurnaceBlock || this is AnvilBlock
        || this is BarrelBlock || this is BeaconBlock || this is BellBlock || this is BrewingStandBlock
        || this is ButtonBlock || this is CakeBlock && player.foodData.needsFood() || this is CandleCakeBlock
        || this is CartographyTableBlock
        || this is CaveVinesPlantBlock && blockState?.getValue(CaveVines.BERRIES) ?: true
        || this is CaveVinesBlock && blockState?.getValue(CaveVines.BERRIES) ?: true
        || this is ComparatorBlock || this is ComposterBlock && (blockState?.getValue(ComposterBlock.LEVEL) ?: 8) == 8
        || this is CrafterBlock || this is CraftingTableBlock || this is DaylightDetectorBlock
        || this is DecoratedPotBlock || this is DispenserBlock || this is DoorBlock || this is DragonEggBlock
        || this is EnchantingTableBlock || this is FenceGateBlock || this is FlowerPotBlock
        || this is GrindstoneBlock || this is HopperBlock || this is GameMasterBlock && player.canUseGameMasterBlocks()
        || this is JukeboxBlock && blockState?.getValue(JukeboxBlock.HAS_RECORD) == true || this is LecternBlock
        || this is LeverBlock || this is LightBlock && player.canUseGameMasterBlocks() || this is NoteBlock
        || this is RedStoneWireBlock || this is RepeaterBlock || this is RespawnAnchorBlock // this only works
        // when we hold glow stone or are not in the nether and the anchor is charged, but it'd be too error-prone when
        // it would be checked as the player can quickly switch to glow stone
        || this is ShulkerBoxBlock || this is StonecutterBlock
        || this is SweetBerryBushBlock && (blockState?.getValue(SweetBerryBushBlock.AGE) ?: 2) > 1
        || this is TrapDoorBlock
}

val BlockState?.isInteractable: Boolean get() = this?.block?.isInteractable(this) ?: false

fun BlockPos.isBlockedByEntities(): Boolean {
    val posBox = FULL_BOX + this
    return world.entitiesForRendering().any {
        it.boundingBox.intersects(posBox)
    }
}

inline fun BlockPos.getBlockingEntities(include: (Entity) -> Boolean = { true }): List<Entity> {
    val posBox = FULL_BOX + this
    return world.entitiesForRendering().filter {
        it.boundingBox.intersects(posBox) &&
            include.invoke(it)
    }
}

/**
 * Like [isBlockedByEntities] but it returns a blocking end crystal if present.
 */
fun BlockPos.isBlockedByEntitiesReturnCrystal(
    box: AABB = FULL_BOX,
    excludeIds: IntArray? = null
): BooleanObjectPair<EndCrystal?> {
    var blocked = false

    val posBox = box + this
    world.entitiesForRendering().forEach {
        if (it.boundingBox.intersects(posBox) && (excludeIds == null || it.id !in excludeIds)) {
            if (it is EndCrystal) {
                return BooleanObjectPair.of(true, it)
            }

            blocked = true
        }
    }

    return BooleanObjectPair.of(blocked, null)
}
