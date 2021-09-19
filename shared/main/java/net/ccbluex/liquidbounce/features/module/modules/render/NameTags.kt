/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.IClassProvider
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.api.minecraft.client.render.entity.IRenderItem
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IGlStateManager
import net.ccbluex.liquidbounce.api.minecraft.renderer.entity.IRenderManager
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.MurderDetector
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_2
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11.*
import kotlin.math.ceil

@ModuleInfo(name = "NameTags", description = "Changes the scale of the nametags so you can always read them.", category = ModuleCategory.RENDER)
class NameTags : Module()
{
	private val elementGroup = ValueGroup("Element")
	private val elementHealthValue = BoolValue("Health", true)
	private val elementPingValue = BoolValue("Ping", true)
	private val elementDistanceValue = BoolValue("Distance", false)
	private val elementArmorValue = BoolValue("Armor", true)
	private val elementBotValue = BoolValue("Bots", true)
	private val elementEntityIDValue = BoolValue("EntityID", true)

	private val scaleValue = FloatValue("Scale", 1F, 1F, 4F)

	private val healthModeValue = ListValue("PlayerHealthGetMethod", arrayOf("Datawatcher", "Mineplex", "Hive"), "Datawatcher")

	private val bodyColorGroup = ValueGroup("BodyColor")
	private val bodyColorValue = RGBAColorValue("Color", 0, 0, 0, 175, listOf("BodyRed", "BodyGreen", "BodyBlue", "BodyAlpha"))
	private val bodyColorRainbowValue = BoolValue("BodyRainbow", false)

	private val borderGroup = ValueGroup("Border")
	private val borderEnabledValue = BoolValue("Enabled", true, "Border")
	private val borderColorValue = RGBAColorValue("Color", 255, 255, 255, 80, listOf("BorderRed", "BorderGreen", "BorderBlue", "BorderAlpha"))
	private val borderColorRainbowValue = BoolValue("BorderRainbow", false)

	private val elementClearNamesValue = BoolValue("ClearNames", false)
	private val stripColorsValue = BoolValue("StripColors", false)

