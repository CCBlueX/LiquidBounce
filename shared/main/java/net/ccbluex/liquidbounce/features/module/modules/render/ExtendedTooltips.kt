/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.api.minecraft.util.IScaledResolution
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.Maps
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ValueGroup
import org.lwjgl.opengl.GL11
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong

@ModuleInfo(name = "ExtendedTooltips", description = "Display more tooltip informations on hotbar. (From Vanilla Enhancements mod)", category = ModuleCategory.RENDER)
class ExtendedTooltips : Module()
{
	private val attackDamageGroup = ValueGroup("AttackDamage")
	private val attackDamageEnabledValue = BoolValue("Enabled", true, "AttackDamage")
	private val attackDamageBackgroundEnabledValue = BoolValue("Background", false)
	private val attackDamageShadowValue = BoolValue("Shadow", false, "AttackDamageShadow")
	private val attackDamageScaleValue = FloatValue("Scale", 0.5F, 0.5F, 1F, "AttackDamageScale")

	private val enchantmentsGroup = ValueGroup("Enchantments")
	private val enchantmentsEnabledValue = BoolValue("Enabled", true, "Enchantments")
	private val enchantmentsBackgroundEnabledValue = BoolValue("Background", false)
	private val enchantmentsShadowValue = BoolValue("Shadow", false, "EnchantmentsShadow")
	private val enchantmentsScaleValue = FloatValue("Scale", 0.5F, 0.5F, 1F, "EnchantmentsScale")

	private val itemDamageAndEnchantmentYPosValue = object : IntegerValue("AttackDamageAndEnchantYPos", 75, 50, 100)
	{
		override fun showCondition() = attackDamageEnabledValue.get() || enchantmentsEnabledValue.get()
	}

	private val heldItemCountGroup = ValueGroup("HeldItemCount")
	private val heldItemCountEnabledValue = BoolValue("Enabled", true, "HeldItemCount")
	private val heldItemCountBackgroundEnabledValue = BoolValue("Background", false)
	private val heldItemCountShadowValue = BoolValue("Shadow", true, "HeldItemCountShadow")
	private val heldItemCountScaleValue = FloatValue("Scale", 1F, 0.5F, 1F, "HeldItemCountScale")
	private val heldItemCountYPosValue = IntegerValue("YPos", 46, 20, 100, "HeldItemCountYPos")

	private val armorPotential = BoolValue("ArmorPotential", true)
	private val durabilityWarning = BoolValue("DurabilityWarning", true)

	/**
	 * Variables
	 */
	private var lastArmorPotential = ""

	private val armorPotentialCooldown: Cooldown = Cooldown.getNewCooldownMiliseconds(300)
	private val armorDurabilityWarningCooldown: Cooldown = Cooldown.getNewCooldownMiliseconds(1000)

	private var showArmorDurabilityWarning = false

	init
	{
		attackDamageGroup.addAll(attackDamageEnabledValue, attackDamageBackgroundEnabledValue, attackDamageShadowValue, attackDamageScaleValue)
		enchantmentsGroup.addAll(enchantmentsEnabledValue, enchantmentsBackgroundEnabledValue, enchantmentsShadowValue, enchantmentsScaleValue)
		heldItemCountGroup.addAll(heldItemCountEnabledValue, heldItemCountBackgroundEnabledValue, heldItemCountShadowValue, heldItemCountScaleValue, heldItemCountYPosValue)
	}

