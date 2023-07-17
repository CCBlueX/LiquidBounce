/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.projectile.EntityArrow
import java.awt.Color

object ItemESP : Module("ItemESP", ModuleCategory.RENDER) {
    private val mode by ListValue("Mode", arrayOf("Box", "OtherBox", "Glow"), "Box")

    private val glowRenderScale by FloatValue("Glow-Renderscale", 1f, 0.1f..2f) { mode == "Glow" }
    private val glowRadius by IntegerValue("Glow-Radius", 4, 1..5) { mode == "Glow" }
    private val glowFade by IntegerValue("Glow-Fade", 10, 0..30) { mode == "Glow" }
    private val glowTargetAlpha by FloatValue("Glow-Target-Alpha", 0f, 0f..1f) { mode == "Glow" }

    private val colorRainbow by BoolValue("Rainbow", true)
    private val colorRed by IntegerValue("R", 0, 0..255) { !colorRainbow }
    private val colorGreen by IntegerValue("G", 255, 0..255) { !colorRainbow }
    private val colorBlue by IntegerValue("B", 0, 0..255) { !colorRainbow }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {

        if(mc.theWorld == null) return

        mc.theWorld.loadedEntityList.filter { it is EntityItem || it is EntityArrow }.forEach { entity ->
            when (mode.lowercase()) {
                "box" -> drawEntityBox(entity, getColor(), true)
                "otherbox" -> drawEntityBox(entity, getColor(), false)
            }
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val mode = mode.lowercase()
        val partialTicks = event.partialTicks
        val shader = if (mode == "glow") GlowShader.GLOW_SHADER else null ?: return

        if(mc.theWorld == null) return

        shader.startDraw(partialTicks, glowRenderScale)
        try {
            mc.theWorld.loadedEntityList.filter { it is EntityItem || it is EntityArrow }.forEach { entity ->
                mc.renderManager.renderEntityStatic(entity, event.partialTicks, true)
            }
        } catch (ex: Exception) {
            LOGGER.error("An error occurred while rendering all item entities for shader esp", ex)
        }
        shader.stopDraw(getColor(), glowRadius, glowFade, glowTargetAlpha)
    }

    private fun getColor():Color{
        return if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)
    }

}