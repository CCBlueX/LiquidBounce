package net.vitox.particle.util

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.cos
import net.ccbluex.liquidbounce.utils.extensions.sin
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetColor
import org.lwjgl.opengl.GL11

object RenderUtils : MinecraftInstance()
{
    fun connectPoints(xOne: Float, yOne: Float, xTwo: Float, yTwo: Float)
    {
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.8f)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glLineWidth(0.5f)

        GL11.glBegin(GL11.GL_LINES)
        GL11.glVertex2f(xOne, yOne)
        GL11.glVertex2f(xTwo, yTwo)
        GL11.glEnd()

        resetColor()
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    fun drawCircle(x: Float, y: Float, radius: Float, color: Int)
    {
        glColor(color)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glPushMatrix()
        GL11.glLineWidth(1.0f)
        GL11.glBegin(GL11.GL_POLYGON)

        (0..360).map {
            val radians = it.toFloat().toRadians
            radians.sin to radians.cos
        }.forEach { GL11.glVertex2f(x + it.first * radius, y + it.second * radius) }

        GL11.glEnd()
        GL11.glPopMatrix()
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        resetColor()
    }
}
