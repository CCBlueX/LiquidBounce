/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
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
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11
import kotlin.math.*

@ModuleInfo(name = "ExtendedTooltips", description = "Display more tooltip informations on hotbar. (From Vanilla Enhancements mod)", category = ModuleCategory.RENDER)
class ExtendedTooltips : Module()
{
	/**
	 * Options
	 */
	private val attackDamage = BoolValue("AttackDamage", true)
	private val enchantments = BoolValue("Enchantments", true)
	private val itemDamageAndEnchantmentYPosValue = IntegerValue("AttackDamageAndEnchantYPos", 75, 50, 100)

	private val heldItemCount = BoolValue("HeldItemCount", true)
	private val heldItemCountYPosValue = IntegerValue("HeldItemCountYPos", 46, 20, 100)

	private val armorPotential = BoolValue("ArmorPotential", true)
	private val durabilityWarning = BoolValue("DurabilityWarning", true)

	/**
	 * Variables
	 */
	private var lastArmorPotential = ""

	private val armorPotentialCooldown: Cooldown = Cooldown.getNewCooldownMiliseconds(300)
	private val durabilityWarningCooldown: Cooldown = Cooldown.getNewCooldownMiliseconds(1000)

	private var showWarning = false

	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val heldItemStack: IItemStack? = thePlayer.inventory.getCurrentItemInHand()

		val resolution = classProvider.createScaledResolution(mc)

		val width = resolution.scaledWidth
		val height = resolution.scaledHeight

		val controller = mc.playerController

		val font = mc.fontRendererObj
		val fontHeight = font.fontHeight

		val dmgAndEnchYPos = itemDamageAndEnchantmentYPosValue.get()
		val heldItemCountYPos = heldItemCountYPosValue.get()

		if (heldItemStack != null)
		{
			if (attackDamage.get())
			{
				GL11.glPushMatrix()
				GL11.glScalef(0.5f, 0.5f, 0.5f)

				val attackDamage: String = getAttackDamageString(thePlayer, heldItemStack)

				var y: Int = height - dmgAndEnchYPos
				y += if (controller.shouldDrawHUD()) -1 else 14
				y = y + fontHeight shl 0
				y = y shl 1
				y += fontHeight

				font.drawString(attackDamage, width - (font.getStringWidth(attackDamage) shr 1), y, 13421772)

				GL11.glScalef(2.0f, 2.0f, 2.0f)
				GL11.glPopMatrix()
			}

			if (enchantments.get())
			{
				val toDraw: String = if (classProvider.isItemPotion(heldItemStack.item)) getPotionEffectString(heldItemStack) else getEnchantmentString(heldItemStack)

				GL11.glPushMatrix()
				GL11.glScalef(0.5f, 0.5f, 0.5f)

				var y: Int = height - dmgAndEnchYPos
				y += if (controller.shouldDrawHUD()) -2 else 14
				y = y + fontHeight shl 0
				y = y shl 1

				font.drawString(toDraw, width - (font.getStringWidth(toDraw) shr 1), y, 13421772)

				GL11.glScalef(2.0f, 2.0f, 2.0f)
				GL11.glPopMatrix()
			}
		}

		if (heldItemCount.get() && thePlayer.currentEquippedItem != null)
		{
			val isHoldingBow = classProvider.isItemBow(thePlayer.currentEquippedItem!!.item)
			val count = getHeldItemCount(thePlayer, isHoldingBow)

			if (count > 1 || isHoldingBow && count > 0) font.drawString("$count", (width - font.getStringWidth("$count" + "") shr 1).toFloat(), (height - heldItemCountYPos - if (controller.currentGameType == IWorldSettings.WGameType.CREATIVE) 10 else 0).toFloat(), 16777215, true)
		}

		val screen = mc.currentScreen
		if (armorPotential.get() && classProvider.isGuiInventory(screen)) screen!!.drawString(font, getArmorPotential(thePlayer).also { lastArmorPotential = it }, 10, height - 16, 16777215)