	@EventTarget
	fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
	{
		val thePlayer = mc.thePlayer ?: return

		val heldItemStack: IItemStack? = thePlayer.inventory.getCurrentItemInHand()

		val provider = classProvider

		val resolution = provider.createScaledResolution(mc)

		val width = resolution.scaledWidth
		val height = resolution.scaledHeight

		val controller = mc.playerController

		val font = mc.fontRendererObj
		val fontHeight = font.fontHeight

		val dmgAndEnchYPos = itemDamageAndEnchantmentYPosValue.get()
		val heldItemCountYPos = heldItemCountYPosValue.get()

		val calcXPos = { string: String, scale: Float, reverseScale: Float ->
			val stringWidth = font.getStringWidth(string) shr 1
			(width - stringWidth * scale) * reverseScale * 0.5f - (stringWidth shr 1)
		}

		val isSurvivalOrAdventure = controller.shouldDrawHUD()

		if (heldItemStack != null)
		{
			if (attackDamageEnabledValue.get())
			{
				val toDraw: String = getAttackDamageString(thePlayer, heldItemStack)
				if (toDraw.isNotBlank())
				{
					val scale = attackDamageScaleValue.get()
					val reverseScale = 1 / scale

					GL11.glPushMatrix()

					val x = calcXPos(toDraw, scale, reverseScale)
					val y = (height - dmgAndEnchYPos + (if (isSurvivalOrAdventure) -1 else 14) + fontHeight).toFloat()

					if (attackDamageBackgroundEnabledValue.get()) RenderUtils.drawBorderedRect(x * scale - 2f, y, (x + font.getStringWidth(toDraw)) * scale + 2f, y + font.fontHeight * scale, 3f, -16777216, -16777216)

					if (scale != 1.0F) GL11.glScalef(scale, scale, scale)

					font.drawString(toDraw, x, y * reverseScale, 13421772, attackDamageShadowValue.get())

					if (scale != 1.0F) GL11.glScalef(reverseScale, reverseScale, reverseScale)

					GL11.glPopMatrix()
				}
			}

			if (enchantmentsEnabledValue.get())
			{
				val toDraw: String = if (provider.isItemPotion(heldItemStack.item)) getPotionEffectString(heldItemStack) else getEnchantmentString(heldItemStack)
				if (toDraw.isNotBlank())
				{
					val scale = enchantmentsScaleValue.get()
					val reverseScale = 1 / scale

					GL11.glPushMatrix()

					val x = calcXPos(toDraw, scale, reverseScale)
					val y = (height - dmgAndEnchYPos + if (isSurvivalOrAdventure) -2 else 14).toFloat()

					if (enchantmentsBackgroundEnabledValue.get()) RenderUtils.drawBorderedRect(x * scale - 2f, y, (x + font.getStringWidth(toDraw)) * scale + 2f, y + font.fontHeight * scale, 3f, -16777216, -16777216)

					if (scale != 1.0F) GL11.glScalef(scale, scale, scale)

					font.drawString(toDraw, x, y * reverseScale, 13421772, enchantmentsShadowValue.get())

					if (scale != 1.0F) GL11.glScalef(reverseScale, reverseScale, reverseScale)

					GL11.glPopMatrix()
				}
			}
		}

		val currentEquippedItem = thePlayer.currentEquippedItem

		if (heldItemCountEnabledValue.get() && currentEquippedItem != null)
		{
			val scale = heldItemCountScaleValue.get()
			val reverseScale = 1 / scale

			val bow = provider.isItemBow(currentEquippedItem.item)

			val count = getHeldItemCount(thePlayer, currentEquippedItem, bow)
			if (count > 1 || bow && count > 0)
			{
				val toDraw = "$count"

				GL11.glPushMatrix()

				val x = calcXPos(toDraw, scale, reverseScale)
				val y = (height - heldItemCountYPos - if (isSurvivalOrAdventure) 10 else 0).toFloat()

				if (heldItemCountBackgroundEnabledValue.get()) RenderUtils.drawBorderedRect(x * scale - 2f, y, (x + font.getStringWidth(toDraw)) * scale + 2f, y + font.fontHeight * scale, 3f, -16777216, -16777216)

				if (scale != 1.0F) GL11.glScalef(scale, scale, scale)

				font.drawString(toDraw, x, y * reverseScale, 16777215, heldItemCountShadowValue.get())

				if (scale != 1.0F) GL11.glScalef(reverseScale, reverseScale, reverseScale)

				GL11.glPopMatrix()
			}
		}

		val screen = mc.currentScreen
		if (armorPotential.get() && screen != null && provider.isGuiInventory(screen)) screen.drawString(font, getArmorPotential(thePlayer).also { lastArmorPotential = it }, 10, height - 16, 16777215)

		if (durabilityWarning.get() && isArmorDurabilityLow(thePlayer)) printArmorWarning(resolution, font)
	}

	private fun getAttackDamageString(thePlayer: IEntityPlayer, stack: IItemStack): String
	{
		val tooltipIterator: Iterator<String> = stack.getTooltip(thePlayer, true).iterator()
		var attackDamageEntry: String

		do
		{
			if (!tooltipIterator.hasNext()) return ""

			attackDamageEntry = tooltipIterator.next()
		} while (!attackDamageEntry.endsWith("Attack Damage"))

		return "\u00A7l${attackDamageEntry.split(" ", limit = 2)[0].substring(2)}"
	}

