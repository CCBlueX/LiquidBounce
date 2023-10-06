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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.utils.drawBoxNew
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.aiming.*
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.getNearestPoint
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.minecraft.block.*
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.abs

/**
 * AutoFarm module
 *
 * Automatically farms stuff for you.
 */
object ModuleAutoFarm : Module("AutoFarm", Category.WORLD) {
    // TODO Fix this entire module-
    private val range by float("Range", 5F, 1F..6F)
    private val wallRange by float("WallRange", 0f, 1F..6F).listen {
        if (it > range) {
            range
        } else {
            it
        }
    }
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

//    private val extraSearchRange by float("extraSearchRange", 0F, 0F..3F)
    private val breakDelay by intRange("BreakDelay", 0..1, 1..15)

    private val disableOnFullInventory by boolean("DisableOnFullInventory", false)

    private object AutoPlaceCrops : ToggleableConfigurable(this, "AutoPlaceCrops", true) {
        val swapBackDelay by intRange("swapBackDelay", 1..2, 1..20)
    }

    private val rotations = RotationsConfigurable()
    private val fortune by boolean("fortune", true)
    open class ToggleableBlockRenderer(module: Module? = null, name: String, enabled: Boolean) : ToggleableConfigurable(module, name, enabled) {
        private val color by color("Color", Color4b(66, 120, 245, 255))
        private val colorRainbow by boolean("Rainbow", false)
        val outline by boolean("Outline", true)

        private val box = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        fun render(renderEnvironment: RenderEnvironment, pos: Vec3, color: Color4b, outlineColor: Color4b) {
            val baseColor = if(colorRainbow) rainbow() else color


            with(renderEnvironment){
                withPosition(pos){
                    withColor(if(colorRainbow) rainbow() else color){
                        drawSolidBox(box)
                    }
                }
            }
        }
    }

    private object Visualize : ToggleableConfigurable(this, "Visualize", true) {
        private object Path : ToggleableConfigurable(this.module, "Path", true) {
            val color by color("PathColor", Color4b(36, 237, 0, 255))

            val renderHandler = handler<WorldRenderEvent> { event ->
                renderEnvironment(event.matrixStack){
                    withColor(color){
                        walkTarget?.let { target ->
                            drawLines(player.interpolateCurrentPosition(event.partialTicks), Vec3(target))
                        }
                    }
                }

            }

        }

        private object Blocks : ToggleableConfigurable(this.module, "FarmableBlocks", true) {
            val outline by boolean("Outline", true)

            private val readyBlockColor by color("ReadyBlockColor", Color4b(36, 237, 0, 255))
            private val farmBlockColor by color("FarmBlockColor", Color4b(36, 237, 0, 255))

            private val colorRainbow by boolean("Rainbow", false)

            // todo: use box of block, not hardcoded
            private val box = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            private object CurrentTarget : ToggleableConfigurable(this.module, "CurrentTarget", true) {
                private val color by color("Color", Color4b(66, 120, 245, 255))
                private val colorRainbow by boolean("Rainbow", false)

                fun render(renderEnvironment: RenderEnvironment) {
                    val target = currentTarget ?: return
                    with(renderEnvironment){
                        withPosition(Vec3(target)){
                            withColor((if(colorRainbow) rainbow() else color).alpha(50)){
                                drawSolidBox(box)
                            }
                        }
                    }
                }
            }


