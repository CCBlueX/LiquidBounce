/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.renderer.GlStateManager.resetColor
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.sqrt

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "SpeedGraph")
class SpeedGraph(x: Double = 75.0, y: Double = 110.0, scale: Float = 1F,
                 side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side) {

    private val yMultiplier = FloatValue("yMultiplier", 7F, 1F..20F)
    private val height = IntegerValue("Height", 50, 30..150)
    private val width = IntegerValue("Width", 150, 100..300)
    private val thickness = FloatValue("Thickness", 2F, 1F..3F)
    private val colorRedValue = IntegerValue("R", 0, 0..255)
    private val colorGreenValue = IntegerValue("G", 111, 0..255)
    private val colorBlueValue = IntegerValue("B", 255, 0..255)

    private val speedList = ArrayList<Double>()
    private var lastTick = -1

    override fun drawElement(): Border {
        val width = width.get()

        val player = mc.thePlayer

        if (lastTick != player.ticksExisted) {
            lastTick = player.ticksExisted
            val z2 = player.posZ
            val z1 = player.prevPosZ
            val x2 = player.posX
            val x1 = player.prevPosX
            var speed = sqrt((z2 - z1) * (z2 - z1) + (x2 - x1) * (x2 - x1))
            if (speed < 0)
                speed = -speed

            speedList += speed
            while (speedList.size > width) {
                speedList.removeAt(0)
            }
        }
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glEnable(GL_LINE_SMOOTH)
        glLineWidth(thickness.get())
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        glBegin(GL_LINES)

        val size = speedList.size

        val start = (if (size > width) size - width else 0)
        for (i in start until size - 1) {
            val y = speedList[i] * 10 * yMultiplier.get()
            val y1 = speedList[i + 1] * 10 * yMultiplier.get()

            glColor(Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), 255))
            glVertex2d(i.toDouble() - start, height.get() + 1 - y.coerceAtMost(height.get().toDouble()))
            glVertex2d(i + 1.0 - start, height.get() + 1 - y1.coerceAtMost(height.get().toDouble()))
        }

        glEnd()

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_DEPTH_TEST)
        glDepthMask(true)
        glDisable(GL_BLEND)
        resetColor()

        return Border(0F, 0F, width.toFloat(), height.get().toFloat() + 2)
    }
}