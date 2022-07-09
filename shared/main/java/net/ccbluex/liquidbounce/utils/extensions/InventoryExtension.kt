package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce.wrapper
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.entity.player.IInventoryPlayer
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld

// !! ---------------------------------------------------------------------------------------------------------------------------- !!
// inventoryContainer.getSlot(i).stack is using different Slot ID system unlike inventory.getStackInSlot()!!!
// ID system can be found on
// mc.thePlayer.inventoryContainer.getSlot(i).stack - https://wiki.vg/File:Inventory-slots.png
// mc.thePlayer.inventory.getStackInSlot() (= mc.thePlayer.inventory.mainInventory) - https://minecraft.gamepedia.com/File:Items_slot_number.png
// !! ---------------------------------------------------------------------------------------------------------------------------- !!

val BLACKLISTED_BLOCKS = run {
    val provider = wrapper.classProvider
    arrayOf(
        // Interactible blocks
        provider.getBlockEnum(BlockType.CHEST), provider.getBlockEnum(BlockType.ENDER_CHEST), provider.getBlockEnum(BlockType.TRAPPED_CHEST), provider.getBlockEnum(BlockType.ANVIL), provider.getBlockEnum(BlockType.DISPENSER), provider.getBlockEnum(BlockType.DROPPER), provider.getBlockEnum(BlockType.FURNACE), provider.getBlockEnum(BlockType.LIT_FURNACE), provider.getBlockEnum(BlockType.CRAFTING_TABLE), provider.getBlockEnum(BlockType.ENCHANTING_TABLE), provider.getBlockEnum(BlockType.JUKEBOX), provider.getBlockEnum(BlockType.BED), provider.getBlockEnum(BlockType.NOTEBLOCK),

        // Some excepted blocks
        provider.getBlockEnum(BlockType.TORCH), provider.getBlockEnum(BlockType.REDSTONE_TORCH), provider.getBlockEnum(BlockType.REDSTONE_WIRE), provider.getBlockEnum(BlockType.LADDER), provider.getBlockEnum(BlockType.VINE), provider.getBlockEnum(BlockType.WATERLILY), provider.getBlockEnum(BlockType.CACTUS), provider.getBlockEnum(BlockType.GLASS_PANE), provider.getBlockEnum(BlockType.IRON_BARS), provider.getBlockEnum(BlockType.WEB),

        // Pressure plates
        provider.getBlockEnum(BlockType.STONE_PRESSURE_PLATE), provider.getBlockEnum(BlockType.WODDEN_PRESSURE_PLATE), provider.getBlockEnum(BlockType.LIGHT_WEIGHTED_PRESSURE_PLATE), provider.getBlockEnum(BlockType.HEAVY_WEIGHTED_PRESSURE_PLATE),

        // Falling blocks
        provider.getBlockEnum(BlockType.SAND), provider.getBlockEnum(BlockType.GRAVEL), provider.getBlockEnum(BlockType.TNT),

        // Signs
        provider.getBlockEnum(BlockType.STANDING_SIGN), provider.getBlockEnum(BlockType.WALL_SIGN),

        // Banners
        provider.getBlockEnum(BlockType.STANDING_BANNER), provider.getBlockEnum(BlockType.WALL_BANNER))
}

private val BLACKLISTED_FOODS = run {
    val provider = wrapper.classProvider
    arrayOf(provider.getItemEnum(ItemType.ROTTEN_FLESH), provider.getItemEnum(ItemType.SPIDER_EYE), provider.getItemEnum(ItemType.POISONOUS_POTATO))
}

val IInventoryPlayer.hasSpaceHotbar: Boolean
    get() = (0 until 9).map(::getStackInSlot).any { it == null }

val IBlock?.canAutoBlock: Boolean
    get()
    {
        val provider = wrapper.classProvider

        return this !in BLACKLISTED_BLOCKS && !provider.isBlockBush(this) && !provider.isBlockRailBase(this) && !provider.isBlockSign(this) && !provider.isBlockDoor(this)
    }