	private fun getPotionEffectString(potionItem: IItemStack): String
	{
		val potionBuilder = StringBuilder()

		potionItem.item?.asItemPotion()?.getEffects(potionItem)?.forEach { effect ->
			val durationInSeconds: Int = effect.duration / 20
			potionBuilder.append("\u00A7l${Maps.POTION_SHORT_NAME[effect.potionID]} ${effect.amplifier + 1} (${durationInSeconds / 60}${String.format(":%02d", durationInSeconds % 60)})  ")
		}

		return "$potionBuilder".trim { it <= ' ' }
	}

	private fun getEnchantmentString(itemStack: IItemStack): String
	{
		val enchantBuilder = StringBuilder()

		functions.getEnchantments(itemStack).forEach { (enchID, amplifier) -> enchantBuilder.append("\u00A7l${Maps.ENCHANTMENT_SHORT_NAME[enchID]} $amplifier  ") }

		return "$enchantBuilder".trim { it <= ' ' }
	}

	private fun getArmorPotential(thePlayer: IEntityPlayer): String
	{
		return if (!armorPotentialCooldown.attemptReset()) lastArmorPotential
		else
		{
			val ap = roundDecimals(getArmorPotential(thePlayer, false))
			val app = roundDecimals(getArmorPotential(thePlayer, true))
			(if (ap == app) "$ap%" else "$ap% | $app%").also { lastArmorPotential = it }
		}
	}

	private fun getArmorPotential(thePlayer: IEntityPlayer, projectileProtection: Boolean): Double
	{
		var armor = 0.0
		var epf = 0

		val provider = classProvider

		thePlayer.inventory.armorInventory.forEach { itemStack ->
			if (itemStack != null)
			{
				val item = itemStack.item

				if (item != null && provider.isItemArmor(item)) armor += item.asItemArmor().damageReduceAmount.toDouble() * 0.04

				val func = functions

				if (itemStack.isItemEnchanted) epf += getEffProtPoints(func.getEnchantmentLevel(0, itemStack))

				if (projectileProtection && itemStack.isItemEnchanted) epf += getEffProtPoints(func.getEnchantmentLevel(4, itemStack))
			}
		}

		return roundDouble(addArmorProtResistance(armor, calcProtection(epf.coerceAtMost(25).toDouble()), thePlayer.getActivePotionEffect(provider.getPotionEnum(PotionType.RESISTANCE))?.amplifier?.plus(1) ?: 0) * 100.0)
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

	private fun getHeldItemCount(thePlayer: IEntityPlayer, currentEquippedItem: IItemStack, bow: Boolean): Int
	{
		val provider = classProvider
		val func = functions

		var itemID: Int = func.getIdFromItem(currentEquippedItem.item!!)
		var itemMeta: Int = currentEquippedItem.itemDamage

		if (bow)
		{
			itemID = func.getIdFromItem(provider.getItemEnum(ItemType.ARROW))
			itemMeta = 0
		}

		var totalItemCount = 0

		thePlayer.inventory.mainInventory.filterNotNull().filter { func.getIdFromItem(it.item!!) == itemID }.filter { it.itemDamage == itemMeta }.forEach { stack -> totalItemCount += stack.stackSize }

		return totalItemCount
	}

	private fun printArmorWarning(resolution: IScaledResolution, font: IFontRenderer)
	{
		val text = "ARMOR DURABILITY IS LOW!"
		font.drawString(text, resolution.scaledWidth - font.getStringWidth(text) - 1f, resolution.scaledHeight - 3 * font.fontHeight - 1f, 16724804, true)
	}

	private fun isArmorDurabilityLow(thePlayer: IEntityPlayer): Boolean
	{
		return if (!armorDurabilityWarningCooldown.attemptReset()) showArmorDurabilityWarning
		else
		{
			thePlayer.inventory.armorInventory.forEach { armor ->
				if (armor != null && (armor.itemDamage.toDouble() / armor.maxDamage.toDouble() > 0.85 || armor.maxDamage - armor.itemDamage < 15))
				{
					val id: Int = functions.getIdFromItem(armor.item ?: return@forEach)

					if (id in 298..317) return@isArmorDurabilityLow true.also { showArmorDurabilityWarning = it }
				}
			}

			false.also { showArmorDurabilityWarning = it }
		}
	}
}
