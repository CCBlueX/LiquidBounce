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
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
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

// TODO: Customizable color & Rainbow support
// TODO: Reduce overheads caused by nametags
@ModuleInfo(name = "NameTags", description = "Changes the scale of the nametags so you can always read them.", category = ModuleCategory.RENDER)
class NameTags : Module()
{
	private val healthValue = BoolValue("Health", true)
	private val pingValue = BoolValue("Ping", true)
	private val distanceValue = BoolValue("Distance", false)
	private val armorValue = BoolValue("Armor", true)
	private val clearNamesValue = BoolValue("ClearNames", false)
	private val fontValue = FontValue("Font", Fonts.font40)
	private val borderValue = BoolValue("Border", true)
	private val scaleValue = FloatValue("Scale", 1F, 1F, 4F)
	private val botValue = BoolValue("Bots", true)
	private val entityIDValue = BoolValue("EntityID", true)
	private val healthModeValue = ListValue("PlayerHealthGetMethod", arrayOf("Datawatcher", "Mineplex", "Hive"), "Datawatcher")
	private val stripColorsValue = BoolValue("StripColors", false)

	private val renderItemOverlaysValue = BoolValue("RenderItemOverlays", false)

	private val bodyRedValue = IntegerValue("BodyRed", 0, 0, 255)
	private val bodyGreenValue = IntegerValue("BodyGreen", 0, 0, 255)
	private val bodyBlueValue = IntegerValue("BodyBlue", 0, 0, 255)
	private val bodyAlphaValue = IntegerValue("BodyAlpha", 175, 0, 255)
	private val bodyRainbowValue = BoolValue("BodyRainbow", false)

	private val borderRedValue = IntegerValue("BorderRed", 255, 0, 255)
	private val borderGreenValue = IntegerValue("BorderGreen", 255, 0, 255)
	private val borderBlueValue = IntegerValue("BorderBlue", 255, 0, 255)
	private val borderAlphaValue = IntegerValue("BorderAlpha", 80, 0, 255)
	private val borderRainbowValue = BoolValue("BorderRainbow", false)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)
	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		glPushClientAttrib(GL_ALL_CLIENT_ATTRIB_BITS)
		glPushAttrib(GL_ALL_ATTRIB_BITS)
		glPushMatrix()

		// Disable lightning and depth test
		glDisable(GL_LIGHTING)
		glDisable(GL_DEPTH_TEST)

		glEnable(GL_LINE_SMOOTH)

		// Enable blend
		glEnable(GL_BLEND)
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		val provider = classProvider
		val glStateManager = classProvider.glStateManager

		val renderManager = mc.renderManager
		val renderItem = mc.renderItem
		val partialTicks = mc.timer.renderPartialTicks

		val murderDetector = LiquidBounce.moduleManager[MurderDetector::class.java] as MurderDetector
		val equipmentArrangement = if (Backend.MINECRAFT_VERSION_MINOR == 8) (0..4).toList().toIntArray() else intArrayOf(0, 1, 2, 3, 5, 4)

		val bot = botValue.get()
		val renderItemOverlays = renderItemOverlaysValue.get()

		theWorld.loadedEntityList.asSequence().filter { EntityUtils.isSelected(it, false) }.map(IEntity::asEntityLivingBase).map { it to AntiBot.isBot(theWorld, thePlayer, it) }.run { if (bot) this else filterNot(Pair<IEntityLivingBase, Boolean>::second) }.forEach { (entity, isBot) ->
			val name = entity.displayName.unformattedText
			renderNameTag(provider, renderManager, renderItem, glStateManager, murderDetector, thePlayer, entity, if (clearNamesValue.get()) ColorUtils.stripColor(name) else name, equipmentArrangement, isBot, partialTicks, renderItemOverlays)
		}

		glPopMatrix()
		glPopAttrib()
		glPopClientAttrib()

		// Reset color
		RenderUtils.resetColor()
	}

	private fun renderNameTag(provider: IClassProvider, renderManager: IRenderManager, renderItem: IRenderItem, glStateManager: IGlStateManager, murderDetector: MurderDetector, thePlayer: IEntity, entity: IEntityLivingBase, name: String, equipmentArrangement: IntArray, isBot: Boolean, partialTicks: Float, renderItemOverlays: Boolean)
	{
		val fontRenderer = fontValue.get()

		val entityIDEnabled = entityIDValue.get()
		val pingEnabled = pingValue.get()
		val distanceEnabled = distanceValue.get()
		val healthEnabled = healthValue.get()

		val stripColors = stripColorsValue.get()
		val healthMode = healthModeValue.get()
		val scaleValue = scaleValue.get()

		val borderEnabled = borderValue.get()

		val rainbowSpeed = rainbowSpeedValue.get()
		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()

		val bodyColor = if (bodyRainbowValue.get()) ColorUtils.rainbowRGB(alpha = bodyAlphaValue.get(), speed = rainbowSpeed, saturation = saturation, brightness = brightness) else ColorUtils.createRGB(bodyRedValue.get(), bodyGreenValue.get(), bodyBlueValue.get(), bodyAlphaValue.get())
		val borderColor = if (borderEnabled) if (borderRainbowValue.get()) ColorUtils.rainbowRGB(alpha = borderAlphaValue.get(), speed = rainbowSpeed, saturation = saturation, brightness = brightness) else ColorUtils.createRGB(borderRedValue.get(), borderGreenValue.get(), borderBlueValue.get(), borderAlphaValue.get()) else 0

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

		AWTFontRenderer.assumeNonVolatile = true

		// Draw NameTag
		val width = fontRenderer.getStringWidth(text) * 0.5f

		glDisable(GL_TEXTURE_2D)
		glEnable(GL_BLEND)

		val fontHeight = fontRenderer.fontHeight

		if (borderEnabled) quickDrawBorderedRect(-width - 2F, -2F, width + 4F, fontHeight + 2F, 2F, borderColor, bodyColor) else quickDrawRect(-width - 2F, -2F, width + 4F, fontHeight + 2F, bodyColor)

		glEnable(GL_TEXTURE_2D)

		fontRenderer.drawString(text, 1F - width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, 0xFFFFFF, true)

		AWTFontRenderer.assumeNonVolatile = false

		if (armorValue.get() && isPlayer)
		{
			val prevZLevel = renderItem.zLevel

			renderItem.zLevel = -147F

			equipmentArrangement.map { it to (entity.getEquipmentInSlot(it) ?: return@map null) }.filterNotNull().forEach { (index, equipment) ->
				RenderUtils.drawItemStack(renderItem, equipment, -50 + index * 20, -22, renderItemOverlays)
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
