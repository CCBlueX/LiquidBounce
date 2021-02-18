/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.IScaledResolution
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings
import net.ccbluex.liquidbounce.api.util.IWrappedArray
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.Maps
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.value.BoolValue
import org.lwjgl.opengl.GL11
import kotlin.math.*

@ModuleInfo(name = "ExtendedTooltips", description = "Display more tooltip informations on hotbar. (From Vanilla Enhancements mod)", category = ModuleCategory.RENDER)
class ExtendedTooltips : Module()
{
	private val attackDamage = BoolValue("AttackDamage", true)
	private val enchantments = BoolValue("Enchantments", true)
	private val heldItemCount = BoolValue("ArrowCount", true)
	private val armorPotential = BoolValue("ArmorPotential", true)
	private val durabilityWarning = BoolValue("DurabilityWarning", true)

	private var lastMessage = ""

	private val cooldown: Cooldown = Cooldown.getNewCooldownMiliseconds(300)

	private var showWarning = false
	private val durCooldown: Cooldown = Cooldown.getNewCooldownMiliseconds(1000)

	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val heldItemStack: IItemStack? = thePlayer.inventory.getCurrentItemInHand()

		val res = classProvider.createScaledResolution(mc)

		val scaledWidth = res.scaledWidth
		val scaledHeight = res.scaledHeight

		val fontRenderer = mc.fontRendererObj
		val fontHeight = fontRenderer.fontHeight

		if (heldItemStack != null)
		{
			if (attackDamage.get())
			{
				GL11.glPushMatrix()
				GL11.glScalef(0.5f, 0.5f, 0.5f)

				val attackDamage: String = getAttackDamageString(thePlayer, heldItemStack)

				val x: Int = scaledWidth - (fontRenderer.getStringWidth(attackDamage) shr 1)
				var y: Int = scaledHeight - 80
				y += if (mc.playerController.shouldDrawHUD()) -1 else 14
				y = y + fontHeight shl 0
				y = y shl 1
				y += fontHeight

				fontRenderer.drawString(attackDamage, x, y, 13421772)

				GL11.glScalef(2.0f, 2.0f, 2.0f)
				GL11.glPopMatrix()
			}

			if (enchantments.get())
			{
				val toDraw: String = if (classProvider.isItemPotion(heldItemStack.item)) getPotionEffectString(heldItemStack) else getEnchantmentString(heldItemStack)

				GL11.glPushMatrix()
				GL11.glScalef(0.5f, 0.5f, 0.5f)

				val x: Int = scaledWidth - (fontRenderer.getStringWidth(toDraw) shr 1)
				var y: Int = scaledHeight - 80
				y += if (mc.playerController.shouldDrawHUD()) -2 else 14
				y = y + fontHeight shl 0
				y = y shl 1

				fontRenderer.drawString(toDraw, x, y, 13421772)

				GL11.glScalef(2.0f, 2.0f, 2.0f)
				GL11.glPopMatrix()
			}
		}

		if (heldItemCount.get() && thePlayer.currentEquippedItem != null)
		{
			val isHoldingBow = classProvider.isItemBow(thePlayer.currentEquippedItem!!.item)
			val count = getHeldItemCount(thePlayer, isHoldingBow)

			if (count > 1 || isHoldingBow && count > 0)
			{
				val offset = if (mc.playerController.currentGameType == IWorldSettings.WGameType.CREATIVE) 10 else 0

				fontRenderer.drawString("$count", (scaledWidth - fontRenderer.getStringWidth("$count" + "") shr 1).toFloat(), (scaledHeight - 46 - offset).toFloat(), 16777215, true)
			}
		}

		if (armorPotential.get() && classProvider.isGuiInventory(mc.currentScreen))
		{
			val message = getAsString(thePlayer)

			lastMessage = message
			mc.currentScreen!!.drawString(fontRenderer, message, 10, scaledHeight - 16, 16777215)
		}

