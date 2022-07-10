/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.MurderDetector
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isSelected
import net.ccbluex.liquidbounce.utils.extensions.ping
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_2
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.*
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

    private val yPosValue = FloatValue("YPos", 0.55f, 0.4f, 2f)

    private val healthModeValue = ListValue("PlayerHealthGetMethod", arrayOf("Metadata", "Mineplex", "Hive"), "Metadata")

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

    private var renderQueue = mapOf<Int, NameTag>()

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val bot = elementBotValue.get()
        val murderDetector = LiquidBounce.moduleManager[MurderDetector::class.java] as MurderDetector

        renderQueue = theWorld.loadedEntityList.asSequence().filterIsInstance<EntityLivingBase>().filter(Entity::isSelected).map { it to AntiBot.isBot(theWorld, thePlayer, it) }.run { if (bot) this else filterNot(Pair<EntityLivingBase, Boolean>::second) }.map { (entity, isBot) ->
            serializeInformations(thePlayer, entity, isBot, murderDetector)
        }.toMap()
    }

    private fun serializeInformations(thePlayer: Entity, entity: EntityLivingBase, isBot: Boolean, murderDetector: MurderDetector): Pair<Int, NameTag>
    {
        val fontRenderer = elementfontValue.get()

        val entityIDEnabled = elementEntityIDValue.get()
        val pingEnabled = elementPingValue.get()
        val distanceEnabled = elementDistanceValue.get()
        val healthEnabled = elementHealthValue.get()

        val stripColors = stripColorsValue.get()
        val healthMode = healthModeValue.get()

        // Murder
        val murderText = if (murderDetector.state && murderDetector.murders.contains(entity)) "\u00A75\u00A7l[MURDER]\u00A7r " else ""

        // EntityID
        val entityIDText = if (entityIDEnabled) "#${entity.entityId} " else ""

        // Distance
        val distanceText = if (distanceEnabled) "\u00A77${DECIMALFORMAT_2.format(thePlayer.getDistanceToEntityBox(entity))}m\u00A7r " else ""

        // Ping
        val pingText = if (pingEnabled && entity is EntityPlayer)
        {
            val ping = entity.ping
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

        // Name
        val nameColor = when
        {
            isBot -> "\u00A74\u00A7l" // DARK_RED + STRIKETHROUGH
            entity.isInvisible -> "\u00A78\u00A7o" // DARK_GRAY + ITALIC
            entity.isSneaking -> "\u00A7o" // ITALIC
            else -> ""
        }

        var name = entity.displayName.unformattedText
        if (elementClearNamesValue.get()) name = ColorUtils.stripColor(name)

        // Health
        val healthText = if (healthEnabled)
        {
            val health = if (entity !is EntityPlayer || healthMode.equals("Metadata", true)) entity.health else EntityUtils.getPlayerHealthFromScoreboard(entity.gameProfile.name, isMineplex = healthMode.equals("Mineplex", true)).toFloat()

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

        // Bot
        val botText = if (isBot) " \u00A7c\u00A7l[BOT]\u00A7r" else ""

        var text = "$murderText$entityIDText$distanceText$pingText\u00A77$nameColor$name\u00A7r$healthText$botText"
        if (stripColors) text = ColorUtils.stripColor(text)
        return entity.entityId to NameTag(text, fontRenderer.getStringWidth(text) * 0.5f, thePlayer.getDistanceToEntity(entity), (0..4).map { it to (entity.getEquipmentInSlot(it) ?: return@map null) }.filterNotNull())
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent)
    {
        val theWorld = mc.theWorld ?: return
        val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f

        glPushAttrib(GL_ENABLE_BIT)
        glPushMatrix()

        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_LINE_SMOOTH)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        theWorld.loadedEntityList.asSequence().mapNotNull { it to (renderQueue[it.entityId] ?: return@mapNotNull null) }.forEach { (entity, text) ->
            renderNameTag(entity as EntityLivingBase, text, partialTicks)
        }

        RenderUtils.resetColor()

        glPopMatrix()
        glPopAttrib()
    }

    private fun renderNameTag(entity: EntityLivingBase, tag: NameTag, partialTicks: Float)
    {
        val renderManager = mc.renderManager
        val renderItem = mc.renderItem

        val fontRenderer = elementfontValue.get()
        val scaleValue = scaleValue.get()
        val ypos = yPosValue.get()

        val borderEnabled = borderEnabledValue.get()

        val rainbowSpeed = rainbowSpeedValue.get()
        val saturation = rainbowSaturationValue.get()
        val brightness = rainbowBrightnessValue.get()

        val bodyColor = if (bodyColorRainbowValue.get()) ColorUtils.rainbowRGB(alpha = bodyColorValue.getAlpha(), speed = rainbowSpeed, saturation = saturation, brightness = brightness) else bodyColorValue.get()
        val borderColor = if (borderEnabled) if (borderColorRainbowValue.get()) ColorUtils.rainbowRGB(alpha = borderColorValue.getAlpha(), speed = rainbowSpeed, saturation = saturation, brightness = brightness) else borderColorValue.get() else 0

        // Push
        glPushMatrix()

        val lastTickPosX = entity.lastTickPosX
        val lastTickPosY = entity.lastTickPosY
        val lastTickPosZ = entity.lastTickPosZ
        glTranslated(lastTickPosX + (entity.posX - lastTickPosX) * partialTicks - renderManager.renderPosX, lastTickPosY + (entity.posY - lastTickPosY) * partialTicks - renderManager.renderPosY + entity.eyeHeight.toDouble() + ypos, lastTickPosZ + (entity.posZ - lastTickPosZ) * partialTicks - renderManager.renderPosZ)

        glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(renderManager.playerViewX, 1F, 0F, 0F)

        // Scale
        var distance = tag.distance * 0.25f
        if (distance < 1F) distance = 1F
        val distanceScale = distance * 0.01f * scaleValue
        glScalef(-distanceScale, -distanceScale, distanceScale)

        // Draw NameTag
        assumeNonVolatile {
            val width = tag.textWidth

            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)

            val fontHeight = fontRenderer.FONT_HEIGHT

            if (borderEnabled) quickDrawBorderedRect(-width - 2F, -2F, width + 4F, fontHeight + 2F, 2F, borderColor, bodyColor) else quickDrawRect(-width - 2F, -2F, width + 4F, fontHeight + 2F, bodyColor)

            glEnable(GL_TEXTURE_2D)

            fontRenderer.drawString(tag.text, 1F - width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, 0xFFFFFF, true)
        }

        // Draw armors
        if (elementArmorValue.get())
        {
            renderItem.zLevel = -147F

            tag.equipments.forEach { (index, equipment) ->
                RenderUtils.drawItemStack(equipment, -50 + index * 20, -22)
            }

            GlStateManager.enableAlpha()
            GlStateManager.disableBlend()
            GlStateManager.enableTexture2D()
            GlStateManager.disableDepth()
        }

        // Pop
        glPopMatrix()
    }
}

data class NameTag(val text: String, val textWidth: Float, val distance: Float, val equipments: List<Pair<Int, ItemStack>>)
