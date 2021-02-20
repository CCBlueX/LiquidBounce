/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.BlockType
import net.ccbluex.liquidbounce.api.enums.EnchantmentType
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoPot
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import kotlin.random.Random

@ModuleInfo(name = "InventoryCleaner", description = "Automatically throws away useless items.", category = ModuleCategory.PLAYER)
class InventoryCleaner : Module()
{

	/**
	 * OPTIONS
	 */

	val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 600, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val minCPS = minDelayValue.get()
			if (minCPS > newValue) set(minCPS)
		}
	}

	val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 400, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val maxDelay = maxDelayValue.get()
			if (maxDelay < newValue) set(maxDelay)
		}
	}

	private val maxHotbarDelayValue: IntegerValue = object : IntegerValue("MaxHotbarDelay", 250, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val minDelay = minHotbarDelayValue.get()
			if (minDelay > newValue) this.set(minDelay)
		}
	}

	private val minHotbarDelayValue: IntegerValue = object : IntegerValue("MinHotbarDelay", 200, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val maxDelay = maxHotbarDelayValue.get()
			if (maxDelay < newValue) this.set(maxDelay)
		}
	}

	// Bypass
	private val invOpenValue = BoolValue("InvOpen", false)
	private val simulateInventory = BoolValue("SimulateInventory", true)
	private val noMoveValue = BoolValue("NoMove", false)

	// Hotbar
	val hotbarValue = BoolValue("Hotbar", true)

	// Bypass
	private val randomSlotValue = BoolValue("RandomSlot", false)
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

	private val allowMisclicksValue = BoolValue("ClickMistakes", false)
	private val misclicksRateValue = IntegerValue("ClickMistakeRate", 5, 0, 100)

	// Sort
	private val items = arrayOf("None", "Ignore", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water", "Gapple", "Pearl")

	private val sortValue = BoolValue("Sort", true)

	private val slot1Value = ListValue("Slot-1", items, "Sword")
	private val slot2Value = ListValue("Slot-2", items, "Bow")
	private val slot3Value = ListValue("Slot-3", items, "Pickaxe")
	private val slot4Value = ListValue("Slot-4", items, "Axe")
	private val slot5Value = ListValue("Slot-5", items, "None")
	private val slot6Value = ListValue("Slot-6", items, "None")
	private val slot7Value = ListValue("Slot-7", items, "Food")
	private val slot8Value = ListValue("Slot-8", items, "Block")
	private val slot9Value = ListValue("Slot-9", items, "Block")

	// Item Filter Options
	private val keepOldSwordValue = BoolValue("KeepOldSword", false)
	private val keepOldToolsValue = BoolValue("KeepOldTools", false)

	private val bowAndArrowValue = BoolValue("BowAndArrow", true)
	private val bucketValue = BoolValue("Bucket", true)
	private val compassValue = BoolValue("Compass", true)
	private val enderPearlValue = BoolValue("EnderPearl", true)
	private val bedValue = BoolValue("Bed", true)
	private val ironIngotValue = BoolValue("IronIngot", true)
	private val diamondValue = BoolValue("Diamond", true)
	private val potionValue = BoolValue("Potion", true)
	private val foodValue = BoolValue("Food", true)

	private val ignoreVehiclesValue = BoolValue("IgnoreVehicles", false)

	// Visuals
	private val indicateClick = BoolValue("ClickIndicationh", false)
	private val indicateLength = IntegerValue("ClickIndicationLength", 100, 50, 200)

	/**
	 * VALUES
	 */

	private var delay = 0L

	private val infoUpdateCooldown = Cooldown.getNewCooldownMiliseconds(100)

	private var cachedInfo: String? = null

	val advancedInformations: String
		get() = if (cachedInfo == null || infoUpdateCooldown.attemptReset()) (if (!state) "InventoryCleaner is not active"
		else
		{
			val minDelay = minDelayValue.get()
			val maxDelay = maxDelayValue.get()
			val random = randomSlotValue.get()
			val noMove = noMoveValue.get()
			val hotbar = hotbarValue.get()
			val itemDelay = itemDelayValue.get()
			val misclick = allowMisclicksValue.get()
			val misclickRate = misclicksRateValue.get()

			"InventoryCleaner active [delay: ($minDelay ~ $maxDelay), itemdelay: $itemDelay, random: $random, nomove: $noMove, hotbar: $hotbar${if (misclick) ", misclick($misclickRate%)" else ""}]"
		}).apply { cachedInfo = this }
		else cachedInfo!!

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		// Delay, openContainer Check
		if (!InventoryUtils.CLICK_TIMER.hasTimePassed(delay) || thePlayer.openContainer != null && thePlayer.openContainer!!.windowId != 0) return

		val hotbar = hotbarValue.get()

		// Clean hotbar
		if (hotbar && !classProvider.isGuiInventory(mc.currentScreen))
		{
			val hotbarItems = items(36, 45)
			val garbageItemsHotbarSlots = hotbarItems.filter { !isUseful(thePlayer, it.key, it.value) }.keys.toMutableList()

			// Break if there is no garbage items in hotbar
			if (garbageItemsHotbarSlots.isNotEmpty())
			{
				val netHandler = mc.netHandler
				val randomSlot = randomSlotValue.get()

				var garbageHotbarItem = if (randomSlot) garbageItemsHotbarSlots[Random.nextInt(garbageItemsHotbarSlots.size)] else garbageItemsHotbarSlots.first()

				var misclick = false

				// Simulate Click Mistakes to bypass some anti-cheats
				if (allowMisclicksValue.get() && misclicksRateValue.get() > 0 && Random.nextInt(100) <= misclicksRateValue.get())
				{
					val firstEmpty: Int = firstEmpty(hotbarItems, randomSlot)
					if (firstEmpty != -1) garbageHotbarItem = firstEmpty
					misclick = true
				}

				// Switch to the slot of garbage item

				netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(garbageHotbarItem - 36))

				// Drop items
				val amount = getAmount(garbageHotbarItem)
				val action = if (amount > 1 || (amount == 1 && Math.random() > 0.8)) ICPacketPlayerDigging.WAction.DROP_ALL_ITEMS else ICPacketPlayerDigging.WAction.DROP_ITEM
				netHandler.addToSendQueue(classProvider.createCPacketPlayerDigging(action, WBlockPos.ORIGIN, classProvider.getEnumFacing(EnumFacingType.DOWN)))

				if (indicateClick.get() && classProvider.isGuiContainer(mc.currentScreen)) mc.currentScreen!!.asGuiContainer().highlight(garbageHotbarItem, indicateLength.get().toLong(), if (misclick) -2130771968 else -2147418368)

				// Back to the original holding slot
				netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

				delay = TimeUtils.randomDelay(minHotbarDelayValue.get(), maxHotbarDelayValue.get())
			}
		}

		// NoMove, AutoArmorLock Check
		if (noMoveValue.get() && MovementUtils.isMoving(thePlayer) || (LiquidBounce.moduleManager[AutoArmor::class.java] as AutoArmor).isLocked) return

		if (!classProvider.isGuiInventory(mc.currentScreen) && invOpenValue.get()) return

		// Sort hotbar
		if (sortValue.get()) sortHotbar()

		// Clean inventory
		cleanInventory(end = if (hotbar) 45 else 36)
	}

	fun cleanInventory(
		start: Int = 9, end: Int = 45, timer: MSTimer = InventoryUtils.CLICK_TIMER, container: IContainer = mc.thePlayer!!.inventoryContainer, delayResetFunc: Runnable = Runnable { delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get()) }
	): Boolean
	{
		val thePlayer = mc.thePlayer ?: return true

		var invItems = items(start, end, container)
		var garbageItems = invItems.filter { !isUseful(thePlayer, it.key, it.value) }.keys.toMutableList()

		if (garbageItems.isEmpty()) return true

		while (timer.hasTimePassed(delay))
		{
			invItems = items(start, end, container)
			garbageItems = invItems.filter { !isUseful(thePlayer, it.key, it.value) }.keys.toMutableList()

			// Return true if there is no remaining garbage items in the inventory
			if (garbageItems.isEmpty()) return true

			var garbageItem = if (randomSlotValue.get()) garbageItems[Random.nextInt(garbageItems.size)] else garbageItems.first()

			var misclick = false

			// Simulate Click Mistakes to bypass some anti-cheats
			if (allowMisclicksValue.get() && misclicksRateValue.get() > 0 && Random.nextInt(100) <= misclicksRateValue.get())
			{
				val firstEmpty: Int = firstEmpty(invItems, randomSlotValue.get())
				if (firstEmpty != -1) garbageItem = firstEmpty
				misclick = true
			}

			// SimulateInventory
			val openInventory = !classProvider.isGuiInventory(mc.currentScreen) && simulateInventory.get()
			if (openInventory) mc.netHandler.addToSendQueue(createOpenInventoryPacket())

			// Drop all useless items
			val amount = getAmount(garbageItem, container)
			if (amount > 1 || /* Click mistake simulation */ (amount == -1 && Random.nextBoolean())) mc.playerController.windowClick(thePlayer.openContainer!!.windowId, garbageItem, 1, 4, thePlayer)
			else mc.playerController.windowClick(thePlayer.openContainer!!.windowId, garbageItem, 0, 4, thePlayer)

			if (indicateClick.get() && classProvider.isGuiContainer(mc.currentScreen)) mc.currentScreen!!.asGuiContainer().highlight(garbageItem, indicateLength.get().toLong(), if (misclick) -2130771968 else -2147418368)

			timer.reset() // For more compatibility with custom MSTimer(s)

			// SimulateInventory
			if (openInventory) mc.netHandler.addToSendQueue(classProvider.createCPacketCloseWindow())

			delayResetFunc.run()
		}

		return false
	}

	/**
	 * Checks if the item is useful
	 *
	 * @param slot Slot id of the item.
	 * @return Returns true when the item is useful
	 */
	fun isUseful(thePlayer: IEntityPlayerSP, slot: Int, itemStack: IItemStack, start: Int = 0, end: Int = 45, container: IContainer = thePlayer.inventoryContainer): Boolean
	{
		return try
		{
			val item = itemStack.item

			when
			{
				classProvider.isItemSword(item) || classProvider.isItemTool(item) ->
				{
					if ((classProvider.isItemSword(item) && keepOldSwordValue.get()) || (classProvider.isItemTool(item) && keepOldToolsValue.get())) return true

					if (slot >= 36 && findBetterItem(slot - 36, thePlayer.inventory.getStackInSlot(slot - 36)) == slot - 36) return true

					for (i in 0..8) if ((type(i).equals("sword", true) && classProvider.isItemSword(item) || type(i).equals("pickaxe", true) && classProvider.isItemPickaxe(item) || type(i).equals("axe", true) && classProvider.isItemAxe(item)) && findBetterItem(i, thePlayer.inventory.getStackInSlot(i)) == null) return true

					val damage = (itemStack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 0.0) + 1.25 * ItemUtils.getEnchantment(itemStack, classProvider.getEnchantmentEnum(EnchantmentType.SHARPNESS))

					items(start, end, container = container).none { (otherSlot, stack) -> otherSlot != slot && stack != itemStack && stack.javaClass == itemStack.javaClass && damage < (stack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 0.0) + 1.25 * ItemUtils.getEnchantment(stack, classProvider.getEnchantmentEnum(EnchantmentType.SHARPNESS)) }
				}

				bowAndArrowValue.get() && classProvider.isItemBow(item) ->
				{
					val currPower = ItemUtils.getEnchantment(itemStack, classProvider.getEnchantmentEnum(EnchantmentType.POWER))

					items(start, end, container = container).none { (otherSlot, stack) -> otherSlot != slot && itemStack != stack && classProvider.isItemBow(stack.item) && currPower < ItemUtils.getEnchantment(stack, classProvider.getEnchantmentEnum(EnchantmentType.POWER)) }
				}

				classProvider.isItemArmor(item) ->
				{
					val currArmor = ArmorPiece(itemStack, slot)

					items(start, end, container = container).none { (otherSlot, otherStack) ->
						if (otherSlot != slot && otherStack != itemStack && classProvider.isItemArmor(otherStack.item))
						{
							val armor = ArmorPiece(otherStack, otherSlot)

							if (armor.armorType != currArmor.armorType) false
							else AutoArmor.ARMOR_COMPARATOR.compare(currArmor, armor) <= 0
						}
						else false
					}
				}

				compassValue.get() && itemStack.unlocalizedName == "item.compass" -> items(start, end, container = container).none { (otherSlot, stack) -> otherSlot != slot && itemStack != stack && stack.unlocalizedName == "item.compass" }

				else -> foodValue.get() && classProvider.isItemFood(item) || bowAndArrowValue.get() && itemStack.unlocalizedName == "item.arrow" || classProvider.isItemBlock(item) && !classProvider.isBlockBush(item?.asItemBlock()?.block) || bedValue.get() && classProvider.isItemBed(item) || diamondValue.get() && itemStack.unlocalizedName == "item.diamond" || ironIngotValue.get() && itemStack.unlocalizedName == "item.ingotIron" || potionValue.get() && classProvider.isItemPotion(item) && AutoPot.isPotionUseful(itemStack) || enderPearlValue.get() && classProvider.isItemEnderPearl(item) || classProvider.isItemEnchantedBook(item) || bucketValue.get() && classProvider.isItemBucket(item) || itemStack.unlocalizedName == "item.stick" || ignoreVehiclesValue.get() && (classProvider.isItemBoat(item) || classProvider.isItemMinecart(item))
			}
		}
		catch (ex: Exception)
		{
			ClientUtils.logger.error("(InventoryCleaner) Failed to check item: ${itemStack.unlocalizedName}.", ex)

			true
		}
	}

	/**
	 * INVENTORY SORTER
	 */

	/**
	 * Sort hotbar
	 */
	private fun sortHotbar()
	{
		for (index in 0..8)
		{
			val thePlayer = mc.thePlayer ?: return

			val bestItem = findBetterItem(index, thePlayer.inventory.getStackInSlot(index)) ?: continue

			if (bestItem != index)
			{
				val openInventory = !classProvider.isGuiInventory(mc.currentScreen) && simulateInventory.get()

				if (openInventory) mc.netHandler.addToSendQueue(createOpenInventoryPacket())

				mc.playerController.windowClick(0, if (bestItem < 9) bestItem + 36 else bestItem, index, 2, thePlayer)

				if (openInventory) mc.netHandler.addToSendQueue(classProvider.createCPacketCloseWindow())

				delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
				break
			}
		}
	}

	private fun findBetterItem(targetSlot: Int, slotStack: IItemStack?): Int?
	{
		val type = type(targetSlot)

		val thePlayer = mc.thePlayer ?: return null

		when (type.toLowerCase())
		{
			"sword", "pickaxe", "axe" ->
			{

				// https://youtrack.jetbrains.com/issue/KT-17018
				// https://youtrack.jetbrains.com/issue/KT-38704
				@Suppress("ConvertLambdaToReference")
				val currentTypeChecker: ((IItem?) -> Boolean) = when
				{
					type.equals("Sword", ignoreCase = true) -> { item: IItem? -> classProvider.isItemSword(item) }
					type.equals("Pickaxe", ignoreCase = true) -> { obj: IItem? -> classProvider.isItemPickaxe(obj) }
					type.equals("Axe", ignoreCase = true) -> { obj: IItem? -> classProvider.isItemAxe(obj) }
					else -> return null
				}

				var bestWeapon = if (currentTypeChecker(slotStack?.item)) targetSlot
				else -1

				thePlayer.inventory.mainInventory.forEachIndexed { index, itemStack ->
					if (itemStack != null && currentTypeChecker(itemStack.item) && !type(index).equals(type, ignoreCase = true))
					{
						if (bestWeapon == -1)
						{
							bestWeapon = index
						}
						else
						{
							val currDamage = (itemStack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 0.0) + 1.25 * ItemUtils.getEnchantment(itemStack, classProvider.getEnchantmentEnum(EnchantmentType.SHARPNESS))

							val bestStack = thePlayer.inventory.getStackInSlot(bestWeapon) ?: return@forEachIndexed
							val bestDamage = (bestStack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 0.0) + 1.25 * ItemUtils.getEnchantment(bestStack, classProvider.getEnchantmentEnum(EnchantmentType.SHARPNESS))

							if (bestDamage < currDamage) bestWeapon = index
						}
					}
				}

				return if (bestWeapon != -1 || bestWeapon == targetSlot) bestWeapon else null
			}

			"bow" ->
			{
				if (bowAndArrowValue.get())
				{
					var bestBow = if (classProvider.isItemBow(slotStack?.item)) targetSlot else -1
					var bestPower = if (bestBow != -1) ItemUtils.getEnchantment(slotStack, classProvider.getEnchantmentEnum(EnchantmentType.POWER))
					else 0

					thePlayer.inventory.mainInventory.forEachIndexed { index, itemStack ->
						if (classProvider.isItemBow(itemStack?.item) && !type(index).equals(type, ignoreCase = true))
						{
							if (bestBow == -1) bestBow = index
							else
							{
								val power = ItemUtils.getEnchantment(itemStack, classProvider.getEnchantmentEnum(EnchantmentType.POWER))

								if (ItemUtils.getEnchantment(itemStack, classProvider.getEnchantmentEnum(EnchantmentType.POWER)) > bestPower)
								{
									bestBow = index
									bestPower = power
								}
							}
						}
					}

					return if (bestBow != -1) bestBow else null
				}
			}

			"food" ->
			{
				if (foodValue.get())
				{
					thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
						if (stack != null)
						{
							val item = stack.item

							if (classProvider.isItemFood(item) && !classProvider.isItemAppleGold(item) && !type(index).equals("Food", ignoreCase = true))
							{
								val replaceCurr = ItemUtils.isStackEmpty(slotStack) || !classProvider.isItemFood(item)

								return@findBetterItem if (replaceCurr) index else null
							}
						}
					}
				}
			}

			"block" ->
			{
				thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
					if (stack != null)
					{
						val item = stack.item!!

						if (classProvider.isItemBlock(item) && !InventoryUtils.AUTOBLOCK_BLACKLIST.contains(item.asItemBlock().block) && !type(index).equals("Block", ignoreCase = true))
						{
							val replaceCurr = ItemUtils.isStackEmpty(slotStack) || !classProvider.isItemBlock(item)

							return@findBetterItem if (replaceCurr) index else null
						}
					}
				}
			}

			"water" ->
			{
				if (bucketValue.get())
				{
					thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
						if (stack != null)
						{
							val item = stack.item!!

							if (classProvider.isItemBucket(item) && item.asItemBucket().isFull == classProvider.getBlockEnum(BlockType.FLOWING_WATER) && !type(index).equals("Water", ignoreCase = true))
							{
								val replaceCurr = ItemUtils.isStackEmpty(slotStack) || !classProvider.isItemBucket(item) || (item.asItemBucket()).isFull != classProvider.getBlockEnum(BlockType.FLOWING_WATER)

								return@findBetterItem if (replaceCurr) index else null
							}
						}
					}
				}
			}

			"gapple" ->
			{
				if (foodValue.get())
				{
					thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
						if (stack != null)
						{
							val item = stack.item!!

							if (classProvider.isItemAppleGold(item) && !type(index).equals("Gapple", ignoreCase = true))
							{
								val replaceCurr = ItemUtils.isStackEmpty(slotStack) || !classProvider.isItemAppleGold(slotStack?.item)

								return@findBetterItem if (replaceCurr) index else null
							}
						}
					}
				}
			}

			"pearl" ->
			{
				if (enderPearlValue.get())
				{
					thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
						if (stack != null)
						{
							val item = stack.item

							if (classProvider.isItemEnderPearl(item) && !type(index).equals("Pearl", ignoreCase = true))
							{
								val replaceCurr = ItemUtils.isStackEmpty(slotStack) || !classProvider.isItemEnderPearl(slotStack?.item)

								return@findBetterItem if (replaceCurr) index else null
							}
						}
					}
				}
			}
		}

		return null
	}

	/**
	 * Get items in inventory
	 */
	private fun items(start: Int = 0, end: Int = 45, container: IContainer = mc.thePlayer!!.inventoryContainer): Map<Int, IItemStack>
	{
		val items = mutableMapOf<Int, IItemStack>()

		for (i in end - 1 downTo start)
		{
			val itemStack = container.getSlot(i).stack ?: continue

			if (ItemUtils.isStackEmpty(itemStack)) continue

			if (i in 36..44 && type(i).equals("Ignore", ignoreCase = true)) continue

			if (System.currentTimeMillis() - (itemStack).itemDelay >= itemDelayValue.get()) items[i] = itemStack
		}

		return items
	}

	private fun firstEmpty(slots: Map<Int, IItemStack?>?, random: Boolean): Int
	{
		slots ?: return -1

		val emptySlots = mutableListOf<Int>()

		slots.forEach { map: Map.Entry<Int, IItemStack?> ->
			if (map.value == null) emptySlots.add(map.key)
		}

		if (emptySlots.isEmpty()) return -1

		return if (random) emptySlots[Random.nextInt(emptySlots.size)] else emptySlots.first()
	}

	private fun getAmount(slot: Int, container: IContainer = mc.thePlayer!!.inventoryContainer): Int
	{
		val itemStack = container.inventorySlots[slot].stack ?: return -1
		itemStack.item ?: return -1
		return itemStack.stackSize
	}

	/**
	 * Get type of [targetSlot]
	 */
	private fun type(targetSlot: Int) = when (targetSlot)
	{
		0 -> slot1Value.get()
		1 -> slot2Value.get()
		2 -> slot3Value.get()
		3 -> slot4Value.get()
		4 -> slot5Value.get()
		5 -> slot6Value.get()
		6 -> slot7Value.get()
		7 -> slot8Value.get()
		8 -> slot9Value.get()
		else -> ""
	}

	override val tag: String
		get() = "${minDelayValue.get()} ~ ${maxDelayValue.get()}"
}
