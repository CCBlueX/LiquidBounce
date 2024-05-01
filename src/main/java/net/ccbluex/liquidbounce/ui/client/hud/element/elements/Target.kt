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
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
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

    private val borderStrength by FloatValue("Border-Strength", 3F, 1F..5F)

    private val backgroundMode by ListValue("Background-Color", arrayOf("Custom", "Rainbow"), "Custom")
    private val backgroundRed by IntegerValue("Background-R", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundGreen by IntegerValue("Background-G", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundBlue by IntegerValue("Background-B", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundAlpha by IntegerValue("Background-Alpha", 255, 0..255) { backgroundMode == "Custom" }

    private val borderMode by ListValue("Border-Color", arrayOf("Custom", "Rainbow"), "Custom")
    private val borderRed by IntegerValue("Border-R", 0, 0..255) { borderMode == "Custom" }
    private val borderGreen by IntegerValue("Border-G", 0, 0..255) { borderMode == "Custom" }
    private val borderBlue by IntegerValue("Border-B", 0, 0..255) { borderMode == "Custom" }
    private val borderAlpha by IntegerValue("Border-Alpha", 255, 0..255) { borderMode == "Custom" }

    private val textRed by IntegerValue("Text-R", 255, 0..255)
    private val textGreen by IntegerValue("Text-G", 255, 0..255)
    private val textBlue by IntegerValue("Text-B", 255, 0..255)
    private val textAlpha by IntegerValue("Text-Alpha", 255, 0..255)

    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { backgroundMode == "Rainbow" }

    private val titleFont by FontValue("TitleFont", Fonts.font40)
    private val bodyFont by FontValue("BodyFont", Fonts.font35)
    private val textShadow by BoolValue("TextShadow", false)

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

            val width = (40f + (target.name?.let(Fonts.font40::getStringWidth) ?: 0)).coerceAtLeast(118f)

            val backgroundCustomColor = Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha).rgb
            val borderCustomColor = Color(borderRed, borderGreen, borderBlue, borderAlpha).rgb
            val textCustomColor = Color(textRed, textGreen, textBlue, textAlpha).rgb

            val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
            val rainbowX = if (rainbowX == 0f) 0f else 1f / rainbowX
            val rainbowY = if (rainbowY == 0f) 0f else 1f / rainbowY

            // Draw rect box
            RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                drawRoundedBorderRect(
                    0F, 0F, width, 40F, borderStrength,
                    when (backgroundMode) {
                        "Rainbow" -> 0
                        else -> backgroundCustomColor
                    },
                    borderCustomColor,
                    roundedRectRadius
                )
            }

            // Health bar
            val healthBarWidth = (targetHealth / target.maxHealth) * (width - 6f)
            drawRectNew(3F, 34F, 3f + healthBarWidth, 36F, healthColor.rgb)

            // Easing health update
            easingHealth += ((targetHealth - easingHealth) / 2f.pow(10f - fadeSpeed)) * deltaTime
            val easingHealthWidth = (easingHealth / target.maxHealth) * (width - 6f)

            // Heal animation, only animate from the right side
            if (easingHealth < targetHealth) {
                drawRectNew(3f + easingHealthWidth, 34F, 3f + healthBarWidth, 36F, Color(44, 201, 144).rgb)
            }

            // Damage animation, only animate from the right side
            if (easingHealth > targetHealth) {
                drawRectNew(3f + healthBarWidth, 34F, 3f + easingHealthWidth, 36F, Color(252, 185, 65).rgb)
            }

            target.name?.let {
                titleFont.drawString(
                    it, 36F, 5F,
                    textCustomColor,
                    textShadow
                )
            }

            bodyFont.drawString(
                "Distance: ${decimalFormat.format(mc.thePlayer.getDistanceToEntityBox(target))}",
                36F,
                15F,
                textCustomColor,
                textShadow
            )

            // Draw info
            val playerInfo = mc.netHandler.getPlayerInfo(target.uniqueID)
            if (playerInfo != null) {
                bodyFont.drawString(
                    "Ping: ${playerInfo.responseTime.coerceAtLeast(0)}",
                    36F,
                    24F,
                    textCustomColor,
                    textShadow
                )

                // Draw head
                val locationSkin = playerInfo.locationSkin
                drawHead(locationSkin, 30, 30)
            }
        }

        lastTarget = target
        return Border(0F, 0F, 116F, 40F)
    }

    private fun drawHead(skin: ResourceLocation, width: Int, height: Int) {
        glColor4f(1F, 1F, 1F, 1F)
        mc.textureManager.bindTexture(skin)
        drawScaledCustomSizeModalRect(4, 4, 8F, 8F, 8, 8, width - 2, height - 2, 64F, 64F)
    }

}