            val renderHandler = handler<WorldRenderEvent> { event ->
                val matrixStack = event.matrixStack
                val baseForReadyBlocks = if (colorRainbow) rainbow() else readyBlockColor
                val baseForFarmBlocks = if (colorRainbow) rainbow() else farmBlockColor

                val markedBlocks = BlockTracker.trackedBlockMap.keys
//                val markedFarmBlocks = FarmBlockTracker.trackedBlockMap.keys
                renderEnvironment(matrixStack) {
                    CurrentTarget.render(this)
                    for (pos in markedBlocks) {
                        val vec3 = Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())

                        val baseColor = baseForReadyBlocks.alpha(50)
                        val outlineColor = baseForReadyBlocks.alpha(100)

                        withPosition(vec3) {
                            withColor(baseColor) {
                                drawSolidBox(box)
                            }

                            if (outline) {
                                withColor(outlineColor) {
                                    drawOutlinedBox(box)
                                }
                            }
                        }
                    }
                }
            }
        }
        init {
            tree(Path)
            tree(Blocks)

        }





    }

    private object Walk : ToggleableConfigurable(this, "GotoCrops", false){

        // makes the player move to farmland blocks where there is need for crop replacement
        val toReplace by boolean("ToReplace", true)

        val toItems by boolean("ToItems", true)
    }
    init {
        tree(AutoPlaceCrops)
        tree(rotations)
        tree(Walk)
        tree(Visualize)
    }

    private val itemsForFarmland = arrayOf(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO)
    private val itemsForSoulsand = arrayOf(Items.NETHER_WART)
    // Rotation

    private var currentTarget: BlockPos? = null
    private var movingToBlock = false
    private var walkTarget: Vec3d? = null // Vec3d to support blocks and items

    private var invHadSpace = false

    private var farmLandBlocks = hashSetOf<Vec3d>()
    val velocityHandler = handler<PlayerVelocityStrafe> { event ->
        if (!movingToBlock || mc.currentScreen is HandledScreen<*>){
            return@handler
        }
        RotationManager.currentRotation?.let { rotation ->
            event.velocity = Entity.movementInputToVelocity(Vec3d(0.0, 0.0, 0.98), event.speed * 1.3f, rotation.yaw)
        }
    }
    val networkTickHandler = repeatable { _ ->
        // return if the user is inside a screen like the inventory
        if (mc.currentScreen is HandledScreen<*>) {
            return@repeatable
        }


        updateTarget()

        // return if the blink module is enabled
        if (ModuleBlink.enabled) {
            return@repeatable
        }

        // disable the module and return if the inventory is full and the setting for disabling the module is enabled
        if(disableOnFullInventory && !hasInventorySpace()){
            notification("Inventory is Full", "autoFarm has been disabled", NotificationEvent.Severity.ERROR)
            disable()
            enabled = false
            return@repeatable
        }

        // if there is no currentTarget (a block close enough to be interacted with) walk if wanted
        currentTarget ?: run {
            if(!Walk.enabled){
                // don't walk if it isn't enabled
                return@repeatable
            }

            val invHasSpace = hasInventorySpace()
            if(!invHasSpace && invHadSpace && Walk.toItems){
                notification("Inventory is Full", "autoFarm wont walk to items", NotificationEvent.Severity.ERROR)
            }
            invHadSpace = invHasSpace

            walkTarget = (findWalkToBlock() ?: if(Walk.toItems && invHasSpace) findWalkToItem() else null)

            val target = walkTarget ?: run {
                movingToBlock = false
                walkTarget = null
                return@repeatable
            }
            RotationManager.aimAt(RotationManager.makeRotation(target, player.eyes), configurable = rotations)
            movingToBlock = true

            return@repeatable
//            val rotation = RotationManager.currentRotation ?: return@repeatable

//            player.velocity = Entity.movementInputToVelocity(Vec3d(0.0, 0.0, 0.98), player.movementSpeed, rotation.yaw).add(player.velocity)

        }


        movingToBlock = false // disabling to stop the player from moving forward (see velocityHandler)
        walkTarget = null // resetting walkTarget to null to stop rendering

        val rotation = RotationManager.currentRotation ?: return@repeatable

        val rayTraceResult = raycast(range.toDouble(), rotation) ?: return@repeatable





        if (rayTraceResult.type != HitResult.Type.BLOCK
        ) {
            chat("hdhefef")
            return@repeatable
        }
        val blockPos = rayTraceResult.blockPos

        val state = rayTraceResult.blockPos.getState() ?: return@repeatable
        if(isTargeted(
                state,
                blockPos
            )){

            if (!state.isAir) {
                if(fortune){
                    findBestItem (1) { _, itemStack -> itemStack.getEnchantment(Enchantments.FORTUNE) }
                        ?.let { (slot, _) ->
                            SilentHotbar.selectSlotSilently(this, slot, 2)
                        } // swap to a fortune item to increase drops
                }
                val direction = rayTraceResult.side
                if (mc.interactionManager!!.updateBlockBreakingProgress(blockPos, direction)) {
                    player.swingHand(Hand.MAIN_HAND)
                }
                if(blockPos.down().getState()?.let { isFarmBlock(it) } == true && Walk.toReplace){
                    farmLandBlocks.add(Vec3d.ofCenter(blockPos))
                }


            }
        } else if(isFarmBlockWithAir(
                state,
                blockPos.offset(rayTraceResult.side).down())){
            val item =
                findClosestItem(
                    if(state.block is FarmlandBlock) {
                        itemsForFarmland
                    } else {
                        itemsForSoulsand
                    }
                )

            if(item != null){
                SilentHotbar.selectSlotSilently(this, item, AutoPlaceCrops.swapBackDelay.random())
                placeCrop(rayTraceResult)

                if(Walk.toReplace){
                    farmLandBlocks.remove(Vec3d.ofCenter(rayTraceResult.blockPos.offset(rayTraceResult.side)))
                }


            }
        }


    }

    private fun findWalkToBlock(): Vec3d?{

        if(BlockTracker.trackedBlockMap.isEmpty()){
            return null
        }

        val allowedItems = arrayOf(true, true, false, false)
        val hotbarItems = (0..8).map { player.inventory.getStack(it).item }
        for (item in hotbarItems){
            if(item in itemsForFarmland) allowedItems[2] = true
            else if(item in itemsForSoulsand) allowedItems[3] = true
        }

//        val closestCropBlock = BlockTracker.trackedBlockMap.keys.map { Vec3d.ofCenter(Vec3i(it.x, it.y, it.z))}
//            .minByOrNull { it.distanceTo(player.pos) }
//        val closestFarmBlock = farmLandBlocks.minByOrNull { it.distanceTo(player.pos) }

//        val closestBlock = (if (!Walk.toReplace) closestCropBlock else
//            listOf(closestCropBlock, closestFarmBlock)
//                .minByOrNull { it?.distanceTo(player.pos) ?: Double.MAX_VALUE})

        val closestBlock = BlockTracker.trackedBlockMap
            .filter { allowedItems[it.value.ordinal] }
            .keys
            .map { Vec3d.ofCenter(Vec3i(it.x, it.y, it.z)) }
            .minByOrNull { it.distanceTo(player.pos)}

        return closestBlock
    }

    private fun findWalkToItem() = world.entities.filter {it is ItemEntity &&  it.distanceTo(player) < 20}.minByOrNull { it.distanceTo(player) }?.pos



    private fun findClosestItem(items: Array<Item>) = (0..8).filter { player.inventory.getStack(it).item in items }
        .minByOrNull { abs(player.inventory.selectedSlot - it) }
    private fun findBestItem(validator: (Int, ItemStack) -> Boolean,
                             sort: (Int, ItemStack) -> Int = { slot, _ -> abs(player.inventory.selectedSlot - slot) }) = (0..8)
        .map {slot -> Pair (slot, player.inventory.getStack(slot)) }
        .filter { (slot, itemStack) -> validator (slot, itemStack) }
        .maxByOrNull { (slot, itemStack) -> sort (slot, itemStack) }


    private fun findBestItem(min: Int, sort: (Int, ItemStack) -> Int) = (0..8)
        .map {slot -> Pair (slot, player.inventory.getStack(slot)) }
        .maxByOrNull { (slot, itemStack) -> sort (slot, itemStack) }
        ?.takeIf {  (slot, itemStack) -> sort(slot, itemStack) >= min }

    private fun hasInventorySpace() = player.inventory.main.any { it.isEmpty }
    private fun placeCrop(rayTraceResult: BlockHitResult){
        val stack = player.mainHandStack
        val count = stack.count
        val interactBlock = interaction.interactBlock(player, Hand.MAIN_HAND, rayTraceResult)

        if (interactBlock.isAccepted) {
            if (interactBlock.shouldSwingHand()) {
                player.swingHand(Hand.MAIN_HAND)

                if (!stack.isEmpty && (stack.count != count || interaction.hasCreativeInventory())) {
                    mc.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.MAIN_HAND)
                }
            }

            return
        } else if (interactBlock == ActionResult.FAIL) {
            return
        }
    }

    private fun updateTarget() {
        this.currentTarget = null

        val radius = range
        val radiusSquared = radius * radius
        val eyesPos = player.eyes

        // searches for any blocks within the radius that need to be destroyed, such as crops.
        // If there are no such blocks, it proceeds to check if there are any blocks suitable for placing crops or nether wart on
        val blocksToBreak = searchBlocksInCuboid(radius.toInt()) { pos, state ->
            !state.isAir && isTargeted(state, pos) && getNearestPoint(
                eyesPos,
                Box(pos, pos.add(1, 1, 1))
            ).squaredDistanceTo(eyesPos) <= radiusSquared
        }.sortedBy { it.first.getCenterDistanceSquared() }


        for ((pos, state) in blocksToBreak) {
            val (rotation, _) = raytraceBlock(
                player.eyes,
                pos,
                state,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble()
            ) ?: continue // We don't have a free angle at the block? Well let me see the next.

            // set currentTarget to the new target
            this.currentTarget = pos
            // aim at target
            RotationManager.aimAt(rotation, openInventory = ignoreOpenInventory, configurable = rotations)
            break // We got a free angle at the block? No need to see more of them.
        }

        if (!AutoPlaceCrops.enabled) return
        val blocksToPlace =
            searchBlocksInCuboid(radius.toInt()) { pos, state ->
                !state.isAir && isFarmBlockWithAir(state, pos) && getNearestPoint(
                    eyesPos,
                    Box(pos, pos.add(1, 1, 1))
                ).squaredDistanceTo(eyesPos) <= radiusSquared
            }.sortedBy { it.first.getCenterDistanceSquared() }

        for ((pos, state) in blocksToPlace) {
            val (rotation, _) = raytracePlaceBlock(
                player.eyes,
                pos.up(),
                state,
                range = range.toDouble(),
                wallsRange = wallRange.toDouble()
            ) ?: continue // We don't have a free angle at the block? Well let me see the next.

            // set currentTarget to the new target
            this.currentTarget = pos
            // aim at target
            RotationManager.aimAt(rotation, openInventory = ignoreOpenInventory, configurable = rotations)
            break // We got a free angle at the block? No need to see more of them.
        }
    }


    private fun isTargeted(state: BlockState, pos: BlockPos): Boolean {
        val block = state.block

        return when (block) {
            is GourdBlock -> true
            is CropBlock -> block.isMature(state)
            is NetherWartBlock -> state.get(NetherWartBlock.AGE) >= 3
            is CocoaBlock -> state.get(CocoaBlock.AGE) >= 2
            is SugarCaneBlock -> isAboveLast<SugarCaneBlock>(pos)
            is CactusBlock -> isAboveLast<CactusBlock>(pos)
            is KelpPlantBlock -> isAboveLast<KelpPlantBlock>(pos)
            is BambooBlock -> isAboveLast<BambooBlock>(pos)
            else -> false
        }
    }

    /**
     * checks if the block is either a farmland or soulsand block and has air above it
     */
    private fun isFarmBlockWithAir(state: BlockState, pos: BlockPos): Boolean {
        return isFarmBlock(state) && pos.up().getState()?.isAir == true
    }

    private fun isFarmBlock(state: BlockState): Boolean {
        val block = state.block
        return when (block) {
                is FarmlandBlock -> true
                is SoulSandBlock -> true
                else -> false
        }
    }

    private inline fun <reified T : Block> isAboveLast(pos: BlockPos): Boolean {
        return pos.down().getBlock() is T && pos.down(2).getBlock() !is T
    }


    override fun enable() {
//        ChunkScanner.subscribe(ReadyBlockTracker)
        ChunkScanner.subscribe(BlockTracker)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(BlockTracker)
//        ChunkScanner.unsubscribe(FarmBlockTracker)
    }
    private enum class TrackedState {
        Destroy,
        Farmland,
        Soulsand
    }

    private object BlockTracker : AbstractBlockLocationTracker<TrackedState>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): TrackedState? {
            val block = state.block
            if(block !is FarmlandBlock){
                val stateBellow = pos.down().getState()
                if(stateBellow?.block is FarmlandBlock){
                    val targetBlockPos = TargetBlockPos(pos.down())
                    if(state.isAir){
                        this.trackedBlockMap[targetBlockPos] = TrackedState.Farmland
                        return null
                    } else {
                        this.trackedBlockMap.remove(targetBlockPos)
                    }
                }

                if (isTargeted(state, pos)) return TrackedState.Destroy

            } else if(pos.up().getState()?.isAir == true){
                return TrackedState.Farmland
            }
            return null


        }

    }


}
