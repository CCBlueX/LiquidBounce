/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawScaledCustomSizeModalRect
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.abs
import kotlin.math.pow

/**
 * A target hud
 */
@ElementInfo(name = "Target")
class Target : Element() {

    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))
    private val fadeSpeed by FloatValue("FadeSpeed", 2F, 1F..9F)
    private val absorption by BoolValue("Absorption", false)

    private var easingHealth = 0F
    private var lastTarget: Entity? = null

    override fun drawElement(): Border {
        val target = KillAura.target

        if (target is EntityPlayer) {
            val targetHealth = target.health + if (absorption) target.absorptionAmount else 0f

            if (target != lastTarget || easingHealth < 0 || easingHealth > target.maxHealth ||
                    abs(easingHealth - targetHealth) < 0.01) {
                easingHealth = targetHealth
            }

            val width = (38 + (target.name?.let(Fonts.font40::getStringWidth) ?: 0))
                    .coerceAtLeast(118)
                    .toFloat()

            // Draw rect box
            drawBorderedRect(0F, 0F, width, 36F, 3F, Color.BLACK.rgb, Color.BLACK.rgb)

            // Damage animation
            if (easingHealth > targetHealth.coerceAtMost(target.maxHealth))
                drawRect(0F, 34F, (easingHealth / target.maxHealth).coerceAtMost(1f) * width,
                        36F, Color(252, 185, 65).rgb)

            // Health bar
            drawRect(0F, 34F, (targetHealth / target.maxHealth).coerceAtMost(1f) * width,
                    36F, Color(252, 96, 66).rgb)

            // Heal animation
            if (easingHealth < targetHealth)
                drawRect((easingHealth / target.maxHealth).coerceAtMost(1f) * width, 34F,
                        (targetHealth / target.maxHealth).coerceAtMost(1f) * width, 36F, Color(44, 201, 144).rgb)

            easingHealth += ((targetHealth - easingHealth) / 2f.pow(10f - fadeSpeed)) * deltaTime

            target.name?.let { Fonts.font40.drawString(it, 36, 3, 0xffffff) }
            Fonts.font35.drawString("Distance: ${decimalFormat.format(mc.thePlayer.getDistanceToEntityBox(target))}", 36, 15, 0xffffff)

            // Draw info
            val playerInfo = mc.netHandler.getPlayerInfo(target.uniqueID)
            if (playerInfo != null) {
                Fonts.font35.drawString("Ping: ${playerInfo.responseTime.coerceAtLeast(0)}",
                        36, 24, 0xffffff)

                // Draw head
                val locationSkin = playerInfo.locationSkin
                drawHead(locationSkin, 30, 30)
            }
        }

        lastTarget = target
        return Border(0F, 0F, 120F, 36F)
    }

    private fun drawHead(skin: ResourceLocation, width: Int, height: Int) {
        glColor4f(1F, 1F, 1F, 1F)
        mc.textureManager.bindTexture(skin)
        drawScaledCustomSizeModalRect(2, 2, 8F, 8F, 8, 8, width, height,
                64F, 64F)
    }

}