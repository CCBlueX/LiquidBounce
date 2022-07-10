/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.extensions.isSelected
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.RGBAColorValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11

@ModuleInfo(name = "Tracers", description = "Draws a line to targets around you.", category = ModuleCategory.RENDER)
class Tracers : Module()
{

    private val colorMode = ListValue("Color", arrayOf("Custom", "DistanceColor", "Rainbow"), "Custom", description = "Tracer line color mode")

    private val thicknessValue = FloatValue("Thickness", 2F, 1F, 5F, description = "Tracer line Thickness")

    private val colorGroup = ValueGroup("Color")
    private val colorValue = RGBAColorValue("Color", 0, 160, 255, 150, listOf("R", "G", "B", null), description = "Tracer line custom color")

    private val colorRainbowGroup = ValueGroup("Rainbow")
    private val colorRainbowEnabledValue = BoolValue("Enabled", true, "Rainbow", description = "Tracer line rainbow enabled")
    private val colorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed", description = "Tracer line rainbow speed")
    private val colorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "HSB-Saturation", description = "Tracer line rainbow HSB(HSV) saturation")
    private val colorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "HSB-Brightness", description = "Tracer line rainbow HSB(HSV) brightness(value)")

    private val botValue = BoolValue("Bots", true)

    private val interpolateValue = BoolValue("Interpolate", true, description = "Interpolate line target positions")

    init
    {
        colorRainbowGroup.addAll(colorRainbowEnabledValue, colorRainbowSpeedValue, colorRainbowSaturationValue, colorRainbowBrightnessValue)
        colorGroup.addAll(colorValue, colorRainbowGroup)
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(thicknessValue.get())
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        GL11.glBegin(GL11.GL_LINES)

        val colorMode = colorMode.get().toLowerCase()

        val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f

        val renderManager = mc.renderManager
        val renderPosX = renderManager.renderPosX
        val renderPosY = renderManager.renderPosY
        val renderPosZ = renderManager.renderPosZ

        val rotationYawRadians = thePlayer.rotationYaw.toRadians
        val rotationPitchRadians = thePlayer.rotationPitch.toRadians

        val eyeHeight = thePlayer.eyeHeight.toDouble()
        val eyeVector = Vec3(0.0, 0.0, 1.0).rotatePitch(-rotationPitchRadians).rotateYaw(-rotationYawRadians).plus(0.0, eyeHeight, 0.0)
        val eyeX = eyeVector.xCoord
        val eyeY = eyeVector.yCoord
        val eyeZ = eyeVector.zCoord

        val alpha = colorValue.getAlpha()
        val color = when
        {
            colorMode.equals("Custom", ignoreCase = true) -> colorValue.get()
            colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = alpha, speed = colorRainbowSpeedValue.get(), saturation = colorRainbowSaturationValue.get(), brightness = colorRainbowBrightnessValue.get())
            else -> ColorUtils.applyAlphaChannel(-1, alpha)
        }

        val bot = botValue.get()
        theWorld.loadedEntityList.filterIsInstance<EntityLivingBase>().filter { it.isSelected(false) }.filter { it != thePlayer }.run { if (bot) this else filter { !AntiBot.isBot(theWorld, thePlayer, it) } }.forEach { entity ->
            val distance = (thePlayer.getDistanceToEntity(entity) * 2f).toInt().coerceAtMost(255)

            val lastTickPosX = entity.lastTickPosX
            val lastTickPosY = entity.lastTickPosY
            val lastTickPosZ = entity.lastTickPosZ

            val x = (lastTickPosX + (entity.posX - lastTickPosX) * partialTicks - renderPosX)
            val y = (lastTickPosY + (entity.posY - lastTickPosY) * partialTicks - renderPosY)
            val z = (lastTickPosZ + (entity.posZ - lastTickPosZ) * partialTicks - renderPosZ)

            RenderUtils.glColor(when
            {
                entity is EntityPlayer && entity.isClientFriend() -> ColorUtils.createRGB(0, 0, 255, alpha)
                colorMode.equals("DistanceColor", ignoreCase = true) -> ColorUtils.createRGB(255 - distance, distance, 0, alpha)
                else -> color
            })

            GL11.glVertex3d(eyeX, eyeY, eyeZ)
            GL11.glVertex3d(x, y, z)
            GL11.glVertex3d(x, y, z)
            GL11.glVertex3d(x, y + entity.height, z)
        }

        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        RenderUtils.resetColor()
    }
}