		if (durabilityWarning.get() && isArmorDurabilityLow(thePlayer)) printArmorWarning(resolution, font)
	}

	private fun getAttackDamageString(thePlayer: IEntityPlayerSP, stack: IItemStack): String
	{
		val tooltipIterator: Iterator<String> = stack.getTooltip(thePlayer, true).iterator()
		var attackDamageEntry: String

		do
		{
			if (!tooltipIterator.hasNext()) return ""

			attackDamageEntry = tooltipIterator.next()
		} while (!attackDamageEntry.endsWith("Attack Damage"))

		return attackDamageEntry.split(" ", limit = 2)[0].substring(2)
	}

	private fun getPotionEffectString(itemStack: IItemStack): String
	{
		val potionBuilder = StringBuilder()

		itemStack.item!!.asItemPotion().getEffects(itemStack).forEach { effect ->
			val durationInSeconds: Int = effect.duration / 20
			potionBuilder.append("\u00A7l${Maps.POTION_SHORT_NAME[effect.potionID]}*${effect.amplifier + 1}(${durationInSeconds / 60}${String.format(":%02d", durationInSeconds % 60)}) ")
		}

		return "$potionBuilder".trim { it <= ' ' }
	}

	private fun getEnchantmentString(itemStack: IItemStack): String
	{
		val enchantBuilder = StringBuilder()

		functions.getEnchantments(itemStack).forEach { (enchID, amplifier) -> enchantBuilder.append("\u00A7l${Maps.ENCHANTMENT_SHORT_NAME[enchID]}*$amplifier ") }

		return "$enchantBuilder".trim { it <= ' ' }
	}

	private fun getArmorPotential(thePlayer: IEntityPlayerSP): String
	{
		return if (!armorPotentialCooldown.attemptReset()) lastArmorPotential
		else
		{
			val ap = roundDecimals(getArmorPotential(thePlayer, false))
			val app = roundDecimals(getArmorPotential(thePlayer, true))
			(if (ap == app) "$ap%" else "$ap% | $app%").also { lastArmorPotential = it }
		}
	}

	private fun getArmorPotential(thePlayer: IEntityPlayerSP, projectileProtection: Boolean): Double
	{
		var armor = 0.0
		var epf = 0

		thePlayer.inventory.armorInventory.forEach { itemStack ->
			if (itemStack != null)
			{
				if (classProvider.isItemArmor(itemStack.item)) armor += itemStack.item!!.asItemArmor().damageReduceAmount.toDouble() * 0.04

				if (itemStack.isItemEnchanted) epf += getEffProtPoints(functions.getEnchantmentLevel(0, itemStack))

				if (projectileProtection && itemStack.isItemEnchanted) epf += getEffProtPoints(functions.getEnchantmentLevel(4, itemStack))
			}
		}

		return roundDouble(addArmorProtResistance(armor, calcProtection(epf.coerceAtMost(25).toDouble()), thePlayer.getActivePotionEffect(classProvider.getPotionEnum(PotionType.RESISTANCE))?.amplifier?.plus(1) ?: 0) * 100.0)
	}

	private fun getEffProtPoints(level: Int, typeModifier: Double = 0.75): Int = if (level != 0) floor((6 + level * level).toDouble() * typeModifier / 3.0).toInt() else 0

	private fun calcProtection(armorEpf: Double): Double = (50..100).sumByDouble { if (ceil(armorEpf * it.toDouble() * 0.01) < 20.0) ceil(armorEpf * it.toDouble() * 0.01) else 20.0 } / 51.0

	private fun addArmorProtResistance(armor: Double, protection: Double, resistanceAmplifier: Int): Double
	{
		var protTotal = armor + (1.0 - armor) * protection * 0.04
		protTotal += (1.0 - protTotal) * resistanceAmplifier.toDouble() * 0.2
		return protTotal.coerceAtMost(1.0)
	}

	private fun roundDecimals(num: Double, places: Int = 2): Double = if (num == 0.0) num else (num * 10.0.pow(places.toDouble())).toInt().toDouble() / 10.0.pow(places.toDouble())

	private fun roundDouble(number: Double): Double = (number * 10000.0).roundToLong().toDouble() * 0.0001f

	private fun getHeldItemCount(thePlayer: IEntityPlayerSP, bow: Boolean): Int
	{
		var itemID: Int = functions.getIdFromItem(thePlayer.currentEquippedItem!!.item!!)
		var itemMeta: Int = thePlayer.currentEquippedItem!!.itemDamage

		if (bow)
		{
			itemID = functions.getIdFromItem(classProvider.getItemEnum(ItemType.ARROW))
			itemMeta = 0
		}

		var totalItemCount = 0

		val inventory: IWrappedArray<IItemStack?> = thePlayer.inventory.mainInventory
		inventory.forEachIndexed { i, _ ->
			val itemInSlot = inventory[i]
			if (itemInSlot != null && functions.getIdFromItem(itemInSlot.item!!) == itemID && itemInSlot.itemDamage == itemMeta) totalItemCount += itemInSlot.stackSize
		}

		return totalItemCount
	}

	private fun printArmorWarning(resolution: IScaledResolution, font: IFontRenderer)
	{
		val text = "ARMOR DURABILITY IS LOW!"
		font.drawString(text, resolution.scaledWidth - font.getStringWidth(text) - 1f, resolution.scaledHeight - 3 * font.fontHeight - 1f, 16724804, true)
	}

	private fun isArmorDurabilityLow(thePlayer: IEntityPlayerSP): Boolean
	{
		return if (!durabilityWarningCooldown.attemptReset()) showWarning
		else
		{
			thePlayer.inventory.armorInventory.forEach { armor ->
				if (armor != null && (armor.itemDamage.toDouble() / armor.maxDamage.toDouble() > 0.85 || armor.maxDamage - armor.itemDamage < 15))
				{
					val id: Int = functions.getIdFromItem(armor.item!!)

					if (id in 298..317) return@isArmorDurabilityLow true.also { showWarning = it }
				}
			}

			false.also { showWarning = it }
		}
	}
}
