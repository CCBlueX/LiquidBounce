/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
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
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.renderer.GlStateManager.*
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.ceil

@ModuleInfo(name = "NameTags", description = "Changes the scale of the nametags so you can always read them.", category = ModuleCategory.RENDER)
class NameTags : Module()
{
	companion object
	{
		private val DECIMAL_FORMAT = DecimalFormat("0.00")
	}

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
	private val opacityValue = IntegerValue("Opacity", 175, 0, 255)
	private val borderOpacityValue = IntegerValue("BorderOpacity", 80, 0, 255)

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		glPushAttrib(GL_ENABLE_BIT)
		glPushMatrix()

		// Disable lightning and depth test
		glDisable(GL_LIGHTING)
		glDisable(GL_DEPTH_TEST)

		glEnable(GL_LINE_SMOOTH)

		// Enable blend
		glEnable(GL_BLEND)
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

		for (entity in mc.theWorld!!.loadedEntityList)
		{
			if (!EntityUtils.isSelected(entity, false)) continue
			if (AntiBot.isBot(entity.asEntityLivingBase()) && !botValue.get()) continue
			val displayName = entity.displayName ?: continue

			renderNameTag(entity.asEntityLivingBase(), if (clearNamesValue.get()) ColorUtils.stripColor(displayName.unformattedText) ?: continue else displayName.unformattedText)
		}

		glPopMatrix()
		glPopAttrib()

		// Reset color
		glColor4f(1F, 1F, 1F, 1F)
	}

	private fun renderNameTag(entity: IEntityLivingBase, tag: String)
	{
		val thePlayer = mc.thePlayer ?: return

		val fontRenderer = fontValue.get()

		val murderDetector = LiquidBounce.moduleManager[MurderDetector::class.java] as MurderDetector

		val entityIDText = if (entityIDValue.get())
		{
			val entityID = entity.entityId

			"#$entityID "
		}
		else ""

		val bot = AntiBot.isBot(entity)
		val nameColor = when
		{
			bot -> "\u00A74\u00A7m" // DARK_RED + BOLD
			entity.invisible -> "\u00A78\u00A7o" // DARK_GRAY + ITALIC
			entity.sneaking -> "\u00A7o" // ITALIC
			else -> ""
		}

		val pingText = if (pingValue.get() && classProvider.isEntityPlayer(entity))
		{
			val ping = if (classProvider.isEntityPlayer(entity)) EntityUtils.getPing(entity) else 0

			(when
			{
				ping > 200 -> "\u00A7c" // ping higher than 200 -> RED
				ping > 100 -> "\u00A7e" // ping higher than 100 -> YELLOW
				ping <= 0 -> "\u00A77" // ping is lower than zero (unknown) -> GRAY
				else -> "\u00A7a" // ping is 0 ~ 100 -> GREEN
			}) + ping + "ms "
		}
		else ""

		val distanceText = if (distanceValue.get()) "\u00A77${DECIMAL_FORMAT.format(thePlayer.getDistanceToEntityBox(entity))}m " else ""

		val healthText = if (healthValue.get())
		{
			val health: Float = if (!classProvider.isEntityPlayer(entity) || healthModeValue.get().equals("Datawatcher", true)) entity.health
			else EntityUtils.getPlayerHealthFromScoreboard(entity.asEntityPlayer().gameProfile.name, healthModeValue.get().equals("Mineplex", true)).toFloat()

			val absorption = if (ceil(entity.absorptionAmount.toDouble()) > 0) entity.absorptionAmount else 0f
			val healthPercentage = (health + absorption) / entity.maxHealth * 100f
			val healthColor = when
			{
				healthPercentage <= 25 -> "\u00A7c" // under 25% -> RED
				healthPercentage <= 50 -> "\u00A7e" // under 50% -> YELLOW
				else -> "\u00A7a"
			}

			"\u00A77 $healthColor${DECIMAL_FORMAT.format(health)}${if (absorption > 0) "\u00A76+${DECIMAL_FORMAT.format(absorption)}$healthColor" else ""} HP \u00A77(${if (absorption > 0) "\u00A76" else healthColor}${DECIMAL_FORMAT.format(healthPercentage)}%\u00A77)"
		}
		else ""

		val botText = if (bot) " \u00A7c\u00A7l[BOT]" else ""
		val murderText = if (murderDetector.state && murderDetector.murders.contains(entity)) "\u00A75\u00A7l[MURDER]\u00A7r " else ""

		var text = "$murderText$entityIDText$distanceText$pingText\u00A77$nameColor$tag$healthText$botText"
		if (stripColorsValue.get()) text = ColorUtils.stripColor(text)!!

		// Push
		glPushMatrix()

		// Translate to player position
		val renderPartialTicks = mc.timer.renderPartialTicks
		val renderManager = mc.renderManager

		// Translate to player position with render pos and interpolate it
		glTranslated(entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * renderPartialTicks - renderManager.renderPosX, entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * renderPartialTicks - renderManager.renderPosY + entity.eyeHeight.toDouble() + 0.55, entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * renderPartialTicks - renderManager.renderPosZ)

		glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
		glRotatef(renderManager.playerViewX, 1F, 0F, 0F)

		// Scale
		var distance = thePlayer.getDistanceToEntity(entity) * 0.25f

		if (distance < 1F) distance = 1F

		val scale = distance / 100f * scaleValue.get()

		glScalef(-scale, -scale, scale)

		AWTFontRenderer.assumeNonVolatile = true

		// Draw NameTag
		val width = fontRenderer.getStringWidth(text) * 0.5f

		glDisable(GL_TEXTURE_2D)
		glEnable(GL_BLEND)

		val fontHeight = fontRenderer.fontHeight

		if (borderValue.get()) quickDrawBorderedRect(-width - 2F, -2F, width + 4F, fontHeight + 2F, 2F, Color(255, 255, 255, 90).rgb, Color(0, 0, 0, borderOpacityValue.get()).rgb)
		else quickDrawRect(-width - 2F, -2F, width + 4F, fontHeight + 2F, Color(0, 0, 0, opacityValue.get()).rgb)

		glEnable(GL_TEXTURE_2D)

		fontRenderer.drawString(text, 1F + -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, 0xFFFFFF, true)

		AWTFontRenderer.assumeNonVolatile = false

		if (armorValue.get() && classProvider.isEntityPlayer(entity))
		{
			mc.renderItem.zLevel = -147F

			val indices: IntArray = if (Backend.MINECRAFT_VERSION_MINOR == 8) (0..4).toList().toIntArray() else intArrayOf(0, 1, 2, 3, 5, 4)

			for (index in indices)
			{
				val equipmentInSlot = entity.getEquipmentInSlot(index) ?: continue

				mc.renderItem.renderItemAndEffectIntoGUI(equipmentInSlot, -50 + index * 20, -22)
			}

			enableAlpha()
			disableBlend()
			enableTexture2D()
		}

		// Pop
		glPopMatrix()
	}
}
