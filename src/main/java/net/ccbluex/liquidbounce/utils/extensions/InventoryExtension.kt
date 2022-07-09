package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.block.*
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.Container
import net.minecraft.item.Item
import net.minecraft.item.ItemAppleGold
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemFood
import net.minecraft.util.BlockPos
import net.minecraft.world.World

// !! ---------------------------------------------------------------------------------------------------------------------------- !!
// inventoryContainer.getSlot(i).stack is using different Slot ID system unlike inventory.getStackInSlot()!!!
// ID system can be found on
// mc.thePlayer.inventoryContainer.getSlot(i).stack - https://wiki.vg/File:Inventory-slots.png
// mc.thePlayer.inventory.getStackInSlot() (= mc.thePlayer.inventory.mainInventory) - https://minecraft.gamepedia.com/File:Items_slot_number.png
// !! ---------------------------------------------------------------------------------------------------------------------------- !!

val BLACKLISTED_BLOCKS = run {
    arrayOf(
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
}

private val BLACKLISTED_FOODS = run {
    arrayOf(Items.rotten_flesh, Items.spider_eye, Items.poisonous_potato)
}

val InventoryPlayer.hasSpaceHotbar: Boolean
    get() = (0 until 9).map(::getStackInSlot).any { it == null }

val Block?.canAutoBlock: Boolean
    get() = this !in BLACKLISTED_BLOCKS && this !is BlockBush && this !is BlockRailBase && this !is BlockSign && this !is BlockDoor

fun Container.findItem(startSlot: Int, endSlot: Int, item: Item?, itemDelay: Long, random: Boolean): Int
{
    val candidates: MutableList<Int> = ArrayList(endSlot - startSlot)

    val currentTime = System.currentTimeMillis()

    (startSlot until endSlot).mapNotNull { it to (getSlot(it).stack ?: return@mapNotNull null) }.filter { it.second.item == item }.filter { currentTime - it.second.itemDelay >= itemDelay }.forEach { candidates.add(it.first) }

    return when
    {
        candidates.isEmpty() -> -1
        random -> candidates.random()
        else -> candidates.first()
    }
}

fun Container.findAutoBlockBlock(theWorld: World, autoblockFullcubeOnly: Boolean, boundingBoxYLimit: Double = 0.0): Int
{
    val hotbarSlots: MutableList<Int> = ArrayList(9)

    (36..44).forEach { i ->
        val itemStack = getSlot(i).stack
        if (itemStack != null && itemStack.item is ItemBlock && itemStack.stackSize > 0)
        {
            val block = (itemStack.item!! as ItemBlock).block
            if (block.canAutoBlock && block.defaultState!!.block.isFullCube) hotbarSlots.add(i)
        }
    }

    (if (boundingBoxYLimit == 0.0) hotbarSlots.firstOrNull()
    else hotbarSlots.filter {
        val block = (getSlot(it).stack?.item!! as ItemBlock).block
        val box = block.getCollisionBoundingBox(theWorld, BlockPos.ORIGIN, block.defaultState!!)

        box != null && box.maxY - box.minY <= boundingBoxYLimit
    }.maxByOrNull {
        val bb = (theWorld.getBlockDefaultCollisionBox((getSlot(it).stack?.item!! as ItemBlock).block))
        bb?.let { box -> box.maxY - box.minY } ?: 1.0
    })?.let { return@findAutoBlockBlock it }

    if (!autoblockFullcubeOnly)
    {
        hotbarSlots.clear() // Reuse list

        (36..44).forEach { i ->
            val itemStack = getSlot(i).stack
            if (itemStack != null && itemStack.item is ItemBlock && itemStack.stackSize > 0)
            {
                val itemBlock = itemStack.item!! as ItemBlock
                val block = itemBlock.block

                if (block.canAutoBlock) hotbarSlots.add(i)
            }
        }

        (if (boundingBoxYLimit == 0.0) hotbarSlots.firstOrNull()
        else hotbarSlots.filter {
            val block = (getSlot(it).stack?.item!! as ItemBlock).block
            val box = block.getCollisionBoundingBox(theWorld, BlockPos.ORIGIN, block.defaultState!!)

            box != null && box.maxY - box.minY <= boundingBoxYLimit
        }.maxByOrNull {
            val bb = theWorld.getBlockDefaultCollisionBox((getSlot(it).stack?.item!! as ItemBlock).block)
            bb?.let { box -> box.maxY - box.minY } ?: 1.0
        })?.let { return@findAutoBlockBlock it }
    }

    return -1
}

fun Container.firstEmpty(startSlot: Int, endSlot: Int, randomSlot: Boolean): Int
{
    val emptySlots = (startSlot until endSlot).filter { getSlot(it).stack == null }.toIntArray()

    return when
    {
        emptySlots.isEmpty() -> -1
        randomSlot -> emptySlots.random()
        else -> emptySlots.first()
    }
}

fun Container.findBestFood(foodLevel: Int, startSlot: Int = 36, endSlot: Int = 45, itemDelay: Long): Int
{

    val currentTime = System.currentTimeMillis()

    return (startSlot until endSlot).mapNotNull { it to (getSlot(it).stack ?: return@mapNotNull null) }.filter { it.second.item is ItemFood }.filterNot { it.second.item in BLACKLISTED_FOODS }.filter { currentTime - it.second.itemDelay >= itemDelay }.maxByOrNull { (_, stack) ->
        val foodStack = stack.item!! as ItemFood
        val healAmount = foodStack.getHealAmount(stack)

        val exactness = foodLevel + healAmount - 20
        (if (exactness !in 0..2) 0 else (3 - exactness) * 10) - (if (foodStack is ItemAppleGold) 15 else 0) + healAmount + foodStack.getSaturationModifier(stack)
    }?.first ?: -1
}
