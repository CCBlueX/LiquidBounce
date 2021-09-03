/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
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
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_1
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_2
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.createRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.*

/**
 * A target hud
 */
@ElementInfo(name = "Target")
class Target : Element()
{
	private val minWidthValue = IntegerValue("MinWidth", 180, 160, 300)
	private val heightValue = IntegerValue("Height", 100, 100, 150)

	private val headSizeValue = IntegerValue("HeadSize", 90, 30, 90)

	private val textYOffsetValue = IntegerValue("TextYOffset", 35, 0, 50)
	private val textScaleValue = FloatValue("TextScale", 0.5F, 0.5F, 0.75F)

	private val playerOnlyValue = BoolValue("PlayerOnly", true)
	private val barWidthSubtractorValue = IntegerValue("BarWidthSubtractor", 2, 0, 5)

	private val damageAnimationColorRedValue = IntegerValue("DamageAnimationColorRed", 252, 0, 255)
	private val damageAnimationColorGreenValue = IntegerValue("DamageAnimationColorGreen", 185, 0, 255)
	private val damageAnimationColorBlueValue = IntegerValue("DamageAnimationColorBlue", 65, 0, 255)

	private val healAnimationColorRedValue = IntegerValue("HealAnimationColorRed", 44, 0, 255)
	private val healAnimationColorGreenValue = IntegerValue("HealAnimationColorGreen", 201, 0, 255)
	private val healAnimationColorBlueValue = IntegerValue("HealAnimationColorBlue", 144, 0, 255)

	private val healthFadeStartDelayValue = IntegerValue("HealthFadeStartDelay", 2, 0, 40)
	private val healthFadeSpeedValue = IntegerValue("HealthFadeSpeed", 2, 1, 9)
	private val absorptionFadeSpeedValue = IntegerValue("AbsorptionFadeSpeed", 2, 1, 9)
	private val armorFadeSpeedValue = IntegerValue("ArmorFadeSpeed", 2, 1, 9)

	private val healthTypeValue = ListValue("HealthType", arrayOf("Datawatcher", "Mineplex", "Hive"), "Datawatcher")

	private val renderEquipmentsValue = BoolValue("Armor", true)

	private val backgroundColorModeValue = ListValue("Background-Color", arrayOf("None", "Custom", "Rainbow", "RainbowShader"), "Custom")
	private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
	private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
	private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
	private val backgroundRainbowCeilValue = BoolValue("Background-RainbowCeil", false)

	private val borderColorModeValue = ListValue("Border-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
	private val borderWidthValue = FloatValue("Border-Width", 3F, 2F, 5F)
	private val borderColorRedValue = IntegerValue("Border-R", 0, 0, 255)
	private val borderColorGreenValue = IntegerValue("Border-G", 0, 0, 255)
	private val borderColorBlueValue = IntegerValue("Border-B", 0, 0, 255)
	private val borderColorAlphaValue = IntegerValue("Border-Alpha", 0, 0, 255)

	private val saturationValue = FloatValue("HSB-Saturation", 0.9f, 0f, 1f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1f, 0f, 1f)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)

	private val rainbowShaderXValue = FloatValue("RainbowShader-X", -1000F, -2000F, 2000F)
	private val rainbowShaderYValue = FloatValue("RainbowShader-Y", -1000F, -2000F, 2000F)

	private val nameFontValue = FontValue("NameFont", Fonts.font40)
	private val textFontValue = FontValue("TextFont", Fonts.font35)

	private var easingHealth: Float = 0F
	private var healthAnimationDelay = 0
	private var easingAbsorption: Float = 0F
	private var easingArmor: Float = 0F
	private var lastTarget: IEntity? = null
	private var prevTargetHealth = 0F
	private var prevTick = 0

