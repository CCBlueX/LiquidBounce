/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import com.mojang.blaze3d.platform.GlStateManager.*
import net.minecraft.client.render.OpenGlHelper
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.entity.LivingEntity
import org.lwjgl.opengl.GL11.*
import kotlin.math.abs
import kotlin.math.atan

/**
 * CustomHUD Model element
 *
 * Draw mini figure of your character to the HUD
 */
@ElementInfo(name = "Model")
class Model(x: Double = 40.0, y: Double = 100.0) : Element(x, y) {

    private val yawMode by ListValue("Yaw", arrayOf("Player", "Animation", "Custom"), "Animation")
        private val customYaw by FloatValue("CustomYaw", 0F, -180F..180F) { yawMode == "Custom" }

    private val pitchMode by ListValue("Pitch", arrayOf("Player", "Custom"), "Player")
        private val customPitch by FloatValue("CustomPitch", 0F, -90F..90F) { pitchMode == "Custom" }

    private var rotate = 0F
    private var rotateDirection = false

    /**
     * Draw element
     */
    override fun drawElement(): Border {
        val yaw = when (yawMode.lowercase()) {
            "player" -> mc.player.yaw
            "animation" -> {
                val delta = deltaTime

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
            "custom" -> customYaw
            else -> 0F
        }

        var pitch = when (pitchMode.lowercase()) {
            "player" -> mc.player.pitch
            "custom" -> customPitch
            else -> 0F
        }

        pitch = if (pitch > 0) -pitch else abs(pitch)

        drawEntityOnScreen(yaw, pitch, mc.player)

        return Border(30F, 10F, -30F, -100F)
    }

    /**
     * Draw [LivingEntity] to screen
     */
    private fun drawEntityOnScreen(yaw: Float, pitch: Float, LivingEntity: LivingEntity) {
        resetColor()
        enableColorMaterial()
        glPushMatrix()
        glTranslatef(0F, 0F, 50F)
        glScalef(-50F, 50F, 50F)
        glRotatef(180F, 0F, 0F, 1F)

        val renderYawOffset = LivingEntity.renderYawOffset
        val rotationYaw = LivingEntity.rotationYaw
        val rotationPitch = LivingEntity.rotationPitch
        val prevRotationYawHead = LivingEntity.prevRotationYawHead
        val rotationYawHead = LivingEntity.rotationYawHead

        glRotatef(135F, 0F, 1F, 0F)
        DiffuseLighting.enableStandardItemLighting()
        glRotatef(-135F, 0F, 1F, 0F)
        glRotatef(-atan(pitch / 40F) * 20f, 1F, 0F, 0F)

        LivingEntity.renderYawOffset = atan(yaw / 40F) * 20F
        LivingEntity.rotationYaw = atan(yaw / 40F) * 40F
        LivingEntity.rotationPitch = -atan(pitch / 40F) * 20F
        LivingEntity.rotationYawHead = LivingEntity.rotationYaw
        LivingEntity.prevRotationYawHead = LivingEntity.rotationYaw

        glTranslatef(0F, 0F, 0F)

        val renderManager = mc.entityRenderManager
        renderManager.yaw = 180F
        renderManager.isRenderShadow = false
        renderManager.renderEntityWithPosYaw(LivingEntity, 0.0, 0.0, 0.0, 0F, 1F)
        renderManager.isRenderShadow = true

        LivingEntity.renderYawOffset = renderYawOffset
        LivingEntity.rotationYaw = rotationYaw
        LivingEntity.rotationPitch = rotationPitch
        LivingEntity.prevRotationYawHead = prevRotationYawHead
        LivingEntity.rotationYawHead = rotationYawHead

        glPopMatrix()
        DiffuseLighting.disableStandardItemLighting()
        disableRescaleNormal()
        setActiveTexture(OpenGlHelper.lightmapTexUnit)
        disableTexture2D()
        setActiveTexture(OpenGlHelper.defaultTexUnit)
        resetColor()
    }
}