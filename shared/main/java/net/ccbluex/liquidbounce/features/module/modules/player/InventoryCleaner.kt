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
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
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
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.value.*
import kotlin.random.Random

@ModuleInfo(name = "InventoryCleaner", description = "Automatically throws away useless items.", category = ModuleCategory.PLAYER)
class InventoryCleaner : Module()
{

	/**
	 * OPTIONS
	 */

	val delayValue = IntegerRangeValue("Delay", 400, 600, 0, 5000, "MaxDelay" to "MinDelay")
	val hotbarDelayValue = IntegerRangeValue("HotbarDelay", 200, 250, 0, 5000, "MaxHotbarDelay" to "MinHotbarDelay")
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

	// Bypass
	private val invOpenValue = BoolValue("InvOpen", false)
	private val simulateInventory = BoolValue("SimulateInventory", true)
	private val noMoveValue = BoolValue("NoMove", false)

	// Hotbar
	private val hotbarValue = BoolValue("Hotbar", true)

	// Bypass
	private val randomSlotValue = BoolValue("RandomSlot", false)

	private val misclickGroup = ValueGroup("ClickMistakes")
	private val misclickEnabledValue = BoolValue("Enabled", false, "ClickMistakes")
	private val misclickRateValue = IntegerValue("Rate", 5, 0, 100, "ClickMistakeRate")

