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
import net.ccbluex.liquidbounce.utils.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRectNew
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
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

    private val roundedRectRadius by FloatValue("Rounded-Radius", 3F, 0F..5F)

    private val fadeSpeed by FloatValue("FadeSpeed", 2F, 1F..9F)
    private val absorption by BoolValue("Absorption", true)
    private val healthFromScoreboard by BoolValue("HealthFromScoreboard", true)

    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))
    private var easingHealth = 0F
    private var lastTarget: Entity? = null

    override fun drawElement(): Border {
        val target = KillAura.target

        if (KillAura.handleEvents() && target is EntityPlayer) {
            val targetHealth = getHealth(target, healthFromScoreboard, absorption)

            // Calculate health color based on entity's health
            val healthColor = when {
                target.health <= 0 -> Color(255, 0, 0)
                else -> {
                    val healthRatio = (targetHealth / target.maxHealth).coerceIn(0.0F, 1.0F)
                    val red = (255 * (1 - healthRatio)).toInt()
                    val green = (255 * healthRatio).toInt()
                    Color(red, green, 0)
                }
            }

            if (target != lastTarget || easingHealth < 0 || easingHealth > target.maxHealth ||
                    abs(easingHealth - targetHealth) < 0.01
            ) {
                easingHealth = targetHealth
            }

            val width = (38f + (target.name?.let(Fonts.font40::getStringWidth) ?: 0)).coerceAtLeast(118f)

            // Draw rect box
            drawRoundedBorderRect(0F, 0F, width, 36F, 3F, Color.BLACK.rgb, Color.BLACK.rgb, roundedRectRadius)

            // Damage animation
            if (easingHealth > targetHealth.coerceAtMost(target.maxHealth))
                drawRectNew(0F, 34F, (easingHealth / target.maxHealth).coerceAtMost(1f) * width, 36F, Color(252, 185, 65).rgb)

            // Health bar
            drawRectNew(0F, 34F, (targetHealth / target.maxHealth).coerceAtMost(1f) * width, 36F, healthColor.rgb)

            // Heal animation
            if (easingHealth < targetHealth)
                drawRectNew((easingHealth / target.maxHealth).coerceAtMost(1f) * width, 34F,
                        (targetHealth / target.maxHealth).coerceAtMost(1f) * width, 36F, Color(44, 201, 144).rgb)

            easingHealth += ((targetHealth - easingHealth) / 2f.pow(10f - fadeSpeed)) * deltaTime

            target.name?.let { Fonts.font40.drawString(it, 36, 3, 0xffffff) }
            Fonts.font35.drawString("Distance: ${decimalFormat.format(mc.thePlayer.getDistanceToEntityBox(target))}", 36, 15, 0xffffff)

            // Draw info
            val playerInfo = mc.netHandler.getPlayerInfo(target.uniqueID)
            if (playerInfo != null) {
                Fonts.font35.drawString("Ping: ${playerInfo.responseTime.coerceAtLeast(0)}", 36, 24, 0xffffff)

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
        drawScaledCustomSizeModalRect(2, 2, 8F, 8F, 8, 8, width, height, 64F, 64F)
    }

}