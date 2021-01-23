/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WDefaultPlayerSkin
import net.ccbluex.liquidbounce.features.module.modules.combat.Aimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * A target hud
 */
@ElementInfo(name = "Target")
class Target : Element()
{

	private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))

	private val damageAnimationColorRed = IntegerValue("DamageAnimationColorRed", 252, 0, 255)
	private val damageAnimationColorGreen = IntegerValue("DamageAnimationColorGreen", 185, 0, 255)
	private val damageAnimationColorBlue = IntegerValue("DamageAnimationColorBlue", 65, 0, 255)

	private val healAnimationColorRed = IntegerValue("HealAnimationColorRed", 44, 0, 255)
	private val healAnimationColorGreen = IntegerValue("HealAnimationColorGreen", 201, 0, 255)
	private val healAnimationColorBlue = IntegerValue("HealAnimationColorBlue", 144, 0, 255)

	private val healthFadeSpeed = IntegerValue("HealthFadeSpeed", 2, 1, 9)
	private val absorptionFadeSpeed = IntegerValue("AbsorptionFadeSpeed", 2, 1, 9)
	private val armorFadeSpeed = IntegerValue("ArmorFadeSpeed", 2, 1, 9)

	private val healthGetMethod = ListValue("HealthGetMethod", arrayOf("Datawatcher", "Mineplex", "Hive"), "Datawatcher")

	private val armor = BoolValue("Armor", true)

	private val borderWidth = FloatValue("BorderWidth", 3F, 2F, 5F)
	private val borderColorRed = IntegerValue("BorderColorRed", 0, 0, 255)
	private val borderColorGreen = IntegerValue("BorderColorGreen", 0, 0, 255)
	private val borderColorBlue = IntegerValue("BorderColorBlue", 0, 0, 255)

	private var easingHealth: Float = 0F
	private var easingAbsorption: Float = 0F
	private var easingArmor: Float = 0F
	private var lastTarget: IEntity? = null

	override fun drawElement(): Border
	{
		val targetEntity = (LiquidBounce.moduleManager[KillAura::class.java] as KillAura).target ?: (LiquidBounce.moduleManager[Aimbot::class.java] as Aimbot).target

		if (classProvider.isEntityPlayer(targetEntity) && targetEntity!!.asEntityPlayer().entityAlive)
		{
			val targetPlayer: IEntityPlayer = targetEntity.asEntityPlayer()
			val targetHealth = when (healthGetMethod.get().toLowerCase())
			{
				"mineplex", "hive" -> EntityUtils.getPlayerHealthFromScoreboard(targetPlayer.gameProfile.name, healthGetMethod.get().equals("Mineplex", true)).toFloat()
				else -> targetPlayer.health
			} + targetPlayer.absorptionAmount

			val targetArmor = targetPlayer.totalArmorValue

			val targetMaxHealth = targetPlayer.maxHealth /* + ptargetHealthBoost + ptargetAbsorption */ + targetPlayer.absorptionAmount
			val targetMaxHealthInt = targetMaxHealth.roundToInt()

			val damageColor = Color(damageAnimationColorRed.get(), damageAnimationColorGreen.get(), damageAnimationColorBlue.get())
			val healColor = Color(healAnimationColorRed.get(), healAnimationColorGreen.get(), healAnimationColorBlue.get())

			if (targetPlayer != lastTarget || easingHealth < 0 || easingHealth > targetMaxHealth || abs(easingHealth - targetHealth) < 0.01) easingHealth = targetHealth
			if (targetPlayer != lastTarget || easingAbsorption < 0 || easingAbsorption > targetPlayer.absorptionAmount || abs(easingAbsorption - targetPlayer.absorptionAmount) < 0.01) easingAbsorption = targetPlayer.absorptionAmount
			if (targetPlayer != lastTarget || easingArmor < 0 || easingArmor > 20 || abs(easingArmor - targetArmor) < 0.01) easingArmor = targetArmor.toFloat()

			val healthColor = ColorUtils.getHealthColor(easingHealth, targetMaxHealth)

			val width = (98.0F + Fonts.font60.getStringWidth(targetPlayer.name!!)).coerceAtLeast(200.0F)

			// Draw rect box
			RenderUtils.drawBorderedRect(0F, 0F, width, 80F, borderWidth.get(), Color(borderColorRed.get(), borderColorGreen.get(), borderColorBlue.get()).rgb, Color.BLACK.rgb)

			// Head Box
			RenderUtils.drawRect(2F, 2F, 62F, 62F, 0x333333)

			// Absorption
			RenderUtils.drawRect(((easingHealth / targetMaxHealth) * width) - ((/* ptargetAbsorption */ easingAbsorption / targetMaxHealth) * width) + 1, 73F, (easingHealth / targetMaxHealth) * width, 74F, Color.YELLOW.rgb)

			// Damage animation
			if (easingHealth > targetHealth) RenderUtils.drawRect(0F, 75F, (easingHealth / targetMaxHealth) * width, 77F, damageColor.rgb)

			// Health bar
			RenderUtils.drawRect(0F, 75F, (targetHealth / targetMaxHealth) * width, 77F, healthColor.rgb)

			// Heal animation
			if (easingHealth < targetHealth) RenderUtils.drawRect((easingHealth / targetMaxHealth) * width, 75F, (targetHealth / targetMaxHealth) * width, 77F, healColor.rgb)

			for (index in 1..targetMaxHealthInt) RenderUtils.drawRect(width / targetMaxHealthInt * index, 73F, width / targetMaxHealthInt * index + 1, 77F, Color.BLACK.rgb)

			// Indicate total armor value
			RenderUtils.drawRect(0F, 79F, (easingArmor / 20) * width, 80F, Color.CYAN.rgb)

			for (index in 1..20) RenderUtils.drawRect(width / 20 * index, 79F, width / 20 * index + 1, 80F, Color.BLACK.rgb)


			easingHealth += ((targetHealth - easingHealth) / 2.0F.pow(10.0F - healthFadeSpeed.get())) * RenderUtils.deltaTime
			easingAbsorption += ((targetPlayer.absorptionAmount - easingAbsorption) / 2.0F.pow(10.0F - absorptionFadeSpeed.get())) * RenderUtils.deltaTime
			easingArmor += ((targetArmor - easingArmor) / 2.0F.pow(10.0F - armorFadeSpeed.get())) * RenderUtils.deltaTime

			Fonts.font60.drawString(targetPlayer.displayNameString, 78, 3, 0xffffff)

			// Draw informations
			val playerInfo = mc.netHandler.getPlayerInfo(targetPlayer.uniqueID)
			if (playerInfo != null)
			{
				val ping = playerInfo.responseTime.coerceAtLeast(0)
				val pingColor = if (ping > 300) Color.RED else ColorUtils.blendColors(floatArrayOf(0.0F, 0.5F, 1.0F), arrayOf(Color.GREEN, Color.YELLOW, Color.RED), ping / 300.0F)
				Fonts.font35.drawString("${ping}ms", 80, 20, pingColor!!.rgb)

				// Draw head
				drawHead(playerInfo.locationSkin, 60, 60)
			} else
			{
				Fonts.font35.drawString("0ms", 80, 20, 0x808080)
				drawHead(WDefaultPlayerSkin.getDefaultSkin(targetPlayer.uniqueID), 60, 60)
				WBlockPos
			}

			Fonts.font35.drawString("${if (targetPlayer.onGround) "On" else "Off"} Ground", 75, 30, 0xffffff)
			Fonts.font35.drawString("${if (!targetPlayer.sprinting) "Not " else ""}Sprinting | ${if (!targetPlayer.sneaking) "Not " else ""}Sneaking", 75, 40, 0xffffff)
			Fonts.font35.drawString("Distance > ${decimalFormat.format(mc.thePlayer!!.getDistanceToEntityBox(targetPlayer))}m", 75, 50, 0xffffff)
			Fonts.font35.drawString("Hurt > ${targetPlayer.hurtTime}", 75, 60, if (targetPlayer.hurtTime > 0) 0xff0000 /* RED */ else 0x00ff00 /* GREEN */)

			// Render equipments
			if (armor.get())
			{
				for (index in 0..4)
				{
					if (targetPlayer.getEquipmentInSlot(index) == null) continue

					mc.renderItem.zLevel = -147F
					mc.renderItem.renderItemAndEffectIntoGUI(targetPlayer.getEquipmentInSlot(index)!!, width.toInt() - 20, 2 + (4 - index) * 12)
				}
			}
		}

		lastTarget = targetEntity
		return Border(0F, 0F, 200F, 80F)
	}

	private fun drawHead(skin: IResourceLocation, width: Int, height: Int)
	{
		GL11.glColor4f(1F, 1F, 1F, 1F)
		mc.textureManager.bindTexture(skin)
		RenderUtils.drawScaledCustomSizeModalRect(4, 4, 8F, 8F, 8, 8, width, height, 64F, 64F)
	}
}
