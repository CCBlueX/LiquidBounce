/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.ValueGroup
import org.lwjgl.opengl.GL11
import kotlin.math.abs
import kotlin.math.atan

/**
 * CustomHUD Model element
 *
 * Draw mini figure of your character to the HUD
 */
@ElementInfo(name = "Model")
class Model(x: Double = 40.0, y: Double = 100.0) : Element(x, y)
{
    private val yawGroup = ValueGroup("Yaw")
    private val yawModeValue = ListValue("Mode", arrayOf("Player", "Animation", "Custom"), "Animation", "Yaw")
    private val yawCustomYawValue = object : FloatValue("CustomYaw", 0F, -180F, 180F, "CustomYaw")
    {
        override fun showCondition() = yawModeValue.get().equals("Custom", ignoreCase = true)
    }

    private val pitchGroup = ValueGroup("Pitch")
    private val pitchModeValue = ListValue("Mode", arrayOf("Player", "Custom"), "Player", "Pitch")
    private val pitchCustomPitchValue = object : FloatValue("CustomPitch", 0F, -90F, 90F, "CustomPitch")
    {
        override fun showCondition() = pitchModeValue.get().equals("Custom", ignoreCase = true)
    }

    private var rotate = 0F
    private var rotateDirection = false

    init
    {
        yawGroup.addAll(yawModeValue, yawCustomYawValue)
        pitchGroup.addAll(pitchModeValue, pitchCustomPitchValue)
    }

    /**
     * Draw element
     */
    override fun drawElement(): Border?
    {
        val thePlayer = mc.thePlayer ?: return null

        val yaw = when (yawModeValue.get().toLowerCase())
        {
            "player" -> thePlayer.rotationYaw

            "animation" ->
            {
                val delta = RenderUtils.frameTime

                if (rotateDirection)
                {
                    if (rotate <= 70F)
                    {
                        rotate += 0.12F * delta
                    }
                    else
                    {
                        rotateDirection = false
                        rotate = 70F
                    }
                }
                else
                {
                    if (rotate >= -70F)
                    {
                        rotate -= 0.12F * delta
                    }
                    else
                    {
                        rotateDirection = true
                        rotate = -70F
                    }
                }

                rotate
            }

            "custom" -> yawCustomYawValue.get()
            else -> 0F
        }

        var pitch = when (pitchModeValue.get().toLowerCase())
        {
            "player" -> thePlayer.rotationPitch
            "custom" -> pitchCustomPitchValue.get()
            else -> 0F
        }

        pitch = if (pitch > 0) -pitch else abs(pitch)

        drawEntityOnScreen(yaw, pitch, thePlayer)

        return Border(30F, 10F, -30F, -100F)
    }

    /**
     * Draw [entityLivingBase] to screen
     */
    private fun drawEntityOnScreen(yaw: Float, pitch: Float, entityLivingBase: IEntityLivingBase)
    {
        val glStateManager = classProvider.glStateManager

        glStateManager.resetColor()
        glStateManager.enableColorMaterial()
        GL11.glPushMatrix()
        GL11.glTranslatef(0F, 0F, 50F)
        GL11.glScalef(-50F, 50F, 50F)
        GL11.glRotatef(180F, 0F, 0F, 1F)

        val renderYawOffset = entityLivingBase.renderYawOffset
        val rotationYaw = entityLivingBase.rotationYaw
        val rotationPitch = entityLivingBase.rotationPitch
        val prevRotationYawHead = entityLivingBase.prevRotationYawHead
        val rotationYawHead = entityLivingBase.rotationYawHead

        val func = functions

        GL11.glRotatef(135F, 0F, 1F, 0F)
        func.enableStandardItemLighting()
        GL11.glRotatef(-135F, 0F, 1F, 0F)
        GL11.glRotatef(-atan(pitch * 0.025f) * 20.0F, 1F, 0F, 0F)

        entityLivingBase.renderYawOffset = atan(yaw * 0.025f) * 20F
        entityLivingBase.rotationYaw = atan(yaw * 0.025f) * 40F
        entityLivingBase.rotationPitch = -atan(pitch * 0.025f) * 20F
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
        func.disableStandardItemLighting()
        glStateManager.disableRescaleNormal()
        func.setActiveTextureLightMapTexUnit()
        glStateManager.disableTexture2D()
        func.setActiveTextureDefaultTexUnit()
        glStateManager.resetColor()
    }
}
