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
import net.ccbluex.liquidbounce.api.minecraft.util.WDefaultPlayerSkin
import net.ccbluex.liquidbounce.features.module.modules.combat.Aimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.BowAimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.TpAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.createRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
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
	companion object
	{
		private val decimalFormat1 = DecimalFormat("##0.0", DecimalFormatSymbols(Locale.ENGLISH))
		private val decimalFormat2 = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))
	}

	private val playerOnlyValue = BoolValue("PlayerOnly", true)
	private val barWidthSubtractorValue = IntegerValue("BarWidthSubtractor", 2, 0, 5)

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

	override fun drawElement(): Border?
	{
		val thePlayer = mc.thePlayer ?: return null
		val renderItem = mc.renderItem
		val netHandler = mc.netHandler
		val textureManager = mc.textureManager

		val moduleManager = LiquidBounce.moduleManager

		val tpAura = moduleManager[TpAura::class.java] as TpAura
		val targetEntity = if (tpAura.state && tpAura.maxTargetsValue.get() == 1 && tpAura.currentTarget != null) tpAura.currentTarget else ((moduleManager[KillAura::class.java] as KillAura).target ?: (moduleManager[Aimbot::class.java] as Aimbot).target) ?: (moduleManager[BowAimbot::class.java] as BowAimbot).target

		if (targetEntity != null && targetEntity.entityAlive)
		{
			val isPlayer = classProvider.isEntityPlayer(targetEntity)

			if (isPlayer || !playerOnlyValue.get())
			{
				val targetAbsorption = targetEntity.absorptionAmount
				var targetHealth = targetEntity.health + targetAbsorption
				var targetArmor = 0

				val targetMaxHealth = targetEntity.maxHealth + targetAbsorption
				val targetMaxHealthInt = targetMaxHealth.roundToInt()

				val damageColor = createRGB(damageAnimationColorRed.get(), damageAnimationColorGreen.get(), damageAnimationColorBlue.get(), 255)
				val healColor = createRGB(healAnimationColorRed.get(), healAnimationColorGreen.get(), healAnimationColorBlue.get(), 255)

				val dataWatcherBuilder = StringJoiner("\u00A7r | ", " | ", "\u00A7r")

				if (targetEntity.invisible) dataWatcherBuilder.add("\u00A77\u00A7oInvisible")

				if (targetEntity.burning) dataWatcherBuilder.add("\u00A7cBurning")

				if (targetEntity.isInWeb) dataWatcherBuilder.add("\u00A77In Web")

				if (targetEntity.isInWater) dataWatcherBuilder.add("\u00A79In Water")

				if (targetEntity.isInLava) dataWatcherBuilder.add("\u00A7cIn Lava")

				var xShift = 2
				var healthBarYOffset = 107F

				var name = targetEntity.customNameTag.ifBlank(targetEntity::name)
				val targetHealthPercentage = targetHealth / targetMaxHealth

				if (isPlayer)
				{
					val targetPlayer: IEntityPlayer = targetEntity.asEntityPlayer()

					val healthMethod = healthGetMethod.get().toLowerCase()
					if (healthMethod.equals("Mineplex", ignoreCase = true) || healthMethod.equals("Hive", ignoreCase = true)) targetHealth = EntityUtils.getPlayerHealthFromScoreboard(targetPlayer.gameProfile.name, isMineplex = healthGetMethod.get().equals("Mineplex", true)).toFloat()

					targetArmor = targetPlayer.totalArmorValue
					name = targetPlayer.customNameTag.ifBlank(targetPlayer::displayNameString)

					xShift = 100
					healthBarYOffset = 104F
				}

				val targetChanged = targetEntity != lastTarget

				if (targetChanged || easingHealth < 0 || easingHealth > targetMaxHealth || abs(easingHealth - targetHealth) < 0.01) easingHealth = targetHealth
				if (targetChanged || easingAbsorption < 0 || easingAbsorption > targetAbsorption || abs(easingAbsorption - targetAbsorption) < 0.01) easingAbsorption = targetAbsorption
				if (isPlayer && (targetChanged || easingArmor < 0 || easingArmor > 20 || abs(easingArmor - targetArmor) < 0.01)) easingArmor = targetArmor.toFloat()

				val healthText = "${if (targetHealthPercentage < 0.25) "\u00A7c" else if (targetHealthPercentage < 0.5) "\u00A7e" else "\u00A7a"}${decimalFormat2.format(targetHealth.toDouble())} (${decimalFormat1.format(targetHealthPercentage * 100.0)}%)\u00A7r"
				val armorText = "${if (targetArmor > 0) "\u00A7b" else "\u00A77"}$targetArmor (${decimalFormat2.format(targetArmor / 20.0 * 100.0)}%)\u00A7r"

				val distanceText = decimalFormat2.format(thePlayer.getDistanceToEntityBox(targetEntity))
				val yawText = "${decimalFormat2.format(targetEntity.rotationYaw % 360f)} (${StringUtils.getHorizontalFacingAdv(targetEntity.rotationYaw)})"
				val pitchText = decimalFormat2.format(targetEntity.rotationPitch)

				val velocityText = "${decimalFormat2.format(targetEntity.motionX)}, ${decimalFormat2.format(targetEntity.motionY)}, ${decimalFormat2.format(targetEntity.motionZ)}"

				val healthColor = ColorUtils.getHealthColor(easingHealth, targetMaxHealth)

				val width = (xShift.toFloat() + Fonts.font60.getStringWidth(name)).coerceAtLeast(250.0F)

				// Draw Body Rect
				RenderUtils.drawBorderedRect(0F, 0F, width, 110F, borderWidth.get(), createRGB(borderColorRed.get(), borderColorGreen.get(), borderColorBlue.get(), 255), -16777216)

				val barWidthSubtractor = barWidthSubtractorValue.get().toFloat()
				val barWidth = width - barWidthSubtractor
				val gradationWidth = barWidth - barWidthSubtractor

				// Draw Absorption
				RenderUtils.drawRect(((easingHealth / targetMaxHealth) * barWidth) - ((easingAbsorption / targetMaxHealth) * barWidth), healthBarYOffset - 2, (easingHealth / targetMaxHealth) * barWidth, healthBarYOffset - 1, -256)

				// Draw Damage animation
				if (easingHealth > targetHealth) RenderUtils.drawRect(barWidthSubtractor, healthBarYOffset, (easingHealth / targetMaxHealth) * barWidth, healthBarYOffset + 2, damageColor)

				// Draw Health bar
				RenderUtils.drawRect(barWidthSubtractor, healthBarYOffset, targetHealthPercentage * barWidth, healthBarYOffset + 2, healthColor.rgb)

				// Draw Heal animation
				if (easingHealth < targetHealth) RenderUtils.drawRect((easingHealth / targetMaxHealth) * barWidth, healthBarYOffset, targetHealthPercentage * barWidth, healthBarYOffset + 2, healColor)

				// Draw Health Gradations
				val healthGradationGap = gradationWidth / targetMaxHealthInt
				for (index in 1 until targetMaxHealthInt) RenderUtils.drawRect(healthGradationGap * index + barWidthSubtractor, healthBarYOffset - 2, healthGradationGap * index + 1 + barWidthSubtractor, healthBarYOffset + 2, -16777216)

				if (isPlayer)
				{
					// Draw Head Box
					RenderUtils.drawRect(2F, 2F, xShift - 4F, 96F, -12566464)

					// Draw Total Armor bar
					RenderUtils.drawRect(barWidthSubtractor, 108F, easingArmor * barWidth * 0.05f, 109F, -16711681)

					// Draw Armor Gradations
					val armorGradationGap = gradationWidth * 0.05f
					for (index in 1 until 20) RenderUtils.drawRect(armorGradationGap * index + barWidthSubtractor, 108F, armorGradationGap * index + 1 + barWidthSubtractor, 109F, -16777216)

					val skinResource: IResourceLocation
					val ping: Int
					val pingTextColor: Int

					val playerInfo = netHandler.getPlayerInfo(targetEntity.uniqueID)
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
						skinResource = WDefaultPlayerSkin.getDefaultSkin(targetEntity.uniqueID)
					}

					// Draw head
					RenderUtils.resetColor()
					textureManager.bindTexture(skinResource)
					RenderUtils.drawScaledCustomSizeModalRect(4, 4, 8F, 8F, 8, 8, 90, 90, 64F, 64F)

					RenderUtils.glColor(Color.white)

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

							RenderUtils.drawRect(equipmentX, equipmentY, equipmentX + 16, equipmentY + 16, -12566464)

							val armor = targetEntity.getEquipmentInSlot(index) ?: return@repeat

							RenderUtils.glColor(Color.white) // Reset Color
							renderItem.zLevel = -147F
							renderItem.renderItemAndEffectIntoGUI(armor, equipmentX, equipmentY)
						}
					}

					RenderUtils.glColor(Color.white) // Reset Color

					easingArmor += ((targetArmor - easingArmor) / 2.0F.pow(10.0F - armorFadeSpeed.get())) * RenderUtils.deltaTime
				}

				easingHealth += ((targetHealth - easingHealth) / 2.0F.pow(10.0F - healthFadeSpeed.get())) * RenderUtils.deltaTime
				easingAbsorption += ((targetAbsorption - easingAbsorption) / 2.0F.pow(10.0F - absorptionFadeSpeed.get())) * RenderUtils.deltaTime

				// Draw Target Name
				Fonts.font60.drawString(name, xShift, 3, 0xffffff)

				// Render Target Stats

				val scale = 0.6F
				val reverseScale = 1.0F / scale

				val scaledXShift = xShift * reverseScale
				val scaledYPos = 55 * reverseScale

				GL11.glScalef(scale, scale, scale)

				// Health/Armor-related
				Fonts.font35.drawString("Health: $healthText | Absorption: ${if (targetAbsorption > 0) "\u00A7e" else "\u00A77"}${decimalFormat1.format(targetAbsorption.toLong())}\u00A7r | Armor: $armorText", scaledXShift, scaledYPos, 0xffffff)

				// Movement/Position-related
				Fonts.font35.drawString("Distance: ${distanceText}m | ${if (targetEntity.onGround) "\u00A7a" else "\u00A7c"}Ground\u00A7r | ${if (targetEntity.isAirBorne) "\u00A7a" else "\u00A7c"}AirBorne\u00A7r | ${if (!targetEntity.sprinting) "\u00A7c" else "\u00A7a"}Sprinting\u00A7r | ${if (!targetEntity.sneaking) "\u00A7c" else "\u00A7a"}Sneaking\u00A7r", scaledXShift, scaledYPos + 15, 0xffffff)

				// Rotation-related
				Fonts.font35.drawString("Yaw: $yawText | Pitch: $pitchText | Velocity: [$velocityText]", scaledXShift, scaledYPos + 25, 0xffffff)

				// Hurt-related
				Fonts.font35.drawString("Hurt: ${if (targetEntity.hurtTime > 0) "\u00A7c" else "\u00A7a"}${targetEntity.hurtTime}\u00A7r | HurtResistantTime: ${if (targetEntity.hurtResistantTime > 0) "\u00A7c" else "\u00A7a"}${targetEntity.hurtResistantTime}\u00A7r ", scaledXShift, scaledYPos + 40, 0xffffff)

				// Datawatcher-related
				Fonts.font35.drawString("EntityID: ${targetEntity.entityId}$dataWatcherBuilder ", scaledXShift, scaledYPos + 55, 0xffffff)

				GL11.glScalef(reverseScale, reverseScale, reverseScale)
			}
		}

		lastTarget = targetEntity
		return Border(0F, 0F, 250F, 110F)
	}
}
