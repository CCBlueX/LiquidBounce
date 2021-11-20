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
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
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
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.extensions.BLACKLISTED_BLOCKS
import net.ccbluex.liquidbounce.utils.extensions.getEnchantmentLevel
import net.ccbluex.liquidbounce.utils.extensions.isEmpty
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.value.*
import java.lang.ref.SoftReference
import java.util.*
import kotlin.random.Random

private const val CLICKINDICATION_MISCLICK = -2147475201
private const val CLICKINDICATION_THROW = -2130771968
private const val CLICKINDICATION_REPLACE_FROM = -2130739200
private const val CLICKINDICATION_REPLACE_TO = -2130722816

@ModuleInfo(name = "InventoryCleaner", description = "Automatically throws away useless items.", category = ModuleCategory.PLAYER)
class InventoryCleaner : Module()
{
	/**
	 * OPTIONS
	 */

	val delayValue = IntegerRangeValue("Delay", 400, 600, 0, 5000, "MaxDelay" to "MinDelay")
	private val hotbarDelayValue = object : IntegerRangeValue("HotbarDelay", 200, 250, 0, 5000, "MaxHotbarDelay" to "MinHotbarDelay")
	{
		override fun showCondition(): Boolean = hotbarValue.get()
	}
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

	// Bypass
	private val invOpenValue = BoolValue("InvOpen", false, description = "Only perform cleaning when the inventory is open")
	private val simulateInventory = BoolValue("SimulateInventory", true, description = "Simulates inventory opening and closing with packets; Unnecessary if InvOpen option is present")
	private val noMoveValue = BoolValue("NoMove", false, "Only perform cleaning when you're not moving (Bypass InventoryMove checks)")

	// Hotbar
	private val hotbarValue = BoolValue("Hotbar", true, description = "Clean hotbar")

	// Bypass
	private val randomSlotValue = BoolValue("RandomSlot", false)

	private val misclickGroup = ValueGroup("ClickMistakes")
	private val misclickEnabledValue = BoolValue("Enabled", false, "ClickMistakes")
	private val misclickChanceValue = IntegerValue("Rate", 5, 0, 100, "ClickMistakeRate")

