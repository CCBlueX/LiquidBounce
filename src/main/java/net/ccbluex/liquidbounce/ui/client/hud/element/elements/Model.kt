/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11
import kotlin.math.abs
import kotlin.math.atan

/**
 * CustomHUD Model element
 *
 * Draw mini figure of your character to the HUD
 */
@ElementInfo(name = "Model")
class Model(x: Double = 40.0, y: Double = 100.0) : Element(x, y) {

    private val yawMode = ListValue("Yaw", arrayOf("Player", "Animation", "Custom"), "Animation")
    private val customYaw = FloatValue("CustomYaw", 0F, -180F, 180F)

    private val pitchMode = ListValue("Pitch", arrayOf("Player", "Custom"), "Player")
    private val customPitch = FloatValue("CustomPitch", 0F, -90F, 90F)

    private var rotate = 0F
    private var rotateDirection = false

    /**
     * Draw element
     */
    override fun drawElement(): Border {
        val yaw = when (yawMode.get().lowercase()) {
            "player" -> mc.thePlayer!!.rotationYaw
            "animation" -> {
                val delta = RenderUtils.deltaTime

                if (rotateDirection) {
                    if (rotate <= 70F) {
                        rotate += 0.12F * delta
                    } else {
                        rotateDirection = false
                        rotate = 70F
                    }
                } else {
                    if (rotate >= -70F) {
                        rotate -= 0.12F * delta
                    } else {
                        rotateDirection = true
                        rotate = -70F
                    }
                }

                rotate
            }
            "custom" -> customYaw.get()
            else -> 0F
        }

        var pitch = when (pitchMode.get().lowercase()) {
            "player" -> mc.thePlayer!!.rotationPitch
            "custom" -> customPitch.get()
            else -> 0F
        }

        pitch = if (pitch > 0) -pitch else abs(pitch)

        drawEntityOnScreen(yaw, pitch, mc.thePlayer!!)

        return Border(30F, 10F, -30F, -100F)
    }

    /**
     * Draw [entityLivingBase] to screen
     */
    private fun drawEntityOnScreen(yaw: Float, pitch: Float, entityLivingBase: EntityLivingBase) {
        GlStateManager.resetColor()
        GlStateManager.enableColorMaterial()
        GL11.glPushMatrix()
        GL11.glTranslatef(0F, 0F, 50F)
        GL11.glScalef(-50F, 50F, 50F)
        GL11.glRotatef(180F, 0F, 0F, 1F)

        val renderYawOffset = entityLivingBase.renderYawOffset
        val rotationYaw = entityLivingBase.rotationYaw
        val rotationPitch = entityLivingBase.rotationPitch
        val prevRotationYawHead = entityLivingBase.prevRotationYawHead
        val rotationYawHead = entityLivingBase.rotationYawHead

        GL11.glRotatef(135F, 0F, 1F, 0F)
        RenderHelper.enableStandardItemLighting()
        GL11.glRotatef(-135F, 0F, 1F, 0F)
        GL11.glRotatef(-atan(pitch / 40F) * 20.0F, 1F, 0F, 0F)

        entityLivingBase.renderYawOffset = atan(yaw / 40F) * 20F
        entityLivingBase.rotationYaw = atan(yaw / 40F) * 40F
        entityLivingBase.rotationPitch = -atan(pitch / 40F) * 20F
        entityLivingBase.rotationYawHead = entityLivingBase.rotationYaw
        entityLivingBase.prevRotationYawHead = entityLivingBase.rotationYaw

        GL11.glTranslatef(0F, 0F, 0F)

        val renderManager = mc.renderManager
        renderManager.playerViewY = 180F
        renderManager.isRenderShadow = false
        renderManager.renderEntityWithPosYaw(entityLivingBase, 0.0, 0.0, 0.0, 0F, 1F)
        renderManager.isRenderShadow = true

        entityLivingBase.renderYawOffset = renderYawOffset
        entityLivingBase.rotationYaw = rotationYaw
        entityLivingBase.rotationPitch = rotationPitch
        entityLivingBase.prevRotationYawHead = prevRotationYawHead
        entityLivingBase.rotationYawHead = rotationYawHead

        GL11.glPopMatrix()
        RenderHelper.disableStandardItemLighting()
        GlStateManager.disableRescaleNormal()
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit)
        GlStateManager.disableTexture2D()
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
        GlStateManager.resetColor()
    }
}