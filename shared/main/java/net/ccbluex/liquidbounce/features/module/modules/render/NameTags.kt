/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
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
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.ceil

// TODO: Optimize nametags
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

		val theWorld = mc.theWorld ?: return

		val bot = botValue.get()
		theWorld.loadedEntityList.asSequence().filter { EntityUtils.isSelected(it, false) }.map(IEntity::asEntityLivingBase).filter { bot || !AntiBot.isBot(it) }.forEach { entity ->
			val name = (entity.displayName ?: return@forEach).unformattedText
			renderNameTag(entity, if (clearNamesValue.get()) ColorUtils.stripColor(name) ?: return@forEach else name)
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

		val provider = classProvider

		val pingText = if (pingValue.get() && provider.isEntityPlayer(entity))
		{
			val ping = if (provider.isEntityPlayer(entity)) EntityUtils.getPing(entity) else 0

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
			val health: Float = if (!provider.isEntityPlayer(entity) || healthModeValue.get().equals("Datawatcher", true)) entity.health else EntityUtils.getPlayerHealthFromScoreboard(entity.asEntityPlayer().gameProfile.name, isMineplex = healthModeValue.get().equals("Mineplex", true)).toFloat()

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
		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		// Translate to player position with render pos and interpolate it
		val lastTickPosX = entity.lastTickPosX
		val lastTickPosY = entity.lastTickPosY
		val lastTickPosZ = entity.lastTickPosZ
		glTranslated(lastTickPosX + (entity.posX - lastTickPosX) * renderPartialTicks - renderPosX, lastTickPosY + (entity.posY - lastTickPosY) * renderPartialTicks - renderPosY + entity.eyeHeight.toDouble() + 0.55, lastTickPosZ + (entity.posZ - lastTickPosZ) * renderPartialTicks - renderPosZ)

		glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
		glRotatef(renderManager.playerViewX, 1F, 0F, 0F)

		// Scale
		var distance = thePlayer.getDistanceToEntity(entity) * 0.25f

		if (distance < 1F) distance = 1F

		val scale = distance * 0.01f * scaleValue.get()

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

		if (armorValue.get() && provider.isEntityPlayer(entity))
		{
			val renderItem = mc.renderItem
			renderItem.zLevel = -147F

			// Used workaround because of IntArray doesn't have .mapNotNull() extension
			(if (Backend.MINECRAFT_VERSION_MINOR == 8) (0..4).toList().toIntArray() else intArrayOf(0, 1, 2, 3, 5, 4)).map { it to (entity.getEquipmentInSlot(it) ?: return@map null) }.filterNotNull().forEach { (index, equipment) -> renderItem.renderItemAndEffectIntoGUI(equipment, -50 + index * 20, -22) }

			val glStateManager = provider.glStateManager
			glStateManager.enableAlpha()
			glStateManager.disableBlend()
			glStateManager.enableTexture2D()
		}

		// Pop
		glPopMatrix()
	}
}
