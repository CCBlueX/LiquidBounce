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
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.*

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

	private val minWidthValue = IntegerValue("MinWidth", 180, 160, 300)
	private val heightValue = IntegerValue("Height", 100, 90, 150)

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

	private val healthFadeSpeedValue = IntegerValue("HealthFadeSpeed", 2, 1, 9)
	private val absorptionFadeSpeedValue = IntegerValue("AbsorptionFadeSpeed", 2, 1, 9)
	private val armorFadeSpeedValue = IntegerValue("ArmorFadeSpeed", 2, 1, 9)

	private val healthTypeValue = ListValue("HealthType", arrayOf("Datawatcher", "Mineplex", "Hive"), "Datawatcher")

	private val renderEquipmentsValue = BoolValue("Armor", true)

	private val borderWidthValue = FloatValue("BorderWidth", 3F, 2F, 5F)
	private val borderColorRedValue = IntegerValue("BorderColorRed", 0, 0, 255)
	private val borderColorGreenValue = IntegerValue("BorderColorGreen", 0, 0, 255)
	private val borderColorBlueValue = IntegerValue("BorderColorBlue", 0, 0, 255)

	private val nameFontValue = FontValue("NameFont", Fonts.font40)
	private val textFontValue = FontValue("TextFont", Fonts.font35)

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
		var targetEntity = if (tpAura.state && tpAura.maxTargetsValue.get() == 1 && tpAura.currentTarget != null) tpAura.currentTarget else ((moduleManager[KillAura::class.java] as KillAura).target ?: (moduleManager[Aimbot::class.java] as Aimbot).target) ?: (moduleManager[BowAimbot::class.java] as BowAimbot).target

		if (targetEntity == null && classProvider.isGuiHudDesigner(mc.currentScreen)) targetEntity = mc.thePlayer

		val minWidth = minWidthValue.get().toFloat()
		val height = heightValue.get().toFloat()

		if (targetEntity != null && targetEntity.entityAlive)
		{
			val isPlayer = classProvider.isEntityPlayer(targetEntity)

			if (isPlayer || !playerOnlyValue.get())
			{
				val headRenderSize = headSizeValue.get()

				val nameFont = nameFontValue.get()
				val textFont = textFontValue.get()

				val targetAbsorption = targetEntity.absorptionAmount
				var targetHealth = targetEntity.health + targetAbsorption
				var targetArmor = 0

				val targetMaxHealth = targetEntity.maxHealth + targetAbsorption

				// Damage/Heal animation color
				val damageColor = createRGB(damageAnimationColorRedValue.get(), damageAnimationColorGreenValue.get(), damageAnimationColorBlueValue.get())
				val healColor = createRGB(healAnimationColorRedValue.get(), healAnimationColorGreenValue.get(), healAnimationColorBlueValue.get())

				val dataWatcherBuilder = StringJoiner("\u00A7r | ", " | ", "\u00A7r").setEmptyValue("")

				if (targetEntity.invisible) dataWatcherBuilder.add("\u00A77\u00A7oInvisible")
				if (targetEntity.burning) dataWatcherBuilder.add("\u00A7cBurning")
				if (targetEntity.isEating) dataWatcherBuilder.add("\u00A7eEating")
				if (targetEntity.isSilent) dataWatcherBuilder.add("\u00A78Silent")

				val headBoxYSize = headRenderSize + 6F

				var textXOffset = 2
				var healthBarYOffset = height - 3F /*107F*/

				val name = targetEntity.displayName.formattedText
				val targetHealthPercentage = targetHealth / targetMaxHealth

				if (isPlayer)
				{
					val targetPlayer: IEntityPlayer = targetEntity.asEntityPlayer()

					val healthMethod = healthTypeValue.get().toLowerCase()
					if (healthMethod.equals("Mineplex", ignoreCase = true) || healthMethod.equals("Hive", ignoreCase = true)) targetHealth = EntityUtils.getPlayerHealthFromScoreboard(targetPlayer.gameProfile.name, isMineplex = healthTypeValue.get().equals("Mineplex", true)).toFloat()

					targetArmor = targetPlayer.totalArmorValue

					textXOffset = headRenderSize + 10
					healthBarYOffset = height - 6F /*104F*/
				}

				var textYOffset = 10

				// Reset easing
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

				val width = (textXOffset.toFloat() + nameFont.getStringWidth(name) + 10).coerceAtLeast(minWidth)

				// Draw Body Rect
				RenderUtils.drawBorderedRect(0F, 0F, width, height, borderWidthValue.get(), createRGB(borderColorRedValue.get(), borderColorGreenValue.get(), borderColorBlueValue.get()), -16777216)

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
				val healthGradationGap = gradationWidth / targetMaxHealth
				for (index in 1 until targetMaxHealth.roundToInt()) RenderUtils.drawRect(healthGradationGap * index + barWidthSubtractor, healthBarYOffset - 2, healthGradationGap * index + 1 + barWidthSubtractor, healthBarYOffset + 2, -16777216)

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

					repeat(5) { index ->
						val isHeldItem = index == 0

						val equipmentX = textXOffset + (4 - index) * 20 + if (isHeldItem) 5 else 0

						RenderUtils.drawRect(equipmentX, textYOffset, equipmentX + 16, textYOffset + 16, -12566464)

						val armor = targetEntity.getEquipmentInSlot(index) ?: return@repeat

						RenderUtils.glColor(Color.white) // Reset Color
						renderItem.zLevel = -147F
						renderItem.renderItemAndEffectIntoGUI(armor, equipmentX, textYOffset)
					}
				}

				RenderUtils.glColor(Color.white) // Reset Color

				easingHealth += ((targetHealth - easingHealth) / 2.0F.pow(10.0F - healthFadeSpeedValue.get())) * RenderUtils.deltaTime
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
				if (targetEntity.customNameTag.isNotBlank()) textFont.drawString("(${targetEntity.customNameTag})", textXOffset * reverseScale, (nameFont.fontHeight + 5) * reverseScale, Color.gray.rgb)

				// Health/Armor-related
				textFont.drawString("Health: $healthText | Absorption: ${if (targetAbsorption > 0) "\u00A7e" else "\u00A77"}${decimalFormat1.format(targetAbsorption.toLong())}\u00A7r | Armor: $armorText", scaledXPos, scaledYPos, 0xffffff)

				// Movement/Position-related
				textFont.drawString("Distance: ${distanceText}m | ${if (targetEntity.onGround) "\u00A7a" else "\u00A7c"}Ground\u00A7r | ${if (!targetEntity.sprinting) "\u00A7c" else "\u00A7a"}Sprinting\u00A7r | ${if (!targetEntity.sneaking) "\u00A7c" else "\u00A7a"}Sneaking\u00A7r", scaledXPos, scaledYPos + 12, 0xffffff)

				// Rotation-related
				textFont.drawString("Yaw: $yawText | Pitch: $pitchText | Velocity: [$velocityText]", scaledXPos, scaledYPos + 24, 0xffffff)

				// Hurt-related
				textFont.drawString("Hurt: ${if (targetEntity.hurtTime > 0) "\u00A7c" else "\u00A7a"}${targetEntity.hurtTime}\u00A7r | HurtResis: ${if (targetEntity.hurtResistantTime > 0) "\u00A7c" else "\u00A7a"}${targetEntity.hurtResistantTime}\u00A7r | Air: ${if (targetEntity.air > 0) "\u00A7c" else "\u00A7a"}${targetEntity.air}\u00A7r ", scaledXPos, scaledYPos + 34, 0xffffff)

				// Datawatcher-related
				textFont.drawString("EntityID: ${targetEntity.entityId}$dataWatcherBuilder", scaledXPos, scaledYPos + 44, 0xffffff)

				GL11.glScalef(reverseScale, reverseScale, reverseScale)
			}
		}

		lastTarget = targetEntity
		return Border(0F, 0F, minWidth, height)
	}
}