	override fun drawElement(): Border?
	{
		val thePlayer = mc.thePlayer ?: return null
		val renderItem = mc.renderItem
		val netHandler = mc.netHandler
		val textureManager = mc.textureManager

		val targetInfo = queryTarget()
		var target = targetInfo?.first
		val debug = targetInfo?.second

		if (targetInfo == null && classProvider.isGuiHudDesigner(mc.currentScreen)) target = thePlayer

		val minWidth = minWidthValue.get().toFloat()
		val height = heightValue.get().toFloat()

		val rainbowSpeed = rainbowSpeedValue.get()
		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()

		val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
		val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

		val rainbowRGB = ColorUtils.rainbowRGB(speed = rainbowSpeed, saturation = saturation, brightness = brightness)

		val backgroundColorMode = backgroundColorModeValue.get()
		val backgroundRainbowCeil = backgroundRainbowCeilValue.get()
		val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
		val backgroundColor = when
		{
			backgroundRainbowShader -> 0
			backgroundColorMode.equals("Rainbow", ignoreCase = true) -> rainbowRGB
			else -> createRGB(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get())
		}

		val borderWidth = borderWidthValue.get()
		val borderColorMode = borderColorModeValue.get()
		val borderColorAlpha = borderColorAlphaValue.get()
		val borderRainbowShader = borderColorMode.equals("RainbowShader", ignoreCase = true)
		val shouldDrawBorder = borderColorAlpha > 0
		val borderColor = if (shouldDrawBorder) when
		{
			borderRainbowShader -> 0
			borderColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.applyAlphaChannel(rainbowRGB, borderColorAlpha)
			else -> createRGB(borderColorRedValue.get(), borderColorGreenValue.get(), borderColorBlueValue.get(), borderColorAlpha)
		}
		else 0

		if (target != null && target.entityAlive)
		{
			val isPlayer = classProvider.isEntityPlayer(target)

			if (isPlayer || !playerOnlyValue.get())
			{
				val headRenderSize = headSizeValue.get()

				val nameFont = nameFontValue.get()
				val textFont = textFontValue.get()

				val targetAbsorption = target.absorptionAmount
				var targetArmor = 0
				var targetHealth = target.health + targetAbsorption
				if (prevTargetHealth != targetHealth) healthAnimationDelay = healthFadeStartDelayValue.get()
				prevTargetHealth = targetHealth

				val targetMaxHealth = target.maxHealth + targetAbsorption

				// Damage/Heal animation color
				val damageColor = createRGB(damageAnimationColorRedValue.get(), damageAnimationColorGreenValue.get(), damageAnimationColorBlueValue.get())
				val healColor = createRGB(healAnimationColorRedValue.get(), healAnimationColorGreenValue.get(), healAnimationColorBlueValue.get())

				val dataWatcherBuilder = StringJoiner("\u00A7r | ", " | ", "\u00A7r").setEmptyValue("")

				if (target.invisible) dataWatcherBuilder.add("\u00A77\u00A7oInvisible")
				if (target.burning) dataWatcherBuilder.add("\u00A7cBurning")
				if (target.isEating) dataWatcherBuilder.add("\u00A7eEating")
				if (target.isSilent) dataWatcherBuilder.add("\u00A78Silent")

				val headBoxYSize = headRenderSize + 6F

				var textXOffset = 2
				var healthBarYOffset = height - 3F /*107F*/

				val name = target.displayName.formattedText
				val targetHealthPercentage = targetHealth / targetMaxHealth

				if (isPlayer)
				{
					val targetPlayer: IEntityPlayer = target.asEntityPlayer()

					val healthMethod = healthTypeValue.get().toLowerCase()
					if (healthMethod.equals("Mineplex", ignoreCase = true) || healthMethod.equals("Hive", ignoreCase = true)) targetHealth = EntityUtils.getPlayerHealthFromScoreboard(targetPlayer.gameProfile.name, isMineplex = healthTypeValue.get().equals("Mineplex", true)).toFloat()

					targetArmor = targetPlayer.totalArmorValue

					textXOffset = headRenderSize + 10
					healthBarYOffset = height - 6F /*104F*/
				}

				var textYOffset = 10

				// Reset easing
				val targetChanged = target != lastTarget
				if (targetChanged || easingHealth < 0 || easingHealth > targetMaxHealth || abs(easingHealth - targetHealth) < 0.01) easingHealth = targetHealth
				if (targetChanged || easingAbsorption < 0 || easingAbsorption > targetAbsorption || abs(easingAbsorption - targetAbsorption) < 0.01) easingAbsorption = targetAbsorption
				if (isPlayer && (targetChanged || easingArmor < 0 || easingArmor > 20 || abs(easingArmor - targetArmor) < 0.01)) easingArmor = targetArmor.toFloat()
				val suspendAnimation = healthAnimationDelay > 0
				if (suspendAnimation && thePlayer.ticksExisted != prevTick && healthAnimationDelay > 0) healthAnimationDelay--
				prevTick = thePlayer.ticksExisted

				val healthText = "${if (targetHealthPercentage < 0.25) "\u00A7c" else if (targetHealthPercentage < 0.5) "\u00A7e" else "\u00A7a"}${DECIMALFORMAT_2.format(targetHealth.toDouble())} (${DECIMALFORMAT_1.format(targetHealthPercentage * 100.0)}%)\u00A7r"
				val armorText = "${if (targetArmor > 0) "\u00A7b" else "\u00A77"}$targetArmor (${DECIMALFORMAT_2.format(targetArmor / 20.0 * 100.0)}%)\u00A7r"

				val distanceText = DECIMALFORMAT_2.format(thePlayer.getDistanceToEntityBox(target))
				val yawText = "${DECIMALFORMAT_2.format(target.rotationYaw % 360f)} (${StringUtils.getHorizontalFacingAdv(target.rotationYaw)})"
				val pitchText = DECIMALFORMAT_2.format(target.rotationPitch)

				val velocityText = "${DECIMALFORMAT_2.format(target.motionX)}, ${DECIMALFORMAT_2.format(target.motionY)}, ${DECIMALFORMAT_2.format(target.motionZ)}"

				val healthColor = ColorUtils.getHealthColor(easingHealth, targetMaxHealth)

				val width = (textXOffset.toFloat() + nameFont.getStringWidth(name) + 10).coerceAtLeast(minWidth)

				// Draw Body Rect
				if (shouldDrawBorder)
				{
					RainbowShader.begin(borderRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
						RenderUtils.drawRect(-borderWidth, -borderWidth, width + borderWidth, height + borderWidth, borderColor)
					}
				}

				RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
					RenderUtils.drawRect(0F, 0F, width, height, backgroundColor)
				}

				if (backgroundRainbowCeil) RainbowShader.begin(true, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
					RenderUtils.drawRect(0F, -1F, width, 0F, 0)
				}

				val barWidthSubtractor = barWidthSubtractorValue.get().toFloat()
				val barWidth = width - barWidthSubtractor
				val gradationWidth = barWidth - barWidthSubtractor

				// Draw Absorption
				RenderUtils.drawRect(((easingHealth / targetMaxHealth) * barWidth) - ((easingAbsorption / targetMaxHealth) * barWidth), healthBarYOffset - 2, (easingHealth / targetMaxHealth) * barWidth, healthBarYOffset - 1, -256)

				// Draw Damage animation
				if (easingHealth > targetHealth) RenderUtils.drawRect(barWidthSubtractor, healthBarYOffset, max((easingHealth / targetMaxHealth) * barWidth, barWidthSubtractor), healthBarYOffset + 2, damageColor)

				// Draw Health bar
				RenderUtils.drawRect(barWidthSubtractor, healthBarYOffset, max(targetHealthPercentage * barWidth, barWidthSubtractor), healthBarYOffset + 2, healthColor.rgb)

				// Draw Heal animation
				if (easingHealth < targetHealth) RenderUtils.drawRect((easingHealth / targetMaxHealth) * barWidth, healthBarYOffset, targetHealthPercentage * barWidth, healthBarYOffset + 2, healColor)

				// Draw Health Gradations
				val limitedMaxHealth = targetMaxHealth.coerceAtMost(50F)
				val healthGradationGap = gradationWidth / limitedMaxHealth
				for (index in 1 until limitedMaxHealth.roundToInt()) RenderUtils.drawRect(healthGradationGap * index + barWidthSubtractor, healthBarYOffset - 2, healthGradationGap * index + 1 + barWidthSubtractor, healthBarYOffset + 2, -16777216)

				if (isPlayer)
				{
					// Draw Head Box
					RenderUtils.drawRect(2F, 2F, textXOffset - 4F, headBoxYSize, -12566464)

					// Draw Total Armor bar
					RenderUtils.drawRect(barWidthSubtractor, height - 2F /*108F*/, max(easingArmor * barWidth * 0.05f, barWidthSubtractor), height - 1F /*109F*/, -16711681)

					// Draw Armor Gradations
					val armorGradationGap = gradationWidth * 0.05f
					for (index in 1 until 20) RenderUtils.drawRect(armorGradationGap * index + barWidthSubtractor, height - 2F /*108F*/, armorGradationGap * index + 1 + barWidthSubtractor, height - 1F /*109F*/, -16777216)

					val skinResource: IResourceLocation
					val ping: Int
					val pingTextColor: Int

					val playerInfo = netHandler.getPlayerInfo(target.uniqueID)
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
						skinResource = WDefaultPlayerSkin.getDefaultSkin(target.uniqueID)
					}

					// Draw head
					RenderUtils.resetColor()
					textureManager.bindTexture(skinResource)
					RenderUtils.drawScaledCustomSizeModalRect(4, 4, 8F, 8F, 8, 8, headRenderSize, headRenderSize, 64F, 64F)

					// Reset color after drawing head
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
					RenderUtils.drawModalRectWithCustomSizedTexture(textXOffset.toFloat(), 20f, 0f, (176 + (pingLevelImageID shl 3)).toFloat(), 10f, 8f, 256f, 256f)

					textYOffset = 20

					// Draw Ping text
					textFont.drawString("${ping}ms", textXOffset + 12, 22, pingTextColor)

					easingArmor += ((targetArmor - easingArmor) / 2.0F.pow(10.0F - armorFadeSpeedValue.get())) * RenderUtils.deltaTime
				}

				// Render equipments
				if (renderEquipmentsValue.get())
				{
					textYOffset += 15

					val prevZLevel = renderItem.zLevel
					renderItem.zLevel = -147F

					repeat(5) { index ->
						val isHeldItem = index == 0

						val equipmentX = textXOffset + (4 - index) * 20 + if (isHeldItem) 5 else 0

						RenderUtils.drawRect(equipmentX, textYOffset, equipmentX + 16, textYOffset + 16, -12566464)

						val armor = target.getEquipmentInSlot(index) ?: return@repeat

						RenderUtils.glColor(Color.white) // Reset Color

						RenderUtils.drawItemStack(renderItem, armor, equipmentX, textYOffset, true)
					}

					renderItem.zLevel = prevZLevel
				}

				RenderUtils.glColor(Color.white) // Reset Color

				if (!suspendAnimation) easingHealth += ((targetHealth - easingHealth) / 2.0F.pow(10.0F - healthFadeSpeedValue.get())) * RenderUtils.deltaTime
				easingAbsorption += ((targetAbsorption - easingAbsorption) / 2.0F.pow(10.0F - absorptionFadeSpeedValue.get())) * RenderUtils.deltaTime

				// Draw Target Name
				nameFont.drawString(name, textXOffset, 3, 0xffffff)

				// Render Target Stats

				val scale = textScaleValue.get()
				val reverseScale = 1.0F / scale

				// val yPos = max(headRenderSize + 10f, textYOffset + 20f)
				val min = min(textYOffset + 20f, 45f)
				val scaledXPos = (if (headRenderSize <= min) 2 else textXOffset) * reverseScale
				val scaledYPos = (textYOffset + 20f + textYOffsetValue.get()) * reverseScale

				GL11.glScalef(scale, scale, scale)

				// Draw CustomNameTag
				if (target.customNameTag.isNotBlank()) textFont.drawString("(${target.customNameTag})", textXOffset * reverseScale, (nameFont.fontHeight + 5) * reverseScale, Color.gray.rgb)

				// Health/Armor-related
				textFont.drawString("Health: $healthText | Absorption: ${if (targetAbsorption > 0) "\u00A7e" else "\u00A77"}${DECIMALFORMAT_1.format(targetAbsorption.toLong())}\u00A7r | Armor: $armorText", scaledXPos, scaledYPos, 0xffffff)

				// Movement/Position-related
				textFont.drawString("Distance: ${distanceText}m | ${if (target.onGround) "\u00A7a" else "\u00A7c"}Ground\u00A7r | ${if (!target.sprinting) "\u00A7c" else "\u00A7a"}Sprinting\u00A7r | ${if (!target.sneaking) "\u00A7c" else "\u00A7a"}Sneaking\u00A7r", scaledXPos, scaledYPos + 12, 0xffffff)

				// Rotation-related
				textFont.drawString("Yaw: $yawText | Pitch: $pitchText | Velocity: [$velocityText]", scaledXPos, scaledYPos + 24, 0xffffff)

				// Hurt-related
				textFont.drawString("Hurt: ${if (target.hurtTime > 0) "\u00A7c" else "\u00A7a"}${target.hurtTime}\u00A7r | HurtResis: ${if (target.hurtResistantTime > 0) "\u00A7c" else "\u00A7a"}${target.hurtResistantTime}\u00A7r | Air: ${if (target.air > 0) "\u00A7c" else "\u00A7a"}${target.air}\u00A7r ", scaledXPos, scaledYPos + 34, 0xffffff)

				// Datawatcher-related
				textFont.drawString("EntityID: ${target.entityId}$dataWatcherBuilder", scaledXPos, scaledYPos + 44, 0xffffff)

				// debug data
				if (debug != null) textFont.drawString(debug, scaledXPos, scaledYPos + 54, 0xffffff)

				GL11.glScalef(reverseScale, reverseScale, reverseScale)
			}
		}

		lastTarget = target

		val borderExpanded = if (shouldDrawBorder) borderWidth else 0F
		return Border(0F - borderExpanded, 0F - borderExpanded, minWidth + borderExpanded, height + borderExpanded)
	}

	private fun queryTarget(): Pair<IEntityLivingBase?, String?>?
	{
		val moduleManager = LiquidBounce.moduleManager

		val tpAura = moduleManager[TpAura::class.java] as TpAura
		val killAura = moduleManager[KillAura::class.java] as KillAura
		val aimbot = moduleManager[Aimbot::class.java] as Aimbot
		val bowAimbot = moduleManager[BowAimbot::class.java] as BowAimbot

		val killAuraTarget = killAura.target
		val aimbotTarget = aimbot.target
		val bowAimbotTarget = bowAimbot.target

		return when
		{
			tpAura.state && tpAura.maxTargetsValue.get() == 1 && tpAura.currentTarget != null -> tpAura.currentTarget to "TpAura[${tpAura.debug}]"
			killAuraTarget != null -> killAuraTarget to "KillAura[${killAura.debug}]"
			aimbotTarget != null -> aimbotTarget to "Aimbot"
			bowAimbotTarget != null -> bowAimbotTarget to "BowAimbot"
			else -> null
		}
	}
}
