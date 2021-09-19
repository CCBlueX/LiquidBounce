package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.api.enums.EnchantmentType
import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.item.ItemUtils.getEnchantment
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game   Minecraft
 */
@ModuleInfo(name = "AutoEnchant", description = "Automatically enchant your weapon when you opened anvil.", category = ModuleCategory.PLAYER)
class AutoEnchant : Module()
{
	val delayValue = IntegerRangeValue("Delay", 100, 100, 0, 500)
	private val firstDelayValue = IntegerRangeValue("FirstDelay", 500, 500, 0, 1000)
	private val enchantSwordBetweenEm = BoolValue("SwordToSword", true)

	private var delay = firstDelayValue.getRandomDelay()
	private val delayTimer = MSTimer()

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		val currentScreen = mc.currentScreen ?: return

		if (classProvider.isGuiRepair(currentScreen))
		{
			val guiRepair = currentScreen.asGuiRepair()
			val container = guiRepair.inventorySlots ?: return
			if (!delayTimer.hasTimePassed(delay)) return

			// Find a item for enchant. If there is no items for enchant found, close the gui automatically if option is present.
			// Map<Integer, ItemStack> items = findbests

			// Find a best sword
			val triedEnchantSwordSlots: MutableList<Int> = ArrayList()

			// For Anvil Inventory
			val item1Slot = container.getSlot(0)
			val item2Slot = container.getSlot(1)
			val outputSlot = container.getSlot(2)
			if (outputSlot.stack != null) // I'm sure there is no anticheat detecting autoenchant with output-slot item spoofing.
			{
				mc.playerController.windowClick(container.windowId, outputSlot.slotNumber, 0, 1, thePlayer)
				delay = delayValue.getRandomDelay()
				delayTimer.reset()
				return
			}
			val bestSwordSlot = findBestSword(container, 3, 39, null)

			// Enchant the best sword with Enchanted Book(s)
			@Suppress("UNUSED_PARAMETER")
			val bestEnchantedBookSlots = findBestEnchantedBooks(container, 3, 39, listOf(classProvider.getEnchantmentEnum(EnchantmentType.SHARPNESS), classProvider.getEnchantmentEnum(EnchantmentType.KNOCKBACK), classProvider.getEnchantmentEnum(EnchantmentType.FIRE_ASPECT), classProvider.getEnchantmentEnum(EnchantmentType.UNBREAKING)))
			if (bestEnchantedBookSlots.isNotEmpty())
			{
				if (item1Slot.stack == null && bestSwordSlot != -1)
				{
					mc.playerController.windowClick(container.windowId, bestSwordSlot, 0, 1, thePlayer)
					delay = delayValue.getRandomDelay()
					delayTimer.reset()
				}
				else if (item2Slot.stack == null)
				{
					mc.playerController.windowClick(container.windowId, bestEnchantedBookSlots[0], 0, 1, thePlayer)
					delay = delayValue.getRandomDelay()
					delayTimer.reset()
				}
			}
			else if (enchantSwordBetweenEm.get())
			{
				// Find second sword
				var secondSwordSlot = -1
				if (bestSwordSlot != -1)
				{
					if (item1Slot.stack == null) triedEnchantSwordSlots.add(bestSwordSlot) else triedEnchantSwordSlots.add(item1Slot.slotNumber)
					triedEnchantSwordSlots.add(outputSlot.slotNumber)
					do
					{
						secondSwordSlot = findBestSword(container, 0, 39, triedEnchantSwordSlots)
						triedEnchantSwordSlots.add(secondSwordSlot)
					} while (secondSwordSlot != -1 && container.getSlot(bestSwordSlot).stack != null && container.getSlot(secondSwordSlot).stack != null && (!container.getSlot(secondSwordSlot).stack?.item?.unlocalizedName.equals(container.getSlot(bestSwordSlot).stack?.item?.unlocalizedName, ignoreCase = true) || container.getSlot(secondSwordSlot).stack?.isItemEnchanted != true))
				}

				// Enchant swords between 'em
				if (item1Slot.stack == null)
				{
					if (bestSwordSlot != -1 && secondSwordSlot != -1)
					{
						mc.playerController.windowClick(container.windowId, bestSwordSlot, 0, 1, thePlayer)
						delay = delayValue.getRandomDelay()
						delayTimer.reset()
					}
				}
				else if (item2Slot.stack == null && secondSwordSlot != -1)
				{
					mc.playerController.windowClick(container.windowId, secondSwordSlot, 0, 1, thePlayer)
					delay = delayValue.getRandomDelay()
					delayTimer.reset()
				}
			}
		}
		else
		{
			delay = firstDelayValue.getRandomDelay()
			delayTimer.reset()
		}
	}

	private fun findBestSword(container: IContainer, start: Int, end: Int, blacklist: List<Int>?): Int
	{
		var slot = -1
		var bestDamage = 0.0
		for (i in start until end)
		{
			val itemStack = container.getSlot(i).stack
			if (blacklist != null && blacklist.contains(i) || itemStack == null || itemStack.item == null) continue
			if (classProvider.isItemSword(itemStack.item) || classProvider.isItemTool(itemStack.item)) for (attributeModifier in itemStack.getAttributeModifier("generic.attackDamage"))
			{
				val damage = attributeModifier.amount + 1.25 * getEnchantment(itemStack, classProvider.getEnchantmentEnum(EnchantmentType.SHARPNESS))
				if (damage > bestDamage)
				{
					bestDamage = damage
					slot = i
				}
			}
		}
		return slot
	}

	private fun findBestEnchantedBooks(container: IContainer, start: Int, end: Int, enchantments: Collection<IEnchantment>): List<Int>
	{
		val bestSlots: MutableList<Int> = ArrayList()
		for (currentEnch in enchantments)
		{
			// For each specified enchantments
			var bestSlot = -1
			var maxLevel = 0
			for (i in start until end)
			{
				// Search inventory for find enchantment book that have specified enchantment
				val itemStack = container.getSlot(i).stack
				if (itemStack?.item?.let { !classProvider.isItemEnchantedBook(it) } != false) continue

				val enchantMap = functions.getEnchantments(itemStack)
				enchantMap.forEach { (id, lvl) ->
					if (currentEnch.effectId == id)
					{
						if (lvl > maxLevel)
						{
							maxLevel = lvl
							bestSlot = i
						}
					}
				}
			}
			if (bestSlot != -1 && !bestSlots.contains(bestSlot)) bestSlots.add(bestSlot)
		}
		return bestSlots
	}
}