fun IContainer.findItem(startSlot: Int, endSlot: Int, item: IItem?, itemDelay: Long, random: Boolean): Int
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

fun IContainer.findAutoBlockBlock(theWorld: IWorld, autoblockFullcubeOnly: Boolean, boundingBoxYLimit: Double = 0.0): Int
{
    val hotbarSlots: MutableList<Int> = ArrayList(9)

    val provider = wrapper.classProvider

    (36..44).forEach { i ->
        val itemStack = getSlot(i).stack
        if (itemStack != null && provider.isItemBlock(itemStack.item) && itemStack.stackSize > 0)
        {
            val block = itemStack.item!!.asItemBlock().block

            if (block.canAutoBlock && block.isFullCube(block.defaultState!!)) hotbarSlots.add(i)
        }
    }

    (if (boundingBoxYLimit == 0.0) hotbarSlots.firstOrNull()
    else hotbarSlots.filter {
        val block = getSlot(it).stack?.item!!.asItemBlock().block
        val box = block.getCollisionBoundingBox(theWorld, WBlockPos.ORIGIN, block.defaultState!!)

        box != null && box.maxY - box.minY <= boundingBoxYLimit
    }.maxBy {
        val bb = theWorld.getBlockDefaultCollisionBox(getSlot(it).stack?.item!!.asItemBlock().block)
        bb?.let { box -> box.maxY - box.minY } ?: 1.0
    })?.let { return@findAutoBlockBlock it }

    if (!autoblockFullcubeOnly)
    {
        hotbarSlots.clear() // Reuse list

        (36..44).forEach { i ->
            val itemStack = getSlot(i).stack
            if (itemStack != null && provider.isItemBlock(itemStack.item) && itemStack.stackSize > 0)
            {
                val itemBlock = itemStack.item!!.asItemBlock()
                val block = itemBlock.block

                if (block.canAutoBlock) hotbarSlots.add(i)
            }
        }

        (if (boundingBoxYLimit == 0.0) hotbarSlots.firstOrNull()
        else hotbarSlots.filter {
            val block = getSlot(it).stack?.item!!.asItemBlock().block
            val box = block.getCollisionBoundingBox(theWorld, WBlockPos.ORIGIN, block.defaultState!!)

            box != null && box.maxY - box.minY <= boundingBoxYLimit
        }.maxBy {
            val bb = theWorld.getBlockDefaultCollisionBox(getSlot(it).stack?.item!!.asItemBlock().block)
            bb?.let { box -> box.maxY - box.minY } ?: 1.0
        })?.let { return@findAutoBlockBlock it }
    }

    return -1
}

fun IContainer.firstEmpty(startSlot: Int, endSlot: Int, randomSlot: Boolean): Int
{
    val emptySlots = (startSlot until endSlot).filter { getSlot(it).stack == null }.toIntArray()

    return when
    {
        emptySlots.isEmpty() -> -1
        randomSlot -> emptySlots.random()
        else -> emptySlots.first()
    }
}

fun IContainer.findBestFood(foodLevel: Int, startSlot: Int = 36, endSlot: Int = 45, itemDelay: Long): Int
{

    val currentTime = System.currentTimeMillis()

    return (startSlot until endSlot).mapNotNull { it to (getSlot(it).stack ?: return@mapNotNull null) }.filter { wrapper.classProvider.isItemFood(it.second.item) }.filterNot { it.second.item in BLACKLISTED_FOODS }.filter { currentTime - it.second.itemDelay >= itemDelay }.maxBy { (_, stack) ->
        val foodStack = stack.item!!.asItemFood()
        val healAmount = foodStack.getHealAmount(stack)

        val exactness = foodLevel + healAmount - 20
        (if (exactness !in 0..2) 0 else (3 - exactness) * 10) - (if (wrapper.classProvider.isItemAppleGold(foodStack)) 15 else 0) + healAmount + foodStack.getSaturationModifier(stack)
    }?.first ?: -1
}
