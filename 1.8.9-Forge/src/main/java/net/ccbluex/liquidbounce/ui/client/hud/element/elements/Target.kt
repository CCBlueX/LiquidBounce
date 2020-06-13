package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * A target hud
 */
@ElementInfo(name = "Target")
class Target : Element() {

    // TODO: Add more options

    private var easingHealth: Float = 0F
    private var easingStep: Float = 0F

    override fun drawElement(): Border {
        val killaura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
        val target = killaura.target

        if (target is EntityPlayer) {
            // Easing
            val delta = RenderUtils.deltaTime
            val width = (target.health / target.maxHealth) * 118F

            // TODO: Fix flicking around
            if (easingHealth < width) {
                easingHealth = AnimationUtils.easeOut(easingStep, width) * width
                easingStep += delta / 0.4F
            } else if (easingHealth > width) {
                easingHealth = AnimationUtils.easeOut(easingStep, width) * width
                easingStep -= delta / 0.4F
            }

            // Draw
            RenderUtils.drawBorderedRect(0F, 0F, 118F, 36F, 3F, Color.BLACK.rgb, Color.BLACK.rgb)
            RenderUtils.drawRect(0F, 34F, easingHealth, 36F, Color.RED.rgb)

            Fonts.font40.drawString(target.name, 36, 3, 0xffffff)
            Fonts.font35.drawString("Distance: ${"%.2f".format(mc.thePlayer.getDistanceToEntityBox(target))}", 36, 15, 0xffffff)

            // Draw head
            val playerInfo = mc.netHandler.getPlayerInfo(target.uniqueID)
            if (playerInfo != null) {
                Fonts.font35.drawString("Ping: ${playerInfo.responseTime.coerceAtLeast(0)}", 36, 23, 0xffffff)

                val locationSkin = playerInfo.locationSkin

                drawHead(locationSkin, 30, 30)
            }
        }

        return Border(0F, 0F, 120F, 36F)
    }

    private fun drawHead(skin: ResourceLocation, width: Int, height: Int) {
        GL11.glColor4f(1F, 1F, 1F, 1F)
        mc.textureManager.bindTexture(skin)
        Gui.drawScaledCustomSizeModalRect(2, 2, 8F, 8F, 8, 8, width, height,
                64F, 64F)
    }

}