	// Sort
	private val items = arrayOf("None", "Ignore", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water", "Gapple", "Pearl")
	private val sortGroup = ValueGroup("Sort")
	private val sortValue = BoolValue("Enabled", true, "Sort")
	private val slot1Value = ListValue("1", items, "Sword", "Slot-1")
	private val slot2Value = ListValue("2", items, "Bow", "Slot-2")
	private val slot3Value = ListValue("3", items, "Pickaxe", "Slot-3")
	private val slot4Value = ListValue("4", items, "Axe", "Slot-4")
	private val slot5Value = ListValue("5", items, "None", "Slot-5")
	private val slot6Value = ListValue("6", items, "None", "Slot-6")
	private val slot7Value = ListValue("7", items, "Food", "Slot-7")
	private val slot8Value = ListValue("8", items, "Block", "Slot-8")
	private val slot9Value = ListValue("9", items, "Block", "Slot-9")

	private val filterGroup = ValueGroup("Filter")
	private val filterKeepOldSwordValue = BoolValue("KeepOldSword", false, "KeepOldSword")
	private val filterKeepOldToolsValue = BoolValue("KeepOldTools", false, "KeepOldTools")
	private val filterKeepOldArmorsValue = BoolValue("KeepOldArmors", false, "KeepOldArmors")

	private val filterBowAndArrowGroup = ValueGroup("BowAndArrow")
	private val filterBowAndArrowBowCountValue = IntegerValue("MaxBows", 45, 0, 45)
	private val filterBowAndArrowArrowCountValue = IntegerValue("MaxArrows", 2304, 0, 2304, "MaxArrows")

	private val filterFoodCountValue = IntegerValue("MaxFoods", 2304, 0, 2304, "MaxFoods")
	private val filterMaxBlockCountValue = IntegerValue("MaxBlocks", 2304, 0, 2304, "MaxBlocks")
	private val filterBedCountValue = IntegerValue("MaxBeds", 45, 0, 45, "MaxFoods")
	private val filterCompassCountValue = IntegerValue("MaxCompasses", 45, 0, 45)

	private val filterBucketValue = BoolValue("Bucket", true, "Bucket")
	private val filterEnderPearlValue = BoolValue("EnderPearl", true, "EnderPearl")
	private val filterSkullValue = BoolValue("Skull", false, "Skull")
	private val filterPotionValue = BoolValue("Potion", true, "Potion")
	private val filterIronIngotValue = BoolValue("IronIngot", true, "IronIngot")
	private val filterDiamondValue = BoolValue("Diamond", true, "Diamond")
	private val filterIgnoreVehiclesValue = BoolValue("IgnoreVehicles", false, "IgnoreVehicles")

	private val filterMaxDuplicateCountValue = IntegerValue("MaxDuplicate", 2304, 0, 2304)

	// Visuals
	private val clickIndicationGroup = ValueGroup("ClickIndication")
	private val clickIndicationEnabledValue = BoolValue("Enabled", false, "ClickIndication")
	private val clickIndicationLengthValue = IntegerValue("Length", 100, 50, 1000, "ClickIndicationLength")

	/**
	 * VALUES
	 */

	private var delay = 0L

	private val infoUpdateCooldown = Cooldown.createCooldownInMillis(100)

	private var cachedDebug: SoftReference<String>? = null

	val debug: String
		get()
		{
			val cache = cachedDebug?.get()

			return if (cache == null || infoUpdateCooldown.attemptReset()) (if (!state) "InventoryCleaner disabled"
			else
			{
				val builder = StringJoiner(", ")

				if (delayValue.getMax() > 0) builder.add("DELAY(${delayValue.getMin()}-${delayValue.getMax()}ms)")
				if (hotbarValue.get()) builder.add("HOTBARDELAY(${hotbarDelayValue.getMin()}-${hotbarDelayValue.getMax()}ms)")
				if (itemDelayValue.get() > 0) builder.add("ITEMDELAY(${itemDelayValue.get()}ms)")

				if (misclickEnabledValue.get() && misclickChanceValue.get() > 0) builder.add("MISCLICK(${misclickChanceValue.get()}%)")

				if (randomSlotValue.get()) builder.add("RANDOM")

				if (invOpenValue.get()) builder.add("INVOPEN")

				if (simulateInventory.get()) builder.add("SIMULATE")

				if (noMoveValue.get()) builder.add("NOMOVE")

				if (sortValue.get()) builder.add("SORT")

				"InventoryCleaner ENABLED {$builder}"
			}).also { cachedDebug = SoftReference(it) }
			else cache
		}

	init
	{
		misclickGroup.addAll(misclickEnabledValue, misclickChanceValue)
		sortGroup.addAll(sortValue, slot1Value, slot2Value, slot2Value, slot3Value, slot4Value, slot5Value, slot6Value, slot7Value, slot8Value, slot9Value)

		filterBowAndArrowGroup.addAll(filterBowAndArrowBowCountValue, filterBowAndArrowArrowCountValue)
		filterGroup.addAll(filterBowAndArrowGroup, filterMaxBlockCountValue, filterKeepOldSwordValue, filterKeepOldToolsValue, filterKeepOldArmorsValue, filterBucketValue, filterEnderPearlValue, filterFoodCountValue, filterCompassCountValue, filterBedCountValue, filterSkullValue, filterPotionValue, filterIronIngotValue, filterDiamondValue, filterIgnoreVehiclesValue, filterMaxDuplicateCountValue)

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
			val garbageItemsHotbarSlots = hotbarItems.filter { !isUseful(thePlayer, it.key, it.value, container = inventoryContainer) }.keys.toList()

			// Break if there is no garbage items in hotbar
			if (garbageItemsHotbarSlots.isNotEmpty())
			{
				val netHandler = mc.netHandler
				val randomSlot = randomSlotValue.get()

				var garbageHotbarItem = if (randomSlot) garbageItemsHotbarSlots[Random.nextInt(garbageItemsHotbarSlots.size)] else garbageItemsHotbarSlots.first()

				var misclick = false

				val misclickRate = misclickChanceValue.get()

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

				if (clickIndicationEnabledValue.get() && screen != null && provider.isGuiContainer(screen)) screen.asGuiContainer().highlight(garbageHotbarItem, clickIndicationLengthValue.get().toLong(), if (misclick) CLICKINDICATION_MISCLICK else CLICKINDICATION_THROW)

				// Back to the original holding slot
				netHandler.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

				delay = hotbarDelayValue.getRandomDelay()
			}
		}

		// NoMove, AutoArmor Lock Check
		if (noMoveValue.get() && thePlayer.isMoving || (LiquidBounce.moduleManager[AutoArmor::class.java] as AutoArmor).isLocked) return

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
			val garbageItems = items.filterNot { isUseful(thePlayer, it.key, it.value, container = container) }.keys.toList()

			// Return true if there is no remaining garbage items in the inventory
			if (garbageItems.isEmpty()) return

			val randomSlot = randomSlotValue.get()
			val misclickRate = misclickChanceValue.get()

			var garbageItem = if (randomSlot) garbageItems[Random.nextInt(garbageItems.size)] else garbageItems.first()

			var misclick = false

			// Simulate Click Mistakes to bypass some anti-cheats
			if (misclickEnabledValue.get() && misclickRate > 0 && Random.nextInt(100) <= misclickRate)
			{
				val firstEmpty = firstEmpty(items, randomSlot)
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

			if (clickIndicationEnabledValue.get() && screen != null && provider.isGuiContainer(screen)) screen.asGuiContainer().highlight(garbageItem, clickIndicationLengthValue.get().toLong(), if (misclick) CLICKINDICATION_MISCLICK else CLICKINDICATION_THROW)

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
	fun isUseful(thePlayer: IEntityPlayer, slot: Int, stack: IItemStack, start: Int = 0, end: Int = 45, container: IContainer): Boolean
	{
		return try
		{
			val item = stack.item
			val stackSize = stack.stackSize

			val provider = classProvider

			val maxDuplicate = filterMaxDuplicateCountValue.get()
			val valueComparator = if (maxDuplicate > 0) 1 else 0 // 0 -> '<', 1 -> '<='

			val items = items(start, end, container).filter { (otherSlot, otherStack) -> otherSlot != slot && otherStack != stack }
			val itemValues = items.values

			when
			{
				provider.isItemSword(item) || provider.isItemTool(item) ->
				{
					if ((provider.isItemSword(item) && filterKeepOldSwordValue.get()) || (provider.isItemTool(item) && filterKeepOldToolsValue.get())) return true

					val hotbarSlot = slot - 36
					if (hotbarSlot >= 0 && findBetterItem(thePlayer, hotbarSlot, thePlayer.inventory.getStackInSlot(hotbarSlot)) == hotbarSlot) return true

					repeat(9) { hotbar ->
						val type = type(hotbar)
						if ((type.equals("sword", true) && provider.isItemSword(item) || type.equals("pickaxe", true) && provider.isItemPickaxe(item) || type.equals("axe", true) && provider.isItemAxe(item)) && findBetterItem(thePlayer, hotbar, thePlayer.inventory.getStackInSlot(hotbar)) == null) return@isUseful true
					}

					val sharpEnch = provider.getEnchantmentEnum(EnchantmentType.SHARPNESS)
					val getAttackDamage = { itemStack: IItemStack -> (itemStack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 0.0) + 1.25 * itemStack.getEnchantmentLevel(sharpEnch) }
					val attackDamage = getAttackDamage(stack)
					val stackID = functions.getIdFromItem(item!!)
					itemValues.filter { it.item?.let(functions::getIdFromItem) == stackID }.filter { attackDamage.compareTo(getAttackDamage(it)) < valueComparator }.sumBy(IItemStack::stackSize) + stackSize <= maxDuplicate
				}

				filterBowAndArrowBowCountValue.get() > 0 && provider.isItemBow(item) ->
				{
					val powerEnch = provider.getEnchantmentEnum(EnchantmentType.POWER)
					val getPower = { itemStack: IItemStack -> itemStack.getEnchantmentLevel(powerEnch) }
					val currentPower = getPower(stack)
					itemValues.filter { provider.isItemBow(it.item) }.filter { currentPower.compareTo(getPower(it)) < valueComparator }.sumBy(IItemStack::stackSize) + stackSize <= filterBowAndArrowBowCountValue.get()
				}

				filterBowAndArrowArrowCountValue.get() > 0 && stack.unlocalizedName == "item.arrow" -> itemValues.filter { it.unlocalizedName == "item.arrow" }.sumBy(IItemStack::stackSize) + stackSize <= filterBowAndArrowArrowCountValue.get()

				provider.isItemArmor(item) ->
				{
					if (filterKeepOldArmorsValue.get()) return true

					val currentArmor = ArmorPiece(stack, slot)
					items.filter { provider.isItemArmor(it.value.item) }.filter { (otherSlot, otherStack) ->
						val otherArmor = ArmorPiece(otherStack, otherSlot)
						otherArmor.armorType == currentArmor.armorType && AutoArmor.ARMOR_COMPARATOR.compare(currentArmor, otherArmor) < valueComparator
					}.values.sumBy(IItemStack::stackSize) + stackSize <= maxDuplicate
				}

				filterCompassCountValue.get() > 0 && stack.unlocalizedName == "item.compass" -> itemValues.filter { it.unlocalizedName == "item.compass" }.sumBy(IItemStack::stackSize) + stackSize < filterCompassCountValue.get()

				filterBedCountValue.get() > 0 && provider.isItemBed(item) ->
				{
					val name = stack.unlocalizedName
					itemValues.filter { it.unlocalizedName == name }.sumBy(IItemStack::stackSize) + stackSize < filterBedCountValue.get()
				}

				provider.isItemBlock(item) && !provider.isBlockBush(item?.asItemBlock()?.block) && !provider.isBlockChest(item?.asItemBlock()?.block) -> itemValues.filter { provider.isItemBlock(it.item) && item?.asItemBlock()?.block?.let(::checkBlock) == true }.sumBy(IItemStack::stackSize) + stackSize <= filterMaxBlockCountValue.get()

				filterFoodCountValue.get() > 0 && provider.isItemFood(item) ->
				{
					val itemID = functions.getIdFromItem(item!!)
					itemValues.filter { provider.isItemFood(it.item) }.filter { functions.getIdFromItem(it.item!!) == itemID }.sumBy(IItemStack::stackSize) + stackSize <= filterFoodCountValue.get()
				}

				else -> filterDiamondValue.get() && stack.unlocalizedName == "item.diamond" // Diamond
					|| filterIronIngotValue.get() && stack.unlocalizedName == "item.ingotIron" // Iron
					|| filterPotionValue.get() && provider.isItemPotion(item) && AutoPot.isPotionUseful(stack) // Potion
					|| filterEnderPearlValue.get() && provider.isItemEnderPearl(item) // Ender Pearl
					|| provider.isItemEnchantedBook(item) // Enchanted Book
					|| filterBucketValue.get() && provider.isItemBucket(item) // Bucket
					|| stack.unlocalizedName == "item.stick" // Stick
					|| filterIgnoreVehiclesValue.get() && (provider.isItemBoat(item) || provider.isItemMinecart(item)) // Vehicles
					|| filterSkullValue.get() && provider.isItemSkull(item)
			}
		}
		catch (ex: Exception)
		{
			ClientUtils.logger.error("(InventoryCleaner) Failed to check item: ${stack.unlocalizedName}.", ex)

			true
		}
	}

	// TODO: update block filter
	private fun checkBlock(block: IBlock): Boolean = !classProvider.isBlockBush(block) && !classProvider.isBlockChest(block)

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
		val screen = mc.currentScreen

		(0..8).mapNotNull { it to (findBetterItem(thePlayer, it, thePlayer.inventory.getStackInSlot(it)) ?: return@mapNotNull null) }.firstOrNull { (index, bestItem) -> index != bestItem }?.let { (index, bestItem) ->
			val openInventory = !provider.isGuiInventory(screen) && simulateInventory.get()

			if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

			val slot = if (bestItem < 9) bestItem + 36 else bestItem
			mc.playerController.windowClick(0, slot, index, 2, thePlayer)

			if (clickIndicationEnabledValue.get() && screen != null && provider.isGuiContainer(screen))
			{
				screen.asGuiContainer().highlight(slot, clickIndicationLengthValue.get().toLong(), CLICKINDICATION_REPLACE_FROM)
				screen.asGuiContainer().highlight(index + 36, clickIndicationLengthValue.get().toLong(), CLICKINDICATION_REPLACE_TO)
			}

			if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

			delay = delayValue.getRandomDelay()
		}
	}

	private fun findBetterItem(thePlayer: IEntityPlayer, slot: Int, slotStack: IItemStack?): Int?
	{
		val type = type(slot).toLowerCase()

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
				val typecheck: ((IItem?) -> Boolean) = when (type)
				{
					"pickaxe" -> { item: IItem? -> provider.isItemPickaxe(item) }
					"axe" -> { item: IItem? -> provider.isItemAxe(item) }
					else -> { item: IItem? -> provider.isItemSword(item) }
				}

				var bestWeapon = if (typecheck(slotStack?.item)) slot else -1
				val getAttackDamage = { itemStack: IItemStack -> (itemStack.getAttributeModifier("generic.attackDamage").firstOrNull()?.amount ?: 0.0) + 1.25 * itemStack.getEnchantmentLevel(provider.getEnchantmentEnum(EnchantmentType.SHARPNESS)) }

				mainInventory.filter { typecheck(it.value?.item) }.map { it.index to it.value!! }.filter { !type(it.first).equals(type, ignoreCase = true) }.forEach { (index, stack) -> if (bestWeapon == -1) bestWeapon = index else if (getAttackDamage(thePlayer.inventory.getStackInSlot(bestWeapon) ?: return@forEach) < getAttackDamage(stack)) bestWeapon = index }

				return if (bestWeapon != -1 || bestWeapon == slot) bestWeapon else null
			}

			"bow" -> if (filterBowAndArrowBowCountValue.get() > 0)
			{
				var bestBow = if (provider.isItemBow(slotStack?.item)) slot else -1
				val powerEnch = provider.getEnchantmentEnum(EnchantmentType.POWER)
				var bestPower = if (bestBow != -1) slotStack.getEnchantmentLevel(powerEnch) else 0

				mainInventory.filter { provider.isItemBow(it.value?.item) }.map { it.index to it.value!! }.filter { !type(it.first).equals(type, ignoreCase = true) }.forEach { (index, stack) ->
					if (bestBow == -1) bestBow = index
					else
					{
						val power = stack.getEnchantmentLevel(powerEnch)
						if (power > bestPower)
						{
							bestBow = index
							bestPower = power
						}
					}
				}

				return if (bestBow != -1) bestBow else null
			}

			"food" -> if (filterFoodCountValue.get() > 0) mainInventory.filter { provider.isItemFood(it.value?.item) }.map { it.index to it.value!! }.filter { !provider.isItemAppleGold(it.second) }.filter { !type(it.first).equals("Food", ignoreCase = true) }.toList().forEach { (index, stack) -> return@findBetterItem if (stack.isEmpty || !provider.isItemFood(stack.item)) index else null }
			"block" -> mainInventory.filter { provider.isItemBlock(it.value?.item) }.mapNotNull { it.index to (it.value?.item?.asItemBlock() ?: return@mapNotNull null) }.filter { !BLACKLISTED_BLOCKS.contains(it.second.block) }.filter { !type(it.first).equals("Block", ignoreCase = true) }.forEach { (index, item) -> return@findBetterItem if (slotStack.isEmpty || !provider.isItemBlock(item)) index else null }

			"water" -> if (filterBucketValue.get())
			{
				val flowingWater = provider.getBlockEnum(BlockType.FLOWING_WATER)
				mainInventory.filter { provider.isItemBucket(it.value?.item) }.mapNotNull { it.index to (it.value?.item?.asItemBucket() ?: return@mapNotNull null) }.filter { it.second.isFull == flowingWater }.filter { !type(it.first).equals("Water", ignoreCase = true) }.toList().forEach { (index, item) -> return@findBetterItem if (slotStack.isEmpty || !provider.isItemBucket(item) || (item.asItemBucket()).isFull != flowingWater) index else null }
			}

			"gapple" -> if (filterFoodCountValue.get() > 0) mainInventory.filter { provider.isItemAppleGold(it.value?.item) }.filter { !type(it.index).equals("Gapple", ignoreCase = true) }.forEach { return@findBetterItem if (slotStack.isEmpty || !provider.isItemAppleGold(slotStack?.item)) it.index else null }
			"pearl" -> if (filterEnderPearlValue.get()) mainInventory.filter { provider.isItemEnderPearl(it.value?.item) }.filter { !type(it.index).equals("Pearl", ignoreCase = true) }.forEach { return@findBetterItem if (slotStack.isEmpty || !provider.isItemEnderPearl(slotStack?.item)) it.index else null }
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
		(end - 1 downTo start).filter { it !in 36..44 || !type(it).equals("Ignore", ignoreCase = true) }.mapNotNull { it to (container.getSlot(it).stack ?: return@mapNotNull null) }.filter { (_, stack) -> !stack.isEmpty && currentTime - stack.itemDelay >= itemDelay }.forEach { items[it.first] = it.second }

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
