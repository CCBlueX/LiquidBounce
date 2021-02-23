/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.render.texture.ITextureManager
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.ccbluex.liquidbounce.api.minecraft.util.WDefaultPlayerSkin
import net.ccbluex.liquidbounce.features.module.modules.combat.Aimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.TpAura
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

// TODO: BowAimbot support
/**
 * A target hud
 */
@ElementInfo(name = "Target")
class Target : Element()
{
	companion object
	{
		private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))
	}

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
		val thePlayer = mc.thePlayer!!
		val renderItem = mc.renderItem
		val netHandler = mc.netHandler
		val textureManager = mc.textureManager

		val moduleManager = LiquidBounce.moduleManager

		val tpAura = moduleManager[TpAura::class.java] as TpAura
		val targetEntity = if (tpAura.state && tpAura.maxTargetsValue.get() == 1 && tpAura.currentTarget != null) tpAura.currentTarget else ((moduleManager[KillAura::class.java] as KillAura).target ?: (moduleManager[Aimbot::class.java] as Aimbot).target)

		if (classProvider.isEntityPlayer(targetEntity) && targetEntity!!.asEntityPlayer().entityAlive)
		{
			val targetPlayer: IEntityPlayer = targetEntity.asEntityPlayer()

			val targetHealth = when (healthGetMethod.get().toLowerCase())
			{
				"mineplex", "hive" -> EntityUtils.getPlayerHealthFromScoreboard(targetPlayer.gameProfile.name, isMineplex = healthGetMethod.get().equals("Mineplex", true)).toFloat()
				else -> targetPlayer.health
			} + targetPlayer.absorptionAmount
			val targetArmor = targetPlayer.totalArmorValue

			val targetMaxHealth = targetPlayer.maxHealth /* + ptargetHealthBoost + ptargetAbsorption */ + targetPlayer.absorptionAmount
			val targetMaxHealthInt = targetMaxHealth.roundToInt()

			val damageColor = Color(damageAnimationColorRed.get(), damageAnimationColorGreen.get(), damageAnimationColorBlue.get())
			val healColor = Color(healAnimationColorRed.get(), healAnimationColorGreen.get(), healAnimationColorBlue.get())

			if (targetPlayer != lastTarget)
			{
				if (easingHealth < 0 || easingHealth > targetMaxHealth || abs(easingHealth - targetHealth) < 0.01) easingHealth = targetHealth
				if (easingAbsorption < 0 || easingAbsorption > targetPlayer.absorptionAmount || abs(easingAbsorption - targetPlayer.absorptionAmount) < 0.01) easingAbsorption = targetPlayer.absorptionAmount
				if (easingArmor < 0 || easingArmor > 20 || abs(easingArmor - targetArmor) < 0.01) easingArmor = targetArmor.toFloat()
			}

			val healthColor = ColorUtils.getHealthColor(easingHealth, targetMaxHealth)

			val width = (100.0F + Fonts.font60.getStringWidth(targetPlayer.name!!)).coerceAtLeast(250.0F)

			// Draw Body Rect
			RenderUtils.drawBorderedRect(0F, 0F, width, 110F, borderWidth.get(), Color(borderColorRed.get(), borderColorGreen.get(), borderColorBlue.get()).rgb, Color.black.rgb)

			// Draw Head Box
			RenderUtils.drawRect(2F, 2F, 96F, 96F, Color.darkGray.rgb)

			// Draw Absorption
			RenderUtils.drawRect(((easingHealth / targetMaxHealth) * width) - ((/* ptargetAbsorption */ easingAbsorption / targetMaxHealth) * width) + 1, 103F, (easingHealth / targetMaxHealth) * width, 104F, Color.yellow.rgb)

			// Draw Damage animation
			if (easingHealth > targetHealth) RenderUtils.drawRect(0F, 105F, (easingHealth / targetMaxHealth) * width, 107F, damageColor.rgb)

			// Draw Health bar
			RenderUtils.drawRect(0F, 105F, (targetHealth / targetMaxHealth) * width, 107F, healthColor.rgb)

			// Draw Heal animation
			if (easingHealth < targetHealth) RenderUtils.drawRect((easingHealth / targetMaxHealth) * width, 105F, (targetHealth / targetMaxHealth) * width, 107F, healColor.rgb)

			// Draw Health Gradations
			val healthGradationGap = width / targetMaxHealthInt
			for (index in 1..targetMaxHealthInt) RenderUtils.drawRect(healthGradationGap * index, 103F, healthGradationGap * index + 1, 107F, Color.black.rgb)

			// Draw Total Armor bar
			RenderUtils.drawRect(0F, 109F, easingArmor * width * 0.05f, 110F, Color.cyan.rgb)

			// Draw Armor Gradations
			val armorGradationGap = width * 0.05f
			for (index in 1..20) RenderUtils.drawRect(armorGradationGap * index, 109F, armorGradationGap * index + 1, 110F, Color.black.rgb)

			easingHealth += ((targetHealth - easingHealth) / 2.0F.pow(10.0F - healthFadeSpeed.get())) * RenderUtils.deltaTime
			easingAbsorption += ((targetPlayer.absorptionAmount - easingAbsorption) / 2.0F.pow(10.0F - absorptionFadeSpeed.get())) * RenderUtils.deltaTime
			easingArmor += ((targetArmor - easingArmor) / 2.0F.pow(10.0F - armorFadeSpeed.get())) * RenderUtils.deltaTime

			// Draw Target Name
			Fonts.font60.drawString(targetPlayer.displayNameString, 100, 3, 0xffffff)

			// Draw informations

			val skinResource: IResourceLocation
			val ping: Int
			val pingTextColor: Int

			val playerInfo = netHandler.getPlayerInfo(targetPlayer.uniqueID)
			if (playerInfo != null)
			{
				ping = playerInfo.responseTime.coerceAtLeast(0)
				pingTextColor = if (ping > 300) 0xff0000 else ColorUtils.blendColors(floatArrayOf(0.0F, 0.5F, 1.0F), arrayOf(Color.GREEN, Color.YELLOW, Color.RED), ping / 300.0F).rgb
				skinResource = playerInfo.locationSkin
			}
			else
			{
				ping = -1
				pingTextColor = 0x808080
				skinResource = WDefaultPlayerSkin.getDefaultSkin(targetPlayer.uniqueID)
			}

			// Draw head
			drawHead(textureManager, skinResource, 90, 90)

			RenderUtils.glColor(Color.white) // Reset Color

			val pingLevelImageID: Int = when
			{
				ping < 0L -> 5
				ping < 150L -> 0
				ping < 300L -> 1
				ping < 600L -> 2
				ping < 1000L -> 3
				else -> 4
			}

			// Draw Ping level
			textureManager.bindTexture(RenderUtils.ICONS)
			RenderUtils.drawModalRectWithCustomSizedTexture(100f, 20f, 0f, (176 + (pingLevelImageID shl 3)).toFloat(), 10f, 8f, 256f, 256f)

			// Draw Ping text
			Fonts.font35.drawString("${ping}ms", 112, 22, pingTextColor)

			// Render equipments
			if (armor.get())
			{
				val equipmentY = 35
				repeat(5) { index ->
					val isHeldItem = index == 0

					val equipmentX = 100 + (4 - index) * 20 + if (isHeldItem) 5 else 0

					RenderUtils.drawRect(equipmentX, equipmentY, equipmentX + 16, equipmentY + 16, Color.darkGray.rgb)

					val armor = targetPlayer.getEquipmentInSlot(index) ?: return@repeat

					RenderUtils.glColor(Color.white) // Reset Color
					renderItem.zLevel = -147F
					renderItem.renderItemAndEffectIntoGUI(armor, equipmentX, equipmentY)
				}
			}

			RenderUtils.glColor(Color.white) // Reset Color

			// Render Target Stats

			val distanceText = decimalFormat.format(thePlayer.getDistanceToEntityBox(targetPlayer))
			Fonts.font35.drawString("${if (targetPlayer.onGround) "\u00A7aOn" else "\u00A7cOff"}-Ground\u00A7r | distance: ${distanceText}m", 100, 60, 0xffffff)
			Fonts.font35.drawString("${if (!targetPlayer.sprinting) "\u00A7cNot " else "\u00A7a"}Sprinting\u00A7r | ${if (!targetPlayer.sneaking) "\u00A7cNot " else "\u00A7a"}Sneaking\u00A7r", 100, 70, 0xffffff)

			val yawText = decimalFormat.format(targetPlayer.rotationYaw % 360f)
			val pitchText = decimalFormat.format(targetPlayer.rotationPitch)
			Fonts.font35.drawString("yaw: $yawText | pitch: $pitchText | hurt: ${if (targetPlayer.hurtTime > 0) "\u00A7c" else "\u00A7a"}${targetPlayer.hurtTime}\u00A7r", 100, 85, 0xffffff)
		}

		lastTarget = targetEntity
		return Border(0F, 0F, 250F, 110F)
	}

	private fun drawHead(textureManager: ITextureManager, skin: IResourceLocation, width: Int, height: Int)
	{
		GL11.glColor4f(1F, 1F, 1F, 1F)

		textureManager.bindTexture(skin)
		RenderUtils.drawScaledCustomSizeModalRect(4, 4, 8F, 8F, 8, 8, width, height, 64F, 64F)
	}
}
