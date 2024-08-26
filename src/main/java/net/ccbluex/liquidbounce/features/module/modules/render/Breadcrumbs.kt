/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*

object Breadcrumbs : Module("Breadcrumbs", Category.RENDER, hideModule = false) {
    val colorRainbow by BoolValue("Rainbow", false)
        val colorRed by IntegerValue("R", 255, 0..255) { !colorRainbow }
        val colorGreen by IntegerValue("G", 179, 0..255) { !colorRainbow }
        val colorBlue by IntegerValue("B", 72, 0..255) { !colorRainbow }

    private val positions = LinkedList<DoubleArray>()

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val color = if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)

        synchronized(positions) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)

            mc.entityRenderer.disableLightmap()

            glBegin(GL_LINE_STRIP)
            glColor(color)

            val renderPosX = mc.renderManager.viewerPosX
            val renderPosY = mc.renderManager.viewerPosY
            val renderPosZ = mc.renderManager.viewerPosZ

            for (pos in positions)
                glVertex3d(pos[0] - renderPosX, pos[1] - renderPosY, pos[2] - renderPosZ)

            glColor4d(1.0, 1.0, 1.0, 1.0)
            glEnd()
            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        synchronized(positions) {
            positions += doubleArrayOf(mc.player.x, mc.player.boundingBox.minY, mc.player.z)
        }
    }

    override fun onEnable() {
        val thePlayer = mc.player ?: return

        synchronized(positions) {
            positions += doubleArrayOf(thePlayer.x, thePlayer.y + thePlayer.eyeHeight * 0.5f, thePlayer.z)

            positions += doubleArrayOf(thePlayer.x, thePlayer.y, thePlayer.z)
        }
    }

    override fun onDisable() {
        synchronized(positions) { positions.clear() }
    }
}