		if (durabilityWarning.get() && isDurabilityLow(thePlayer)) printArmorWarning(res, fontRenderer)

	}

	private fun getAttackDamageString(thePlayer: IEntityPlayerSP, stack: IItemStack): String
	{
		val itr: Iterator<String> = stack.getTooltip(thePlayer, true).iterator()
		var entry: String

		do
		{
			if (!itr.hasNext()) return ""

			entry = itr.next()
		} while (!entry.endsWith("Attack Damage"))

		return entry.split(" ", limit = 2)[0].substring(2)
	}

	private fun getPotionEffectString(heldItemStack: IItemStack): String
	{
		val potion = heldItemStack.item!!.asItemPotion()
		val effects: Iterable<IPotionEffect> = potion.getEffects(heldItemStack)

		return run {
			val potionBuilder = StringBuilder()
			val itr = effects.iterator()

			while (itr.hasNext())
			{
				val entry = itr.next()
				val duration: Int = entry.duration / 20
				potionBuilder.append("\u00A7l").append(functions.translateToLocal(entry.effectName)).append(" ").append(entry.amplifier + 1).append(" ").append("(").append("${duration / 60}${String.format(":%02d", duration % 60)}").append(") ")
			}

			"$potionBuilder".trim { it <= ' ' }
		}
	}

	private fun getEnchantmentString(heldItemStack: IItemStack): String
	{
		val enchantBuilder = StringBuilder()
		val enchantments: Map<Int, Int> = functions.getEnchantments(heldItemStack)
		val var4: Iterator<Map.Entry<*, *>> = enchantments.entries.iterator()

		while (var4.hasNext())
		{
			val entry = var4.next()
			enchantBuilder.append("\u00A7l").append(Maps.ENCHANTMENT_SHORT_NAME[entry.key] as String).append(" ").append(entry.value).append(" ")
		}

		return "$enchantBuilder".trim { it <= ' ' }
	}

	private fun getAsString(thePlayer: IEntityPlayerSP): String
	{
		return if (!cooldown.attemptReset()) lastMessage
		else
		{
			val ap = roundDecimals(getArmorPotential(thePlayer, false))
			val app = roundDecimals(getArmorPotential(thePlayer, true))
			(if (ap == app) "$ap%" else "$ap% | $app%").also { lastMessage = it }
		}
	}

	private fun roundDecimals(num: Double, a: Int = 2): Double
	{
		var fixedNum = num

		return if (fixedNum == 0.0) fixedNum
		else
		{
			fixedNum = (fixedNum * 10.0.pow(a.toDouble())).toInt().toDouble()
			fixedNum /= 10.0.pow(a.toDouble())
			fixedNum
		}
	}

	private fun getArmorPotential(thePlayer: IEntityPlayerSP, getProj: Boolean): Double
	{
		var armor = 0.0
		var epf = 0
		val resistance = thePlayer.getActivePotionEffect(classProvider.getPotionEnum(PotionType.RESISTANCE))?.amplifier?.plus(1) ?: 0

		val armorInv: IWrappedArray<IItemStack?> = thePlayer.inventory.armorInventory

		armorInv.forEach { itemStack ->
			if (itemStack != null)
			{
				if (classProvider.isItemArmor(itemStack.item))
				{
					val armorItem = itemStack.item!!.asItemArmor()
					armor += armorItem.damageReduceAmount.toDouble() * 0.04
				}

				if (itemStack.isItemEnchanted) epf += getEffProtPoints(functions.getEnchantmentLevel(0, itemStack))

				if (getProj && itemStack.isItemEnchanted) epf += getEffProtPoints(functions.getEnchantmentLevel(4, itemStack))
			}
		}

		epf = if (epf < 25) epf else 25

		val avgdef = addArmorProtResistance(armor, calcProtection(epf.toDouble()), resistance)
		return roundDouble(avgdef * 100.0)
	}

	private fun getEffProtPoints(level: Int, typeModifier: Double = 0.75): Int = if (level != 0) floor((6 + level * level).toDouble() * typeModifier / 3.0).toInt() else 0

	private fun calcProtection(armorEpf: Double): Double
	{
		val protection = (50..100).sumByDouble { if (ceil(armorEpf * it.toDouble() / 100.0) < 20.0) ceil(armorEpf * it.toDouble() / 100.0) else 20.0 }
		return protection / 51.0
	}

	private fun addArmorProtResistance(armor: Double, prot: Double, resi: Int): Double
	{
		var protTotal = armor + (1.0 - armor) * prot * 0.04
		protTotal += (1.0 - protTotal) * resi.toDouble() * 0.2
		return if (protTotal < 1.0) protTotal else 1.0
	}

	private fun roundDouble(number: Double): Double = (number * 10000.0).roundToLong().toDouble() / 10000.0

	private fun getHeldItemCount(thePlayer: IEntityPlayerSP, bow: Boolean): Int
	{
		var id: Int = functions.getIdFromItem(thePlayer.currentEquippedItem!!.item!!)
		var data: Int = thePlayer.currentEquippedItem!!.itemDamage

		if (bow)
		{
			id = functions.getIdFromItem(classProvider.getItemEnum(ItemType.ARROW))
			data = 0
		}

		var count = 0

		val inventory: IWrappedArray<IItemStack?> = thePlayer.inventory.mainInventory
		inventory.forEachIndexed { i, _ ->
			val itemInSlot = inventory[i]
			if (itemInSlot != null)
			{
				val item: IItem = itemInSlot.item!!
				if (functions.getIdFromItem(item) == id && itemInSlot.itemDamage == data) count += itemInSlot.stackSize
			}
		}

		return count
	}

	private fun printArmorWarning(resolution: IScaledResolution, fontRendererObj: IFontRenderer)
	{
		val text = "Armor durability is low!"
		val width: Int = resolution.scaledWidth - fontRendererObj.getStringWidth(text) - 1
		val height: Int = resolution.scaledHeight - 3 * fontRendererObj.fontHeight - 1

		fontRendererObj.drawString(text, width.toFloat(), height.toFloat(), 16724804, true)
	}

	private fun isDurabilityLow(thePlayer: IEntityPlayerSP): Boolean
	{
		return if (!durCooldown.attemptReset()) showWarning
		else
		{
			val armorInventory: IWrappedArray<IItemStack?> = thePlayer.inventory.armorInventory

			armorInventory.forEach { armor ->
				if (armor != null && (armor.itemDamage.toDouble() / armor.maxDamage.toDouble() > 0.85 || armor.maxDamage - armor.itemDamage < 15))
				{
					val id: Int = functions.getIdFromItem(armor.item!!)

					if (id in 298..317) return@isDurabilityLow true.also { showWarning = it }
				}
			}

			false.also { showWarning = it }
		}
	}
}