	private val rainbowGroup = object : ValueGroup("Rainbow")
	{
		override fun showCondition() = bodyColorRainbowValue.get() || borderEnabledValue.get() && borderColorRainbowValue.get()
	}
	private val rainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
	private val rainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "HSB-Saturation")
	private val rainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "HSB-Brightness")

	private val interpolateValue = BoolValue("Interpolate", true)

	private val elementfontValue = FontValue("Font", Fonts.font40)

	init
	{
		elementGroup.addAll(elementHealthValue, elementPingValue, elementDistanceValue, elementArmorValue, elementBotValue, elementEntityIDValue)
		bodyColorGroup.addAll(bodyColorValue, bodyColorRainbowValue)
		borderGroup.addAll(borderEnabledValue, borderColorValue, borderColorRainbowValue)
		rainbowGroup.addAll(rainbowSpeedValue, rainbowSaturationValue, rainbowBrightnessValue)
	}

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return
		val renderManager = mc.renderManager
		val renderItem = mc.renderItem
		val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f

		glPushClientAttrib(GL_ALL_CLIENT_ATTRIB_BITS)
		glPushAttrib(GL_ALL_ATTRIB_BITS)
		glPushMatrix()

		glDisable(GL_LIGHTING)
		glDisable(GL_DEPTH_TEST)
		glEnable(GL_LINE_SMOOTH)
		glEnable(GL_BLEND)
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

		val provider = classProvider
		val glStateManager = provider.glStateManager

		val murderDetector = LiquidBounce.moduleManager[MurderDetector::class.java] as MurderDetector
		val equipmentArrangement = if (Backend.MINECRAFT_VERSION_MINOR == 8) (0..4).toList().toIntArray() else intArrayOf(0, 1, 2, 3, 5, 4)

		val bot = elementBotValue.get()

		theWorld.loadedEntityList.asSequence().filter { EntityUtils.isSelected(it, false) }.map(IEntity::asEntityLivingBase).map { it to AntiBot.isBot(theWorld, thePlayer, it) }.run { if (bot) this else filterNot(Pair<IEntityLivingBase, Boolean>::second) }.forEach { (entity, isBot) ->
			val name = entity.displayName.unformattedText
			renderNameTag(provider, renderManager, renderItem, glStateManager, murderDetector, thePlayer, entity, if (elementClearNamesValue.get()) ColorUtils.stripColor(name) else name, equipmentArrangement, isBot, partialTicks)
		}

		RenderUtils.resetColor()

		glPopMatrix()
		glPopAttrib()
		glPopClientAttrib()
	}

	private fun renderNameTag(provider: IClassProvider, renderManager: IRenderManager, renderItem: IRenderItem, glStateManager: IGlStateManager, murderDetector: MurderDetector, thePlayer: IEntity, entity: IEntityLivingBase, name: String, equipmentArrangement: IntArray, isBot: Boolean, partialTicks: Float)
	{
		val fontRenderer = elementfontValue.get()

		val entityIDEnabled = elementEntityIDValue.get()
		val pingEnabled = elementPingValue.get()
		val distanceEnabled = elementDistanceValue.get()
		val healthEnabled = elementHealthValue.get()

		val stripColors = stripColorsValue.get()
		val healthMode = healthModeValue.get()
		val scaleValue = scaleValue.get()

		val borderEnabled = borderEnabledValue.get()

		val rainbowSpeed = rainbowSpeedValue.get()
		val saturation = rainbowSaturationValue.get()
		val brightness = rainbowBrightnessValue.get()

		val bodyColor = if (bodyColorRainbowValue.get()) ColorUtils.rainbowRGB(alpha = bodyColorValue.getAlpha(), speed = rainbowSpeed, saturation = saturation, brightness = brightness) else bodyColorValue.get()
		val borderColor = if (borderEnabled) if (borderColorRainbowValue.get()) ColorUtils.rainbowRGB(alpha = borderColorValue.getAlpha(), speed = rainbowSpeed, saturation = saturation, brightness = brightness) else borderColorValue.get() else 0

		val isPlayer = provider.isEntityPlayer(entity)

		val entityIDText = if (entityIDEnabled) "#${entity.entityId} " else ""

		val nameColor = when
		{
			isBot -> "\u00A74\u00A7l" // DARK_RED + STRIKETHROUGH
			entity.invisible -> "\u00A78\u00A7o" // DARK_GRAY + ITALIC
			entity.sneaking -> "\u00A7o" // ITALIC
			else -> ""
		}

		val pingText = if (pingEnabled && isPlayer)
		{
			val ping = EntityUtils.getPing(entity)

			"${
				when
				{
					ping > 200 -> "\u00A7c" // ping higher than 200 -> RED
					ping > 100 -> "\u00A7e" // ping higher than 100 -> YELLOW
					ping <= 0 -> "\u00A77" // ping is lower than zero (unknown) -> GRAY
					else -> "\u00A7a" // ping is 0 ~ 100 -> GREEN
				}
			} $ping ms\u00A7r "
		}
		else ""

		val distanceText = if (distanceEnabled) "\u00A77${DECIMALFORMAT_2.format(thePlayer.getDistanceToEntityBox(entity))}m\u00A7r " else ""

		val healthText = if (healthEnabled)
		{
			val health: Float = if (!isPlayer || healthMode.equals("Datawatcher", true)) entity.health
			else EntityUtils.getPlayerHealthFromScoreboard(entity.asEntityPlayer().gameProfile.name, isMineplex = healthMode.equals("Mineplex", true)).toFloat()

			val absorption = if (ceil(entity.absorptionAmount.toDouble()) > 0) entity.absorptionAmount else 0f
			val healthPercentage = (health + absorption) / entity.maxHealth * 100f
			val healthColor = when
			{
				healthPercentage <= 25 -> "\u00A7c" // under 25% -> RED
				healthPercentage <= 50 -> "\u00A7e" // under 50% -> YELLOW
				else -> "\u00A7a"
			}

			"\u00A77 $healthColor${DECIMALFORMAT_2.format(health)}${if (absorption > 0) "\u00A76+${DECIMALFORMAT_2.format(absorption)}$healthColor" else ""} HP \u00A77(${if (absorption > 0) "\u00A76" else healthColor}${DECIMALFORMAT_2.format(healthPercentage)}%\u00A77)\u00A7r"
		}
		else ""

		val botText = if (isBot) " \u00A7c\u00A7l[BOT]\u00A7r" else ""
		val murderText = if (murderDetector.state && murderDetector.murders.contains(entity)) "\u00A75\u00A7l[MURDER]\u00A7r " else ""

		var text = "$murderText$entityIDText$distanceText$pingText\u00A77$nameColor$name\u00A7r$healthText$botText"
		if (stripColors) text = ColorUtils.stripColor(text)

		// Push
		glPushMatrix()

		// Translate to player position
		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		// Translate to player position with render pos and interpolate it
		val lastTickPosX = entity.lastTickPosX
		val lastTickPosY = entity.lastTickPosY
		val lastTickPosZ = entity.lastTickPosZ
		glTranslated(lastTickPosX + (entity.posX - lastTickPosX) * partialTicks - renderPosX, lastTickPosY + (entity.posY - lastTickPosY) * partialTicks - renderPosY + entity.eyeHeight.toDouble() + 0.55, lastTickPosZ + (entity.posZ - lastTickPosZ) * partialTicks - renderPosZ)

		glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
		glRotatef(renderManager.playerViewX, 1F, 0F, 0F)

		// Scale
		var distance = thePlayer.getDistanceToEntity(entity) * 0.25f

		if (distance < 1F) distance = 1F

		val distanceScale = distance * 0.01f * scaleValue

		glScalef(-distanceScale, -distanceScale, distanceScale)

		assumeNonVolatile {
			// Draw NameTag
			val width = fontRenderer.getStringWidth(text) * 0.5f

			glDisable(GL_TEXTURE_2D)
			glEnable(GL_BLEND)

			val fontHeight = fontRenderer.fontHeight

			if (borderEnabled) quickDrawBorderedRect(-width - 2F, -2F, width + 4F, fontHeight + 2F, 2F, borderColor, bodyColor) else quickDrawRect(-width - 2F, -2F, width + 4F, fontHeight + 2F, bodyColor)

			glEnable(GL_TEXTURE_2D)

			fontRenderer.drawString(text, 1F - width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, 0xFFFFFF, true)
		}

		if (elementArmorValue.get() && isPlayer)
		{
			val prevZLevel = renderItem.zLevel

			renderItem.zLevel = -147F

			equipmentArrangement.map { it to (entity.getEquipmentInSlot(it) ?: return@map null) }.filterNotNull().forEach { (index, equipment) ->
				RenderUtils.drawItemStack(renderItem, equipment, -50 + index * 20, -22)
			}

			glStateManager.enableAlpha()
			glStateManager.disableBlend()
			glStateManager.enableTexture2D()

			renderItem.zLevel = prevZLevel
		}

		// Pop
		glPopMatrix()
	}
}
