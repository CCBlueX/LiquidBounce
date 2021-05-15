/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.IClassProvider
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.FastUse
import net.ccbluex.liquidbounce.features.module.modules.player.Zoot
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import kotlin.math.abs
import kotlin.random.Random

@ModuleInfo(name = "AutoUse", description = "Automatically eat/drink foods/potions in your hotbar)", category = ModuleCategory.COMBAT)
class AutoUse : Module()
{
	private val foodValue = BoolValue("Food", true)
	private val foodLevelValue = IntegerValue("FoodLevel", 18, 1, 20)

	private val potionValue = BoolValue("Potion", true)

	private val gappleValue = BoolValue("Gapple", true)
	private val gappleHealthValue = FloatValue("Gapple-Health", 12F, 1F, 20F)

	private val milkValue = BoolValue("Milk", true)

	private val silentValue = BoolValue("Silent", false)

	private val offset = IntegerValue("Offset", 1, 0, 10)

	private val maxDelayValue: IntegerValue = object : IntegerValue("MaxUseDelay", 2000, 0, 5000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minDelayValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minDelayValue: IntegerValue = object : IntegerValue("MinUseDelay", 2000, 0, 5000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxDelayValue.get()
			if (i < newValue) this.set(i)
		}
	}

	private val maxInvDelayValue: IntegerValue = object : IntegerValue("MaxInvDelay", 200, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = minInvDelayValue.get()
			if (i > newValue) this.set(i)
		}
	}

	private val minInvDelayValue: IntegerValue = object : IntegerValue("MinInvDelay", 100, 0, 1000)
	{
		override fun onChanged(oldValue: Int, newValue: Int)
		{
			val i = maxInvDelayValue.get()
			if (i < newValue) this.set(i)
		}
	}
	private val openInventoryValue = BoolValue("OpenInv", false)
	private val simulateInventoryValue = BoolValue("SimulateInventory", true)
	private val noMoveValue = BoolValue("NoMove", false)
	private val randomSlotValue = BoolValue("RandomSlot", false)
	private val misClickValue = BoolValue("ClickMistakes", false)
	private val misClickRateValue = IntegerValue("ClickMistakeRate", 5, 0, 100)
	private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

	private val glassBottleValue = ListValue("GlassBottle", arrayOf("Drop", "Move", "Stay"), "Drop")

	private val ignoreScreen = BoolValue("IgnoreScreen", true)

	private val useDelayTimer = MSTimer()
	private var useDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

	private var invDelay = TimeUtils.randomDelay(minInvDelayValue.get(), maxInvDelayValue.get())

	private var slotToUse = -1

	private var lastRequiredTicks: Int? = null
	private var waitedTicks = -1

	override fun onDisable()
	{
		endEating(mc.thePlayer ?: return, glassBottleValue.get(), classProvider, mc.netHandler, silentValue.get())

		slotToUse = -1
		waitedTicks = -1
	}

	@EventTarget
	fun onMotion(motionEvent: MotionEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val netHandler = mc.netHandler

		val controller = mc.playerController
		val screen = mc.currentScreen

		val openContainer = thePlayer.openContainer
		val inventory = thePlayer.inventory
		val inventoryContainer = thePlayer.inventoryContainer
		val activePotionEffects = thePlayer.activePotionEffects
		val health = thePlayer.health
		val foodLevel = thePlayer.foodStats.foodLevel

		val provider = classProvider

		val food = foodValue.get()
		val potion = potionValue.get()
		val gapple = gappleValue.get()
		val milk = milkValue.get()

		val gappleHealth = gappleHealthValue.get()

		val itemDelay = itemDelayValue.get().toLong()
		val random = randomSlotValue.get()
		val handleGlassBottle = glassBottleValue.get()
		val silent = silentValue.get()

		when (motionEvent.eventState)
		{
			EventState.PRE ->
			{
				if (useDelayTimer.hasTimePassed(useDelay) && (ignoreScreen.get() || provider.isGuiContainer(screen)))
				{
					val foodInHotbar = if (food && foodLevel <= foodLevelValue.get()) findBestFood(thePlayer) else -1
					val potionInHotbar = if (potion) findPotion(activePotionEffects, inventoryContainer, random = random) else -1
					val gappleInHotbar = if (gapple && if (gappleHealth < 20) health <= gappleHealth else thePlayer.absorptionAmount <= 0) InventoryUtils.findItem(inventoryContainer, 36, 45, provider.getItemEnum(ItemType.GOLDEN_APPLE), itemDelay, random) else -1
					val milkInHotbar = if (milk && activePotionEffects.map(IPotionEffect::potionID).any(Zoot.badEffectsArray::contains)) InventoryUtils.findItem(inventoryContainer, 36, 45, provider.getItemEnum(ItemType.MILK_BUCKET), itemDelay, random) else -1

					val slot = arrayOf(foodInHotbar, potionInHotbar, gappleInHotbar, milkInHotbar).firstOrNull { it != -1 }

					if (slot != null)
					{
						slotToUse = slot

						val slotIndex = slotToUse - 36

						if (!silent) inventory.currentItem = slotIndex

						val isFirst = waitedTicks <= 0

						if (isFirst) if (silent) netHandler.addToSendQueue(provider.createCPacketHeldItemChange(slotIndex)) else mc.playerController.updateController()

						val stack = inventoryContainer.getSlot(slotToUse).stack

						if (isFirst) netHandler.addToSendQueue(createUseItemPacket(stack, WEnumHand.MAIN_HAND))

						if (silent)
						{
							val itemUseTicks = if (isFirst) 0 else (lastRequiredTicks?.minus(waitedTicks)) ?: 0
							lastRequiredTicks = performFastUse(thePlayer, stack?.item, itemUseTicks) + offset.get()

							if (isFirst) waitedTicks = lastRequiredTicks!!
						}
						else
						{
							lastRequiredTicks = 32

							mc.gameSettings.keyBindUseItem.pressed = true // FIXME: Change to better solution

							if (isFirst) waitedTicks = lastRequiredTicks!!
						}

						return
					}
				}

				if (InventoryUtils.CLICK_TIMER.hasTimePassed(invDelay) && !(noMoveValue.get() && MovementUtils.isMoving(thePlayer)) && !(openContainer != null && openContainer.windowId != 0))
				{
					val glassBottle = provider.getItemEnum(ItemType.GLASS_BOTTLE)

					// Move empty glass bottles to inventory
					val glassBottleInHotbar = InventoryUtils.findItem(inventoryContainer, 36, 45, glassBottle, itemDelay, random)

					val isGuiInventory = provider.isGuiInventory(screen)
					val simulateInv = simulateInventoryValue.get()

					if (handleGlassBottle.equals("Move", true) && glassBottleInHotbar != -1)
					{
						if (openInventoryValue.get() && !isGuiInventory) return

						if ((9..36).map(inventory::getStackInSlot).any { it == null || it.item == glassBottle && it.stackSize < 16 })
						{
							val openInventory = !isGuiInventory && simulateInv

							if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

							controller.windowClick(0, glassBottleInHotbar, 0, 1, thePlayer)

							invDelay = TimeUtils.randomDelay(minInvDelayValue.get(), maxInvDelayValue.get())
							InventoryUtils.CLICK_TIMER.reset()

							return
						}
					}

					// Move foods to hotbar
					val foodInInventory = if (food && foodLevel <= foodLevelValue.get()) findBestFood(thePlayer, startSlot = 9, endSlot = 36) else -1
					val potionInInventory = if (potion) findPotion(activePotionEffects, inventoryContainer, startSlot = 9, endSlot = 36, random = random) else -1
					val gappleInInventory = if (gapple && if (gappleHealth < 20) health <= gappleHealth else thePlayer.absorptionAmount <= 0) InventoryUtils.findItem(inventoryContainer, 9, 36, provider.getItemEnum(ItemType.GOLDEN_APPLE), itemDelay, random) else -1
					val milkInInventory = if (milk && activePotionEffects.map(IPotionEffect::potionID).any(Zoot.badEffectsArray::contains)) InventoryUtils.findItem(inventoryContainer, 9, 36, provider.getItemEnum(ItemType.MILK_BUCKET), itemDelay, random) else -1

					var slot = arrayOf(foodInInventory, potionInInventory, gappleInInventory, milkInInventory).firstOrNull { it != -1 }

					if (slot != null && InventoryUtils.hasSpaceHotbar(inventory))
					{
						// OpenInventory Check
						if (openInventoryValue.get() && !isGuiInventory) return

						// Simulate Click Mistakes to bypass some anti-cheats
						if (misClickValue.get() && misClickRateValue.get() > 0 && Random.nextInt(100) <= misClickRateValue.get())
						{
							val firstEmpty = InventoryUtils.firstEmpty(inventoryContainer, 9, 36, random)
							if (firstEmpty != -1) slot = firstEmpty
						}

						val openInventory = !isGuiInventory && simulateInv
						if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

						controller.windowClick(0, slot, 0, 1, thePlayer)

						if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

						invDelay = TimeUtils.randomDelay(minInvDelayValue.get(), maxInvDelayValue.get())
						InventoryUtils.CLICK_TIMER.reset()
					}
				}
			}

			EventState.POST -> if (slotToUse >= 0)
			{
				waitedTicks = (waitedTicks - 1).coerceAtLeast(-1)
				if (waitedTicks <= 0) endEating(thePlayer, handleGlassBottle, provider, netHandler, silent)
			}
		}
	}

	private fun endEating(thePlayer: IEntityPlayerSP, handleGlassBottle: String, provider: IClassProvider, netHandler: IINetHandlerPlayClient, silent: Boolean)
	{
		val itemStack = thePlayer.inventoryContainer.getSlot(slotToUse).stack

		if (itemStack != null)
		{
			if (handleGlassBottle.equals("Drop", true) && (provider.isItemGlassBottle(itemStack.item) || provider.isItemPotion(itemStack.item) && !itemStack.isSplash())) netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.DROP_ITEM, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))

			if (silent) netHandler.addToSendQueue(provider.createCPacketHeldItemChange(thePlayer.inventory.currentItem)) else mc.gameSettings.keyBindUseItem.unpressKey()

			useDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
			useDelayTimer.reset()

			slotToUse = -1
		}
	}

	private fun performFastUse(thePlayer: IEntityPlayerSP, item: IItem?, itemUseTicks: Int): Int = (LiquidBounce.moduleManager[FastUse::class.java] as FastUse).perform(thePlayer, mc.timer, item, itemUseTicks)

	private fun findPotion(activePotionEffects: Collection<IPotionEffect>, inventoryContainer: IContainer, startSlot: Int = 36, endSlot: Int = 45, random: Boolean): Int = (LiquidBounce.moduleManager[AutoPot::class.java] as AutoPot).findBuffPotion(activePotionEffects, startSlot, endSlot, inventoryContainer, random, false) // TODO: findHealPotion() support

	private fun findBestFood(thePlayer: IEntityPlayerSP, startSlot: Int = 36, endSlot: Int = 45): Int
	{
		val inventoryContainer = thePlayer.inventoryContainer
		val currentFoodLevel = thePlayer.foodStats.foodLevel
		return (startSlot until endSlot).mapNotNull { it to (inventoryContainer.getSlot(it).stack ?: return@mapNotNull null) }.filter { classProvider.isItemFood(it.second.item) }.maxBy { (_, stack) ->
			val foodStack = stack.item!!.asItemFood()
			20 - abs(currentFoodLevel - foodStack.getHealAmount(stack)) + foodStack.getSaturationModifier(stack)
		}?.first ?: -1
	}

	override val tag: String?
		get() = if (silentValue.get()) "Silent" else null
}
