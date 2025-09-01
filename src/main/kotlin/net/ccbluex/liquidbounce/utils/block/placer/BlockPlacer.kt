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
package net.ccbluex.liquidbounce.utils.block.placer

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.FULL_BOX
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.collection.getSlot
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3i
import kotlin.math.max

@Suppress("TooManyFunctions")
class BlockPlacer(
    name: String,
    val module: ClientModule,
    val priority: Priority,
    val slotFinder: (BlockPos?) -> HotbarItemSlot?,
    allowSupportPlacements: Boolean = true
) : Configurable(name), EventListener {

    val range by float("Range", 4.5f, 1f..6f)
    val wallRange by float("WallRange", 4.5f, 0f..6f)
    val cooldown by intRange("Cooldown", 1..2, 0..40, "ticks")
    val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)

    /**
     * Construct a center hit result when the raytrace result is invalid.
     * This can make the module rotations wrong as well as place a bit outside the range,
     * but it makes the placements a lot more reliable and works on most servers.
     */
    val constructFailResult by boolean("ConstructFailResult", true)

    /**
     * Defines how long the player should sneak when placing on an interactable block.
     * This can make placing multiple blocks seem smoother.
     */
    val sneak by int("Sneak", 1, 0..10, "ticks")

    private val ignores by multiEnumChoice("Ignore", Ignore.entries)

    val ignoreOpenInventory get() = Ignore.OPEN_INVENTORY in ignores

    val ignoreUsingItem get() = Ignore.USING_ITEM in ignores

    val slotResetDelay by intRange("SlotResetDelay", 4..6, 0..40, "ticks")

    val rotationMode = choices(this, "RotationMode") {
        arrayOf(NormalRotationMode(it, this), NoRotationMode(it, this))
    }

    val support = SupportFeature(this)

    init {
        if (allowSupportPlacements) {
            tree(support)
        } else {
            support.enabled = false
        }
    }

    val crystalDestroyer = tree(CrystalDestroyFeature(this, module))

    /**
     * Renders all tracked positions that are queued to be placed.
     */
    val targetRenderer = tree(PlacementRenderer("TargetRendering", false, module))

    /**
     * Renders all placements.
     */
    val placedRenderer = tree(PlacementRenderer(
        "PlacedRendering",
        true,
        module,
        keep = false
    ))

    /**
     * Stores all block positions where blocks should be placed paired with a boolean that is `true`
     * if the position was added by [support].
     */
    val blocks = Object2BooleanLinkedOpenHashMap<BlockPos>()

    val inaccessible = hashSetOf<BlockPos>()
    var ticksToWait = 0
    var ranAction = false
    private var sneakTimes = 0

    @Suppress("unused")
    private val targetUpdater = handler<RotationUpdateEvent>(priority = -20) {
        if (ticksToWait > 0) {
            ticksToWait--
        } else if (ranAction) {
            ranAction = false
            ticksToWait = cooldown.random()
        }

        val inventoryOpen = !ignoreOpenInventory && mc.currentScreen is HandledScreen<*>
        val usingItem = !ignoreUsingItem && player.isUsingItem
        if (inventoryOpen || usingItem) {
            return@handler
        }

        if (blocks.isEmpty()) {
            return@handler
        }

        // return if no blocks are available
        slotFinder(null) ?: return@handler

        val itemStack = ItemStack(Items.SANDSTONE)

        inaccessible.clear()
        rotationMode.activeChoice.onTickStart()
        if (scheduleCurrentPlacements(itemStack)) {
            return@handler
        }

        // no possible position found, now a support placement can be considered

        if (support.enabled && support.chronometer.hasElapsed(support.delay.toLong())) {
            findSupportPath(itemStack)
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (sneakTimes > 0) {
            sneakTimes--
            event.sneak = true
        }
    }

    private fun findSupportPath(itemStack: ItemStack) {
        val currentPlaceCandidates = mutableSetOf<BlockPos>()
        var supportPath: Set<BlockPos>? = null

        // remove all positions of the current support path
        blocks.object2BooleanEntrySet().iterator().apply {
            while (hasNext()) {
                val entry = next()
                if (entry.booleanValue) {
                    currentPlaceCandidates.add(entry.key)
                    remove()
                }
            }
        }

        // find the best path
        (blocks.keys - inaccessible).forEach { pos ->
            support.findSupport(pos)?.let { path ->
                val size = path.size
                if (supportPath == null || supportPath!!.size > size) {
                    supportPath = path
                }

                // one block is almost the best we can get, so why bother scanning the other blocks
                if (size <= 1) {
                    return@forEach
                }
            }
        }

        // we found the same path again, updating is not required
        if (currentPlaceCandidates == supportPath) {
            currentPlaceCandidates.forEach { blocks.put(it, true) }
            return
        }

        currentPlaceCandidates.forEach(this::removeFromQueue)

        supportPath?.let { path ->
            (path - blocks.keys).forEach { pos ->
                addToQueue(pos, isSupport = true)
            }
            scheduleCurrentPlacements(itemStack)
        }

        support.chronometer.reset()
    }

    private fun scheduleCurrentPlacements(itemStack: ItemStack): Boolean {
        var hasPlaced = false

        val iterator = blocks.object2BooleanEntrySet().iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val pos = entry.key

            if (inaccessible.contains(pos)) {
                continue
            }

            if (isBlocked(pos)) {
                continue
            }

            val searchOptions = BlockPlacementTargetFindingOptions(
                BlockOffsetOptions(
                    listOf(Vec3i.ZERO),
                    BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
                ),
                FaceHandlingOptions(CenterTargetPositionFactory, considerFacingAwayFaces = wallRange > 0),
                stackToPlaceWith = itemStack,
                PlayerLocationOnPlacement(position = player.pos),
            )

            // TODO prioritize faces where sneaking is not required
            val placementTarget = findBestBlockPlacementTarget(pos, searchOptions) ?: continue

            // Check if we can reach the target
            if (!canReach(placementTarget.interactedBlockPos, placementTarget.rotation)) {
                inaccessible.add(pos)
                continue
            }

            ModuleDebug.debugGeometry(
                this, "PlacementTarget",
                ModuleDebug.DebuggedPoint(pos.toCenterPos(), Color4b.GREEN.with(a = 100))
            )

            // sneak when placing on interactable block to not trigger their action
            if (placementTarget.interactedBlockPos.getBlock().isInteractable(
                    placementTarget.interactedBlockPos.getState()
                )
            ) {
                sneakTimes = sneak - 1
            }

            if (rotationMode.activeChoice(entry.booleanValue, pos, placementTarget)) {
                return true
            }

            hasPlaced = true
        }

        return hasPlaced
    }

    private fun isBlocked(pos: BlockPos): Boolean {
        if (!pos.getState()!!.isReplaceable) {
            inaccessible.add(pos)
            return true
        }

        val blockedResult = pos.isBlockedByEntitiesReturnCrystal()
        if (crystalDestroyer.enabled) {
            blockedResult.value()?.let {
                crystalDestroyer.currentTarget = it
            }
        }

        if (blockedResult.keyBoolean()) {
            inaccessible.add(pos)
            return true
        }

        return false
    }

    fun doPlacement(isSupport: Boolean, pos: BlockPos, placementTarget: BlockPlacementTarget) {
        blocks.removeBoolean(pos)

        // choose block to place
        val slot = if (isSupport) {
            support.filter.getSlot(support.blocks)
        } else {
            slotFinder(pos)
        } ?: return

        val verificationRotation = rotationMode.activeChoice.getVerificationRotation(placementTarget.rotation)

        // check if we can still reach the target
        if (!canReach(placementTarget.interactedBlockPos, verificationRotation)) {
            return
        }

        // get the block hit result needed for the placement
        val blockHitResult = raytraceTarget(
            placementTarget.interactedBlockPos,
            verificationRotation,
            placementTarget.direction
        ) ?: return

        SilentHotbar.selectSlotSilently(this, slot, slotResetDelay.random())

        if (slot.itemStack.item !is BlockItem || pos.getState()!!.isReplaceable) {
            // place the block
            doPlacement(blockHitResult, hand = slot.useHand, swingMode = swingMode)
            placedRenderer.addBlock(pos)
        }

        targetRenderer.removeBlock(pos)
    }

    private fun raytraceTarget(pos: BlockPos, providedRotation: Rotation, direction: Direction): BlockHitResult? {
        val blockHitResult = raytraceBlock(
            range = max(range, wallRange).toDouble(),
            rotation = providedRotation,
            pos = pos,
            state = pos.getState()!!
        )

        if (blockHitResult != null && blockHitResult.type == HitResult.Type.BLOCK && blockHitResult.blockPos == pos) {
            return blockHitResult.withSide(direction)
        }

        if (constructFailResult) {
            return BlockHitResult(pos.toCenterPos(), direction, pos, false)
        }

        return null
    }

    fun canReach(pos: BlockPos, rotation: Rotation): Boolean {
        // not the exact distance but good enough
        val distance = pos.getCenterDistanceSquaredEyes()
        val wallRangeSq = wallRange.toDouble().sq()

        // if the wall range already covers it, the actual range doesn't matter
        if (distance <= wallRangeSq) {
            return true
        }

        val raycast = raycast(range = range.toDouble(), rotation = rotation)
        return raycast.type == HitResult.Type.BLOCK && raycast.blockPos == pos
    }

    /**
     * Removes all positions that are not in [positions] and adds all that are not in the queue.
     */
    fun update(positions: Set<BlockPos>) {
        val iterator = blocks.keys.iterator()
        while (iterator.hasNext()) {
            val position = iterator.next()
            if (position !in positions) {
                targetRenderer.removeBlock(position)
                iterator.remove()
            } else {
                blocks.put(position, false)
            }
        }

        positions.forEach { addToQueue(it, false) }
        targetRenderer.updateAll()
    }

    /**
     * Adds a block to be placed.
     *
     * @param update Whether the renderer should update the culling.
     */
    fun addToQueue(pos: BlockPos, update: Boolean = true, isSupport: Boolean = false) {
        if (blocks.containsKey(pos)) {
            return
        }

        blocks.put(pos, isSupport)
        targetRenderer.addBlock(pos, update, FULL_BOX)
    }

    /**
     * Removes a block from the queue.
     */
    fun removeFromQueue(pos: BlockPos) {
        blocks.removeBoolean(pos)
        targetRenderer.removeBlock(pos)
    }

    /**
     * Discards all blocks.
     */
    fun clear() {
        blocks.keys.forEach { targetRenderer.removeBlock(it) }
        blocks.clear()
    }

    /**
     * THis should be called when the module using this placer is disabled.
     */
    fun disable() {
        reset()
        crystalDestroyer.onDisable()
        targetRenderer.clearSilently()
        placedRenderer.clearSilently()
    }

    fun isDone(): Boolean {
        return blocks.isEmpty()
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    private fun reset() {
        sneakTimes = 0
        blocks.clear()
        inaccessible.clear()
    }

    override fun parent(): EventListener = module

    private enum class Ignore(override val choiceName: String) : NamedChoice {
        OPEN_INVENTORY("OpenInventory"),
        USING_ITEM("UsingItem")
    }
}