	// Sort
	private val items = arrayOf("None", "Ignore", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water", "Gapple", "Pearl")
	private val sortGroup = ValueGroup("Sort")
	private val sortValue = BoolValue("Enabled", true, "Sort")
	private val slot1Value = ListValue("Slot-1", items, "Sword")
	private val slot2Value = ListValue("Slot-2", items, "Bow")
	private val slot3Value = ListValue("Slot-3", items, "Pickaxe")
	private val slot4Value = ListValue("Slot-4", items, "Axe")
	private val slot5Value = ListValue("Slot-5", items, "None")
	private val slot6Value = ListValue("Slot-6", items, "None")
	private val slot7Value = ListValue("Slot-7", items, "Food")
	private val slot8Value = ListValue("Slot-8", items, "Block")
	private val slot9Value = ListValue("Slot-9", items, "Block")

	private val filterGroup = ValueGroup("Filter")
	private val filterKeepOldSwordValue = BoolValue("KeepOldSword", false, "KeepOldSword")
	private val filterKeepOldToolsValue = BoolValue("KeepOldTools", false, "KeepOldTools")
	private val filterKeepOldArmorsValue = BoolValue("KeepOldArmors", false, "KeepOldArmors")

	private val filterBowAndArrowGroup = ValueGroup("BowAndArrow")
	private val filterBowAndArrowKeepValue = BoolValue("Keep", true, "BowAndArrow")
	private val filterBowAndArrowArrowCountValue = IntegerValue("MaxArrows", 2304, 64, 2304, "MaxArrows")

	private val filterFoodGroup = ValueGroup("Food")
	private val filterFoodKeepValue = BoolValue("Keep", true, "Food")
	private val filterFoodCountValue = IntegerValue("MaxFoods", 2304, 64, 2304, "MaxFoods")

	private val filterMaxBlockCountValue = IntegerValue("MaxBlocks", 2304, 64, 2304, "MaxBlocks")

	private val filterBucketValue = BoolValue("Bucket", true, "Bucket")
	private val filterCompassValue = BoolValue("Compass", true, "Compass")
	private val filterEnderPearlValue = BoolValue("EnderPearl", true, "EnderPearl")
	private val filterBedValue = BoolValue("Bed", true, "Bed")
	private val filterSkullValue = BoolValue("Skull", false, "Skull")
	private val filterPotionValue = BoolValue("Potion", true, "Potion")
	private val filterIronIngotValue = BoolValue("IronIngot", true, "IronIngot")
	private val filterDiamondValue = BoolValue("Diamond", true, "Diamond")

	private val filterIgnoreVehiclesValue = BoolValue("IgnoreVehicles", false, "IgnoreVehicles")
	private val filterMaxDuplicateCountValue = IntegerValue("MaxDuplicate", 2304, 64, 2304)

	// Visuals
	private val clickIndicationGroup = ValueGroup("ClickIndication")
	private val clickIndicationEnabledValue = BoolValue("Enabled", false, "ClickIndication")
	private val clickIndicationLengthValue = IntegerValue("Length", 100, 50, 1000, "ClickIndicationLength")

	/**
	 * VALUES
	 */

	private var delay = 0L

	private val infoUpdateCooldown = Cooldown.getNewCooldownMiliseconds(100)

	private var cachedInfo: String? = null

	val advancedInformations: String
		get()
		{
			val cache = cachedInfo

			return if (cache == null || infoUpdateCooldown.attemptReset()) (if (!state) "InventoryCleaner is not active"
			else
			{
				val minDelay = delayValue.getMin()
				val maxDelay = delayValue.getMax()
				val random = randomSlotValue.get()
				val noMove = noMoveValue.get()
				val hotbar = hotbarValue.get()
				val itemDelay = itemDelayValue.get()
				val misclick = misclickEnabledValue.get()
				val misclickRate = misclickRateValue.get()

				"InventoryCleaner active [delay: ($minDelay ~ $maxDelay), itemdelay: $itemDelay, random: $random, nomove: $noMove, hotbar: $hotbar${if (misclick) ", misclick($misclickRate%)" else ""}]"
			}).apply { cachedInfo = this }
			else cache
		}

	init
	{
		misclickGroup.addAll(misclickEnabledValue, misclickRateValue)
		sortGroup.addAll(sortValue, slot1Value, slot2Value, slot2Value, slot3Value, slot4Value, slot5Value, slot6Value, slot7Value, slot8Value, slot9Value)

		filterBowAndArrowGroup.addAll(filterBowAndArrowKeepValue, filterBowAndArrowArrowCountValue)
		filterFoodGroup.addAll(filterFoodKeepValue, filterFoodCountValue)
		filterGroup.addAll(filterBowAndArrowGroup, filterFoodGroup, filterMaxBlockCountValue, filterKeepOldSwordValue, filterKeepOldToolsValue, filterKeepOldArmorsValue, filterBucketValue, filterCompassValue, filterEnderPearlValue, filterBedValue, filterSkullValue, filterPotionValue, filterIronIngotValue, filterDiamondValue)

		clickIndicationGroup.addAll(clickIndicationEnabledValue, clickIndicationLengthValue)
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val inventoryContainer = thePlayer.inventoryContainer
		val openContainer = thePlayer.openContainer

		// Delay, openContainer Check
		if (!InventoryUtils.CLICK_TIMER.hasTimePassed(delay) || openContainer != null && openContainer.windowId != 0) return

		val hotbar = hotbarValue.get()

		val provider = classProvider

		val screen = mc.currentScreen

		// Clean hotbar
		if (hotbar && !provider.isGuiInventory(screen))
		{
			val hotbarItems = items(36, 45, inventoryContainer)
			val garbageItemsHotbarSlots = hotbarItems.filter { !isUseful(thePlayer, it.key, it.value, container = inventoryContainer) }.keys.toMutableList()

			// Break if there is no garbage items in hotbar
			if (garbageItemsHotbarSlots.isNotEmpty())
			{
				val netHandler = mc.netHandler
				val randomSlot = randomSlotValue.get()

				var garbageHotbarItem = if (randomSlot) garbageItemsHotbarSlots[Random.nextInt(garbageItemsHotbarSlots.size)] else garbageItemsHotbarSlots.first()

				var misclick = false

				val misclickRate = misclickRateValue.get()

				// Simulate Click Mistakes to bypass some anti-cheats
				if (misclickEnabledValue.get() && misclickRate > 0 && Random.nextInt(100) <= misclickRate)
				{
					val firstEmpty: Int = firstEmpty(hotbarItems, randomSlot)
					if (firstEmpty != -1) garbageHotbarItem = firstEmpty
					misclick = true
				}

				// Switch to the slot of garbage item

				netHandler.addToSendQueue(provider.createCPacketHeldItemChange(garbageHotbarItem - 36))

				// Drop items
				val amount = getAmount(garbageHotbarItem, inventoryContainer)
				val action = if (amount > 1 || (amount == 1 && Math.random() > 0.8)) ICPacketPlayerDigging.WAction.DROP_ALL_ITEMS else ICPacketPlayerDigging.WAction.DROP_ITEM
				netHandler.addToSendQueue(provider.createCPacketPlayerDigging(action, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))

				if (clickIndicationEnabledValue.get() && screen != null && provider.isGuiContainer(screen)) screen.asGuiContainer().highlight(garbageHotbarItem, clickIndicationLengthValue.get().toLong(), if (misclick) -2130771968 else -2147418368)

				// Back to the original holding slot
				netHandler.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

				delay = hotbarDelayValue.getRandomDelay()
			}
		}

		// NoMove, AutoArmorLock Check
		if (noMoveValue.get() && MovementUtils.isMoving(thePlayer) || (LiquidBounce.moduleManager[AutoArmor::class.java] as AutoArmor).isLocked) return

		if (!provider.isGuiInventory(screen) && invOpenValue.get()) return

		// Sort hotbar
		if (sortValue.get()) sortHotbar(thePlayer)

		// Clean inventory
		cleanInventory(thePlayer, end = if (hotbar) 45 else 36, container = inventoryContainer)
	}

	private fun cleanInventory(thePlayer: IEntityPlayerSP, start: Int = 9, end: Int = 45, container: IContainer)
	{
		val controller = mc.playerController
		val netHandler = mc.netHandler
		val screen = mc.currentScreen

		val clickTimer = InventoryUtils.CLICK_TIMER

		while (clickTimer.hasTimePassed(delay))
		{
			val items = items(start, end, container)
			val garbageItems = items.filterNot { isUseful(thePlayer, it.key, it.value, container = container) }.keys.toMutableList()

			// Return true if there is no remaining garbage items in the inventory
			if (garbageItems.isEmpty()) return

			val randomSlot = randomSlotValue.get()
			val misclickRate = misclickRateValue.get()

			var garbageItem = if (randomSlot) garbageItems[Random.nextInt(garbageItems.size)] else garbageItems.first()

			var misclick = false

			// Simulate Click Mistakes to bypass some anti-cheats
			if (misclickEnabledValue.get() && misclickRate > 0 && Random.nextInt(100) <= misclickRate)
			{
				val firstEmpty: Int = firstEmpty(items, randomSlot)
				if (firstEmpty != -1) garbageItem = firstEmpty
				misclick = true
			}

			val provider = classProvider

			// SimulateInventory
			val openInventory = simulateInventory.get() && !provider.isGuiInventory(screen)
			if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

			// Drop all useless items
			val amount = getAmount(garbageItem, container)

			if (amount > 1 || /* Mistake simulation */ Random.nextBoolean()) controller.windowClick(container.windowId, garbageItem, 1, 4, thePlayer) else controller.windowClick(container.windowId, garbageItem, 0, 4, thePlayer)

			if (clickIndicationEnabledValue.get() && screen != null && provider.isGuiContainer(screen)) screen.asGuiContainer().highlight(garbageItem, clickIndicationLengthValue.get().toLong(), if (misclick) -2130771968 else -2147418368)

			clickTimer.reset() // For more compatibility with custom MSTimer(s)

			// SimulateInventory
			if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

			delay = delayValue.getRandomDelay()
		}

		return
	}

	/**
	 * Checks if the item is useful
	 *
	 * @param slot Slot id of the item.
	 * @return Returns true when the item is useful
	 */
	fun isUseful(thePlayer: IEntityPlayer, slot: Int, itemStack: IItemStack, start: Int = 0, end: Int = 45, container: IContainer): Boolean
	{
		return try
		{
			val item = itemStack.item

			val provider = classProvider

			val containerItems = items(start, end, container)
			val inventoryItems = items(0, 45, container = thePlayer.inventoryContainer)

			val maxDuplicate = filterMaxDuplicateCountValue.get()
			val allowDuplicate = maxDuplicate >= 2304
			val valueComparator = if (maxDuplicate > 0) 1 else 0

			val bowAndArrow = filterBowAndArrowKeepValue.get()

			// TODO: Replace .none { ... } checks to .filter{ ... }.map { it.value.stackSize }.sum() < maxDuplicate

			when
			{
				provider.isItemSword(item) || provider.isItemTool(item) ->
				{
					if ((provider.isItemSword(item) && filterKeepOldSwordValue.get()) || (provider.isItemTool(item) && filterKeepOldToolsValue.get())) return true

					val hotbarSlot = slot - 36
					if (hotbarSlot >= 0 && findBetterItem(thePlayer, hotbarSlot, thePlayer.inventory.getStackInSlot(hotbarSlot)) == hotbarSlot) return true

					repeat(9) {
						val type = type(it)
						if ((type.equals("sword", true) && provider.isItemSword(item) || type.equals("pickaxe", true) && provider.isItemPickaxe(item) || type.equals("axe", true) && provider.isItemAxe(item)) && findBetterItem(thePlayer, it, thePlayer.inventory.getStackInSlot(it)) == null) return@isUseful true
					}

					val getAttackDamage = { stack: IItemStack -> (stack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 0.0) + 1.25 * ItemUtils.getEnchantment(itemStack, provider.getEnchantmentEnum(EnchantmentType.SHARPNESS)) }

					val attackDamage = getAttackDamage(itemStack)

					containerItems.none { (otherSlot, otherStack) -> otherSlot != slot && otherStack != itemStack && otherStack.javaClass == itemStack.javaClass && attackDamage.compareTo(getAttackDamage(otherStack)) < valueComparator }
				}

				bowAndArrow && provider.isItemBow(item) ->
				{
					val powerEnch = provider.getEnchantmentEnum(EnchantmentType.POWER)
					val currentPower = ItemUtils.getEnchantment(itemStack, powerEnch)

					containerItems.none { (otherSlot, otherStack) -> otherSlot != slot && otherStack != itemStack && provider.isItemBow(otherStack.item) && currentPower.compareTo(ItemUtils.getEnchantment(otherStack, powerEnch)) < valueComparator }
				}

				bowAndArrow && itemStack.unlocalizedName == "item.arrow" -> inventoryItems.filter { (otherSlot, otherStack) -> otherSlot != slot && otherStack.unlocalizedName == "item.arrow" }.values.sumBy(IItemStack::stackSize) + itemStack.stackSize <= filterBowAndArrowArrowCountValue.get()

				provider.isItemArmor(item) ->
				{
					if (filterKeepOldArmorsValue.get()) return true

					val currentArmor = ArmorPiece(itemStack, slot)

					containerItems.none { (otherSlot, otherStack) ->
						if (otherSlot != slot && otherStack != itemStack && provider.isItemArmor(otherStack.item))
						{
							val armor = ArmorPiece(otherStack, otherSlot)

							armor.armorType == currentArmor.armorType && AutoArmor.ARMOR_COMPARATOR.compare(currentArmor, armor) < valueComparator
						}
						else false
					}
				}

				filterCompassValue.get() && itemStack.unlocalizedName == "item.compass" -> allowDuplicate || inventoryItems.filter { (_, otherStack) -> itemStack != otherStack && otherStack.unlocalizedName == "item.compass" }.map { it.value.stackSize }.sum() < maxDuplicate

				filterBedValue.get() && provider.isItemBed(item) -> allowDuplicate || run {
					val name = itemStack.unlocalizedName
					inventoryItems.filter { (_, otherStack) -> itemStack != otherStack && otherStack.unlocalizedName == name }.map { it.value.stackSize }.sum() < maxDuplicate
				}

				provider.isItemBlock(item) && !provider.isBlockBush(item?.asItemBlock()?.block) && !provider.isBlockChest(item?.asItemBlock()?.block) ->
				{
					inventoryItems.filter { (otherSlot, otherStack) ->
						val otherItem = otherStack.item
						otherSlot != slot && provider.isItemBlock(otherItem) && !provider.isBlockBush(otherItem?.asItemBlock()?.block) && !provider.isBlockChest(item?.asItemBlock()?.block)
					}.values.sumBy(IItemStack::stackSize) + itemStack.stackSize <= filterMaxBlockCountValue.get()
				}

				filterFoodKeepValue.get() && provider.isItemFood(item) ->
				{
					val itemID = functions.getIdFromItem(item!!)

					inventoryItems.filter { (otherSlot, otherStack) ->
						otherSlot != slot && provider.isItemFood(otherStack.item) && (allowDuplicate || functions.getIdFromItem(otherStack.item!!) == itemID)
					}.values.sumBy(IItemStack::stackSize) + itemStack.stackSize <= filterFoodCountValue.get()
				}

				else -> filterDiamondValue.get() && itemStack.unlocalizedName == "item.diamond" // Diamond
					|| filterIronIngotValue.get() && itemStack.unlocalizedName == "item.ingotIron" // Iron
					|| filterPotionValue.get() && provider.isItemPotion(item) && AutoPot.isPotionUseful(itemStack) // Potion
					|| filterEnderPearlValue.get() && provider.isItemEnderPearl(item) // Ender Pearl
					|| provider.isItemEnchantedBook(item) // Enchanted Book
					|| filterBucketValue.get() && provider.isItemBucket(item) // Bucket
					|| itemStack.unlocalizedName == "item.stick" // Stick
					|| filterIgnoreVehiclesValue.get() && (provider.isItemBoat(item) || provider.isItemMinecart(item)) // Vehicles
					|| filterSkullValue.get() && provider.isItemSkull(item)
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
	private fun sortHotbar(thePlayer: IEntityPlayerSP)
	{
		val provider = classProvider
		val netHandler = mc.netHandler

		(0..8).mapNotNull { it to (findBetterItem(thePlayer, it, thePlayer.inventory.getStackInSlot(it)) ?: return@mapNotNull null) }.firstOrNull { (index, bestItem) -> index != bestItem }?.let { (index, bestItem) ->
			val openInventory = !provider.isGuiInventory(mc.currentScreen) && simulateInventory.get()

			if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

			mc.playerController.windowClick(0, if (bestItem < 9) bestItem + 36 else bestItem, index, 2, thePlayer)

			if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

			delay = delayValue.getRandomDelay()
		}
	}

	private fun findBetterItem(thePlayer: IEntityPlayer, hotbarSlot: Int, slotStack: IItemStack?): Int?
	{
		val type = type(hotbarSlot).toLowerCase()

		val provider = classProvider

		val mainInventory = thePlayer.inventory.mainInventory.asSequence().withIndex()

		when (type)
		{
			"sword", "pickaxe", "axe" ->
			{
				// Kotlin compiler bug
				// https://youtrack.jetbrains.com/issue/KT-17018
				// https://youtrack.jetbrains.com/issue/KT-38704
				@Suppress("ConvertLambdaToReference")
				val currentTypeChecker: ((IItem?) -> Boolean) = when (type)
				{
					"pickaxe" -> { item: IItem? -> provider.isItemPickaxe(item) }
					"axe" -> { item: IItem? -> provider.isItemAxe(item) }
					else -> { item: IItem? -> provider.isItemSword(item) }
				}

				var bestWeapon = if (currentTypeChecker(slotStack?.item)) hotbarSlot else -1

				val sharpEnch = provider.getEnchantmentEnum(EnchantmentType.SHARPNESS)
				val getAttackDamage = { stack: IItemStack -> (stack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 0.0) + 1.25 * ItemUtils.getEnchantment(stack, sharpEnch) }

				mainInventory.filter { currentTypeChecker(it.value?.item) }.map { it.index to it.value as IItemStack }.filter { !type(it.first).equals(type, ignoreCase = true) }.forEach { (index, stack) ->
					if (bestWeapon == -1) bestWeapon = index
					else
					{
						val currDamage = getAttackDamage(stack)

						val bestStack = thePlayer.inventory.getStackInSlot(bestWeapon) ?: return@forEach
						val bestDamage = getAttackDamage(bestStack)

						if (bestDamage < currDamage) bestWeapon = index
					}
				}

				return if (bestWeapon != -1 || bestWeapon == hotbarSlot) bestWeapon else null
			}

			"bow" -> if (filterBowAndArrowKeepValue.get())
			{
				var bestBow = if (provider.isItemBow(slotStack?.item)) hotbarSlot else -1

				val powerEnch = provider.getEnchantmentEnum(EnchantmentType.POWER)

				var bestPower = if (bestBow != -1) ItemUtils.getEnchantment(slotStack, powerEnch) else 0

				mainInventory.filter { provider.isItemBow(it.value?.item) }.map { it.index to it.value as IItemStack }.filter { !type(it.first).equals(type, ignoreCase = true) }.forEach { (index, stack) ->
					if (bestBow == -1) bestBow = index
					else
					{
						val power = ItemUtils.getEnchantment(stack, powerEnch)

						if (ItemUtils.getEnchantment(stack, powerEnch) > bestPower)
						{
							bestBow = index
							bestPower = power
						}
					}
				}

				return if (bestBow != -1) bestBow else null
			}

			"food" -> if (filterFoodKeepValue.get()) mainInventory.filter { provider.isItemFood(it.value?.item) }.map { it.index to it.value as IItemStack }.filter { !provider.isItemAppleGold(it.second) }.filter { !type(it.first).equals("Food", ignoreCase = true) }.toList().forEach { (index, stack) -> return@findBetterItem if (ItemUtils.isStackEmpty(slotStack) || !provider.isItemFood(stack.item)) index else null }
			"block" -> mainInventory.filter { provider.isItemBlock(it.value?.item) }.mapNotNull { it.index to (it.value?.item?.asItemBlock() ?: return@mapNotNull null) }.filter { !InventoryUtils.AUTOBLOCK_BLACKLIST.contains(it.second.block) }.filter { !type(it.first).equals("Block", ignoreCase = true) }.forEach { (index, item) -> return@findBetterItem if (ItemUtils.isStackEmpty(slotStack) || !provider.isItemBlock(item)) index else null }

			"water" -> if (filterBucketValue.get())
			{
				val flowingWater = provider.getBlockEnum(BlockType.FLOWING_WATER)
				mainInventory.filter { provider.isItemBucket(it.value?.item) }.mapNotNull { it.index to (it.value?.item?.asItemBucket() ?: return@mapNotNull null) }.filter { it.second.isFull == flowingWater }.filter { !type(it.first).equals("Water", ignoreCase = true) }.toList().forEach { (index, item) -> return@findBetterItem if (ItemUtils.isStackEmpty(slotStack) || !provider.isItemBucket(item) || (item.asItemBucket()).isFull != flowingWater) index else null }
			}

			"gapple" -> if (filterFoodKeepValue.get()) mainInventory.filter { provider.isItemAppleGold(it.value?.item) }.filter { !type(it.index).equals("Gapple", ignoreCase = true) }.forEach { return@findBetterItem if (ItemUtils.isStackEmpty(slotStack) || !provider.isItemAppleGold(slotStack?.item)) it.index else null }
			"pearl" -> if (filterEnderPearlValue.get()) mainInventory.filter { provider.isItemEnderPearl(it.value?.item) }.filter { !type(it.index).equals("Pearl", ignoreCase = true) }.forEach { return@findBetterItem if (ItemUtils.isStackEmpty(slotStack) || !provider.isItemEnderPearl(slotStack?.item)) it.index else null }
		}

		return null
	}

	/**
	 * Get items in inventory
	 */
	private fun items(start: Int = 0, end: Int = 45, container: IContainer): Map<Int, IItemStack>
	{
		val items = mutableMapOf<Int, IItemStack>()

		val itemDelay = itemDelayValue.get()

		val currentTime = System.currentTimeMillis()
		(end - 1 downTo start).filter { it !in 36..44 || !type(it).equals("Ignore", ignoreCase = true) }.mapNotNull { it to (container.getSlot(it).stack ?: return@mapNotNull null) }.filter { (_, stack) -> !ItemUtils.isStackEmpty(stack) && currentTime - stack.itemDelay >= itemDelay }.forEach { items[it.first] = it.second }

		return items
	}

	private fun firstEmpty(slots: Map<Int, IItemStack?>?, random: Boolean): Int
	{
		slots ?: return -1

		val emptySlots = mutableListOf<Int>()

		slots.forEach { (key, value) ->
			if (value == null) emptySlots.add(key)
		}

		if (emptySlots.isEmpty()) return -1

		return if (random) emptySlots.random() else emptySlots.first()
	}

	private fun getAmount(slot: Int, container: IContainer): Int
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
		get() = "${delayValue.getMin()} ~ ${delayValue.getMax()}"
}
