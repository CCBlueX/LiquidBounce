/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.entity.player.IInventoryPlayer
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos.Companion.ORIGIN
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer

class InventoryUtils : MinecraftInstance(), Listenable
{
	@EventTarget
	fun onClick(@Suppress("UNUSED_PARAMETER") event: ClickWindowEvent?)
	{
		CLICK_TIMER.reset()
	}

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val packet = event.packet

		if (classProvider.isCPacketPlayerDigging(packet))
		{
			val digging = packet.asCPacketPlayerDigging()

			if ((digging.status == ICPacketPlayerDigging.WAction.DROP_ITEM || digging.status == ICPacketPlayerDigging.WAction.DROP_ALL_ITEMS) && digging.position == ORIGIN && classProvider.getEnumFacing(EnumFacingType.DOWN) == digging.facing) CLICK_TIMER.reset() // Drop (all) item(s) in hotbar with Q (Ctrl+Q)
		}

		if (classProvider.isCPacketPlayerBlockPlacement(packet)) CLICK_TIMER.reset()
	}

	override fun handleEvents(): Boolean = true

	companion object
	{
		// !! ---------------------------------------------------------------------------------------------------------------------------- !!
		// inventoryContainer.getSlot(i).stack is using different Slot ID system unlike inventory.getStackInSlot()
		// ID system can be found on
		// mc.thePlayer.inventoryContainer.getSlot(i).stack - https://wiki.vg/File:Inventory-slots.png
		// mc.thePlayer.inventory.getStackInSlot() (same as mc.thePlayer.inventory.mainInventory) - https://minecraft.gamepedia.com/File:Items_slot_number.png
		// !! ---------------------------------------------------------------------------------------------------------------------------- !!

		val AUTOBLOCK_BLACKLIST = run {
			val provider = classProvider
			arrayOf(
				// Interactible blocks
				provider.getBlockEnum(BlockType.CHEST), provider.getBlockEnum(BlockType.ENDER_CHEST), provider.getBlockEnum(BlockType.TRAPPED_CHEST), provider.getBlockEnum(BlockType.ANVIL), provider.getBlockEnum(BlockType.DISPENSER), provider.getBlockEnum(BlockType.DROPPER), provider.getBlockEnum(BlockType.FURNACE), provider.getBlockEnum(BlockType.LIT_FURNACE), provider.getBlockEnum(BlockType.CRAFTING_TABLE), provider.getBlockEnum(BlockType.ENCHANTING_TABLE), provider.getBlockEnum(BlockType.JUKEBOX), provider.getBlockEnum(BlockType.BED), provider.getBlockEnum(BlockType.NOTEBLOCK), provider.getBlockEnum(BlockType.WEB),

				// Some excepted blocks
				provider.getBlockEnum(BlockType.TORCH), provider.getBlockEnum(BlockType.REDSTONE_TORCH), provider.getBlockEnum(BlockType.REDSTONE_WIRE), provider.getBlockEnum(BlockType.LADDER), provider.getBlockEnum(BlockType.VINE), provider.getBlockEnum(BlockType.WATERLILY), provider.getBlockEnum(BlockType.CACTUS), provider.getBlockEnum(BlockType.GLASS_PANE), provider.getBlockEnum(BlockType.IRON_BARS),

				// Pressure plates
				provider.getBlockEnum(BlockType.STONE_PRESSURE_PLATE), provider.getBlockEnum(BlockType.WODDEN_PRESSURE_PLATE), provider.getBlockEnum(BlockType.LIGHT_WEIGHTED_PRESSURE_PLATE), provider.getBlockEnum(BlockType.HEAVY_WEIGHTED_PRESSURE_PLATE),

				// Falling blocks
				provider.getBlockEnum(BlockType.SAND), provider.getBlockEnum(BlockType.GRAVEL), provider.getBlockEnum(BlockType.TNT), provider.getBlockEnum(BlockType.STANDING_BANNER), provider.getBlockEnum(BlockType.WALL_BANNER))
		}

		val CLICK_TIMER = MSTimer()

		fun findItem(container: IContainer, startSlot: Int, endSlot: Int, item: IItem?, itemDelay: Long, random: Boolean): Int
		{
			val candidates: MutableList<Int> = ArrayList(endSlot - startSlot)

			(startSlot until endSlot).map { it to (container.getSlot(it).stack ?: return@map null) }.filterNotNull().filter { (_, stack) -> stack.item == item && stack.itemDelay >= itemDelay }.forEach { (i, _) -> candidates.add(i) }

			return when
			{
				candidates.isEmpty() -> -1
				random -> candidates.random()
				else -> candidates.first()
			}
		}

		fun hasSpaceHotbar(inventory: IInventoryPlayer): Boolean = (36..44).map(inventory::getStackInSlot).any { it == null }

		fun findAutoBlockBlock(theWorld: IWorldClient, container: IContainer, autoblockFullcubeOnly: Boolean, boundingBoxYLimit: Double): Int
		{
			val hotbarSlots: MutableList<Int> = ArrayList(9)

			(36..44).forEach { i ->
				val itemStack = container.getSlot(i).stack
				if (itemStack != null && classProvider.isItemBlock(itemStack.item) && itemStack.stackSize > 0)
				{
					val itemBlock = itemStack.item!!.asItemBlock()
					val block = itemBlock.block

					if (canAutoBlock(block) && block.isFullCube(block.defaultState!!)) hotbarSlots.add(i)
				}
			}

			val pred = if (boundingBoxYLimit == 0.0) hotbarSlots.firstOrNull()
			else hotbarSlots.filter {
				val block = container.getSlot(it).stack?.item!!.asItemBlock().block
				val box = block.getCollisionBoundingBox(theWorld, ORIGIN, block.defaultState!!)

				box != null && box.maxY - box.minY <= boundingBoxYLimit
			}.maxBy {
				val block = container.getSlot(it).stack?.item!!.asItemBlock().block
				block.getBlockBoundsMaxY() - block.getBlockBoundsMinY()
			}

			if (pred != null) return pred

			hotbarSlots.clear() // Reuse list

			if (!autoblockFullcubeOnly)
			{
				(36..44).forEach { i ->
					val itemStack = container.getSlot(i).stack
					if (itemStack != null && classProvider.isItemBlock(itemStack.item) && itemStack.stackSize > 0)
					{
						val itemBlock = itemStack.item!!.asItemBlock()
						val block = itemBlock.block

						if (canAutoBlock(block)) hotbarSlots.add(i)
					}
				}

				val pred2 = if (boundingBoxYLimit == 0.0) hotbarSlots.firstOrNull()
				else hotbarSlots.filter {
					val block = container.getSlot(it).stack?.item!!.asItemBlock().block
					val box = block.getCollisionBoundingBox(theWorld, ORIGIN, block.defaultState!!)

					box != null && box.maxY - box.minY <= boundingBoxYLimit
				}.maxBy {
					val block = container.getSlot(it).stack?.item!!.asItemBlock().block
					block.getBlockBoundsMaxY() - block.getBlockBoundsMinY()
				}

				if (pred2 != null) return pred2
			}

			return -1
		}

		fun canAutoBlock(block: IBlock?): Boolean = block !in AUTOBLOCK_BLACKLIST && !classProvider.isBlockBush(block) && !classProvider.isBlockRailBase(block) && !classProvider.isBlockSign(block) && !classProvider.isBlockDoor(block)

		fun firstEmpty(container: IContainer, startSlot: Int, endSlot: Int, randomSlot: Boolean): Int
		{
			val emptySlots = (startSlot until endSlot).filter { container.getSlot(it).stack == null }.toIntArray()

			return when
			{
				emptySlots.isEmpty() -> -1
				randomSlot -> emptySlots.random()
				else -> emptySlots.first()
			}
		}
	}
}
