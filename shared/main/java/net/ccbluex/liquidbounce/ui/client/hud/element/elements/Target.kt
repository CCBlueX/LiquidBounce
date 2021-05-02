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

	private val playerOnlyValue = BoolValue("PlayerOnly", true)
	private val textScaleValue = FloatValue("TextScale", 0.65F, 0.5F, 0.67F)

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
		val targetEntity = if (tpAura.state && tpAura.maxTargetsValue.get() == 1 && tpAura.currentTarget != null) tpAura.currentTarget else ((moduleManager[KillAura::class.java] as KillAura).target ?: (moduleManager[Aimbot::class.java] as Aimbot).target)

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

				var name = targetEntity.name // TODO: displayName

				val healthText = "$targetHealth (${decimalFormat.format(targetHealth / targetMaxHealth * 100.0)})"
				val armorText = "$targetArmor (${decimalFormat.format(targetArmor / 20.0 * 100.0)})"

				val distanceText = decimalFormat.format(thePlayer.getDistanceToEntityBox(targetEntity))
				val yawText = "${decimalFormat.format(targetEntity.rotationYaw % 360f)} (${StringUtils.getHorizontalFacingAdv(targetEntity.rotationYaw)})"
				val pitchText = decimalFormat.format(targetEntity.rotationPitch)

				val dataWatcherBuilder = StringJoiner("\u00A7r | ", "", "\u00A7r")

				if (targetEntity.invisible) dataWatcherBuilder.add("\u00A7oInvisible")

				if (targetEntity.burning) dataWatcherBuilder.add("\u00A7cBurning")

				// TODO: Add more DataWatcher options

				if (targetEntity != lastTarget)
				{
					if (easingHealth < 0 || easingHealth > targetMaxHealth || abs(easingHealth - targetHealth) < 0.01) easingHealth = targetHealth
					if (easingAbsorption < 0 || easingAbsorption > targetAbsorption || abs(easingAbsorption - targetAbsorption) < 0.01) easingAbsorption = targetAbsorption
					if (isPlayer && (easingArmor < 0 || easingArmor > 20 || abs(easingArmor - targetArmor) < 0.01)) easingArmor = targetArmor.toFloat()
				}

				var xShift = 2
				var healthBarYOffset = 108F

				if (isPlayer)
				{
					val targetPlayer: IEntityPlayer = targetEntity.asEntityPlayer()

					val healthMethod = healthGetMethod.get().toLowerCase()
					if (healthMethod.equals("Mineplex", ignoreCase = true) || healthMethod.equals("Hive", ignoreCase = true)) targetHealth = EntityUtils.getPlayerHealthFromScoreboard(targetPlayer.gameProfile.name, isMineplex = healthGetMethod.get().equals("Mineplex", true)).toFloat()

					targetArmor = targetPlayer.totalArmorValue
					name = targetPlayer.displayNameString

					xShift = 100
					healthBarYOffset = 105F
				}

				val healthColor = ColorUtils.getHealthColor(easingHealth, targetMaxHealth)

				val width = (xShift.toFloat() + Fonts.font60.getStringWidth(name)).coerceAtLeast(250.0F)

				// Draw Body Rect
				RenderUtils.drawBorderedRect(0F, 0F, width, 110F, borderWidth.get(), createRGB(borderColorRed.get(), borderColorGreen.get(), borderColorBlue.get(), 255), -16777216)

				// Draw Absorption
				RenderUtils.drawRect(((easingHealth / targetMaxHealth) * width) - ((easingAbsorption / targetMaxHealth) * width), healthBarYOffset - 2, (easingHealth / targetMaxHealth) * width, healthBarYOffset - 1, -256)

				// Draw Damage animation
				if (easingHealth > targetHealth) RenderUtils.drawRect(0F, healthBarYOffset, (easingHealth / targetMaxHealth) * width, healthBarYOffset + 2, damageColor)

				// Draw Health bar
				RenderUtils.drawRect(0F, healthBarYOffset, (targetHealth / targetMaxHealth) * width, healthBarYOffset + 2, healthColor.rgb)

				// Draw Heal animation
				if (easingHealth < targetHealth) RenderUtils.drawRect((easingHealth / targetMaxHealth) * width, healthBarYOffset, (targetHealth / targetMaxHealth) * width, healthBarYOffset + 2, healColor)

				// Draw Health Gradations
				val healthGradationGap = width / targetMaxHealthInt
				for (index in 1..targetMaxHealthInt) RenderUtils.drawRect(healthGradationGap * index, healthBarYOffset, healthGradationGap * index + 1, healthBarYOffset + 2, -16777216)

				if (isPlayer)
				{
					// Draw Head Box
					RenderUtils.drawRect(2F, 2F, xShift - 4F, 96F, -12566464)

					// Draw Total Armor bar
					RenderUtils.drawRect(0F, 109F, easingArmor * width * 0.05f, 110F, -16711681)

					// Draw Armor Gradations
					val armorGradationGap = width * 0.05f
					for (index in 1..20) RenderUtils.drawRect(armorGradationGap * index, 109F, armorGradationGap * index + 1, 110F, -16777216)

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

				val scale = textScaleValue.get()
				val reverseScale = 1.0F / scale

				val scaledXShift = xShift * reverseScale
				val scaledYPos = 60 * reverseScale

				GL11.glScalef(scale, scale, scale)

				// Health/Armor-related
				Fonts.font35.drawString("Health: $healthText | Absorption: $targetAbsorption | Armor: $armorText", scaledXShift, scaledYPos, 0xffffff)

				// Movement/Position-related
				Fonts.font35.drawString("Distance: ${distanceText}m | ${if (targetEntity.onGround) "\u00A7a" else "\u00A7c"}Ground\u00A7r | ${if (targetEntity.isAirBorne) "\u00A7a" else "\u00A7c"}AirBorne\u00A7r | ${if (!targetEntity.sprinting) "\u00A7c" else "\u00A7a"}Sprinting§r | ${if (!targetEntity.sneaking) "\u00A7c" else "\u00A7a"}Sneaking§r", scaledXShift, scaledYPos + 10, 0xffffff)

				// Rotation-related
				Fonts.font35.drawString("Yaw: $yawText | Pitch: $pitchText", scaledXShift, scaledYPos + 20, 0xffffff)

				// Hurt-related
				Fonts.font35.drawString("Hurt: ${if (targetEntity.hurtTime > 0) "\u00A7c" else "\u00A7a"}${targetEntity.hurtTime}\u00A7r | HurtResistantTime: ${if (targetEntity.hurtResistantTime > 0) "\u00A7c" else "\u00A7a"}${targetEntity.hurtResistantTime}\u00A7r ", scaledXShift, scaledYPos + 30, 0xffffff)

				// Datawatcher-related
				Fonts.font35.drawString("$dataWatcherBuilder ", scaledXShift, scaledYPos + 40, 0xffffff)

				GL11.glScalef(reverseScale, reverseScale, reverseScale)
			}
		}

		lastTarget = targetEntity
		return Border(0F, 0F, 250F, 110F)
	}
}
