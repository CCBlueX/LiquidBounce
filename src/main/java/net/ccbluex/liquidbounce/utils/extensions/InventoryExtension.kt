package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.block.*
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.Container
import net.minecraft.item.*
import net.minecraft.util.BlockPos
import net.minecraft.world.World

// !! ---------------------------------------------------------------------------------------------------------------------------- !!
// inventoryContainer.getSlot(i).stack is using different Slot ID system unlike inventory.getStackInSlot()!!!
// ID system can be found on
// mc.thePlayer.inventoryContainer.getSlot(i).stack - https://wiki.vg/File:Inventory-slots.png
// mc.thePlayer.inventory.getStackInSlot() (= mc.thePlayer.inventory.mainInventory) - https://minecraft.gamepedia.com/File:Items_slot_number.png
// !! ---------------------------------------------------------------------------------------------------------------------------- !!

val BLACKLISTED_BLOCKS = arrayOf(
    // Interactible blocks
    Blocks.chest, Blocks.ender_chest, Blocks.trapped_chest, Blocks.anvil, Blocks.dispenser, Blocks.dropper, Blocks.furnace, Blocks.lit_furnace, Blocks.crafting_table, Blocks.enchanting_table, Blocks.jukebox, Blocks.bed, Blocks.noteblock,

    // Some excepted blocks
    Blocks.torch, Blocks.redstone_torch, Blocks.redstone_wire, Blocks.ladder, Blocks.vine, Blocks.waterlily, Blocks.cactus, Blocks.glass_pane, Blocks.iron_bars, Blocks.web,

    // Pressure plates
    Blocks.stone_pressure_plate, Blocks.wooden_pressure_plate, Blocks.light_weighted_pressure_plate, Blocks.heavy_weighted_pressure_plate,

    // Falling blocks
    Blocks.sand, Blocks.gravel, Blocks.tnt,

    // Signs
    Blocks.standing_sign, Blocks.wall_sign,

    // Banners
    Blocks.standing_banner, Blocks.wall_banner)

private val BLACKLISTED_FOODS = arrayOf(Items.rotten_flesh, Items.spider_eye, Items.poisonous_potato)

val InventoryPlayer.hasSpaceHotbar: Boolean
    get() = (0 until 9).map(::getStackInSlot).any { it == null }

val Block?.canAutoBlock: Boolean
    get() = this !in BLACKLISTED_BLOCKS && this !is BlockBush && this !is BlockRailBase && this !is BlockSign && this !is BlockDoor

fun Container.findItem(startSlot: Int, endSlot: Int, item: Item?, itemDelay: Long, random: Boolean): Int
{
    val currentTime = System.currentTimeMillis()
    return (startSlot until endSlot).mapToSlotAndStack(this).filter { it.second.item == item }.filter { currentTime - it.second.itemDelay >= itemDelay }.let { if (random) it.randomOrNull() else it.firstOrNull() }?.first ?: -1
}

fun Container.findAutoBlockBlock(theWorld: World, autoblockFullcubeOnly: Boolean, boundingBoxYLimit: Double = 0.0): Int
{
    val isStackValid: (Pair<Int, ItemStack>) -> Boolean = { (_, stack) -> stack.stackSize > 0 && stack.item.let { item -> item is ItemBlock && item.block.canAutoBlock } }

    // Check full-cube only
    (36..44).asSequence().mapToSlotAndStack(this).filter { isStackValid(it) && (it.second.item as ItemBlock).block.defaultState?.block?.isFullCube != false }.map(Pair<Int, *>::first).let { findOptimalSlot(theWorld, it, boundingBoxYLimit) }?.let { return@findAutoBlockBlock it }

    if (!autoblockFullcubeOnly) // Check for all blocks
        (36..44).asSequence().mapToSlotAndStack(this).filter(isStackValid).map(Pair<Int, ItemStack>::first).let { findOptimalSlot(theWorld, it, boundingBoxYLimit) }?.let { return@findAutoBlockBlock it }
    return -1
}

fun Container.firstEmpty(startSlot: Int, endSlot: Int, randomSlot: Boolean): Int = (startSlot until endSlot).filter { getSlot(it).stack == null }.let { (if (randomSlot) it.randomOrNull() else it.firstOrNull()) } ?: -1

fun Container.findBestFood(foodLevel: Int, startSlot: Int = 36, endSlot: Int = 45, itemDelay: Long): Int
{
    val currentTime = System.currentTimeMillis()

    return (startSlot until endSlot).asSequence().mapToSlotAndStack(this).filter { it.second.item.let { item -> item is ItemFood && item !in BLACKLISTED_FOODS } }.filter { currentTime - it.second.itemDelay >= itemDelay }.maxByOrNull { (_, stack) -> (stack.item as ItemFood).foodSelector(stack, foodLevel) }?.first ?: -1
}

/* Method chain helpers */

private fun Container.findOptimalSlot(theWorld: World, slotSeq: Sequence<Int>, boundingBoxYLimit: Double): Int?
{
    return if (boundingBoxYLimit == 0.0) slotSeq.firstOrNull()
    else slotSeq.filter {
        (getSlot(it).stack?.item!! as ItemBlock).block.let { block -> block.getCollisionBoundingBox(theWorld, BlockPos.ORIGIN, block.defaultState!!) }.let { box -> box != null && box.maxY - box.minY <= boundingBoxYLimit }
    }.maxByOrNull {
        theWorld.getBlockDefaultCollisionBox((getSlot(it).stack?.item!! as ItemBlock).block)?.let { box -> box.maxY - box.minY } ?: 1.0
    }
}

private fun ItemFood.foodSelector(stack: ItemStack, currentfoodLevel: Int): Float
{
    val healAmount = getHealAmount(stack)
    val exactness = currentfoodLevel + healAmount - 20
    return (if (exactness !in 0..2) 0 else (3 - exactness) * 10) - (if (this is ItemAppleGold) 15 else 0) + healAmount + getSaturationModifier(stack)
}

fun Sequence<Int>.mapToSlotAndStack(container: Container): Sequence<Pair<Int, ItemStack>> = mapNotNull { it to (container.getSlot(it).stack ?: return@mapNotNull null) }
fun Iterable<Int>.mapToSlotAndStack(container: Container): Collection<Pair<Int, ItemStack>> = mapNotNull { it to (container.getSlot(it).stack ?: return@mapNotNull null) }
