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
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "Tracers", description = "Draws a line to targets around you.", category = ModuleCategory.RENDER)
class Tracers : Module() {
    private val thicknessValue = FloatValue("Thickness", 2F, 1F, 5F)
    private val distanceColorValue = BoolValue("DistanceColor", false)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(thicknessValue.get())
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        GL11.glBegin(GL11.GL_LINES)

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity != null && entity != mc.thePlayer && EntityUtils.isSelected(entity, false)) {
                var dist = (mc.thePlayer.getDistanceToEntity(entity) * 2).toInt()

                if (dist > 255) dist = 255

                val color = when {
                    EntityUtils.isFriend(entity) -> Color(0, 0, 255, 150)
                    distanceColorValue.get() -> Color(255 - dist, dist, 0, 150)
                    else -> Color(255, 255, 255, 150)
                }

                drawTraces(entity, color)
            }
        }

        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        GlStateManager.resetColor()
    }

    private fun drawTraces(entity: Entity, color: Color) {
        val x = (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosX)
        val y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosY)
        val z = (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosZ)

        val eyeVector = Vec3(0.0, 0.0, 1.0)
                .rotatePitch((-Math.toRadians(mc.thePlayer.rotationPitch.toDouble())).toFloat())
                .rotateYaw((-Math.toRadians(mc.thePlayer.rotationYaw.toDouble())).toFloat())

        RenderUtils.glColor(color)

        GL11.glVertex3d(eyeVector.xCoord, mc.thePlayer.getEyeHeight().toDouble() + eyeVector.yCoord, eyeVector.zCoord)
        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x, y + entity.height, z)
    }
}
