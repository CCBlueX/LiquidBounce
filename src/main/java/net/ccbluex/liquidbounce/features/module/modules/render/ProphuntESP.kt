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
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.util.BlockPos
import java.awt.Color

object ProphuntESP : Module("ProphuntESP", ModuleCategory.RENDER) {
    val blocks = mutableMapOf<BlockPos, Long>()

    private val mode by ListValue("Mode", arrayOf("Box", "OtherBox", "Glow"), "OtherBox")

    private val glowRenderScale by FloatValue("Glow-Renderscale", 1f, 0.1f..2f) { mode == "Glow" }
    private val glowRadius by IntegerValue("Glow-Radius", 4, 1..5) { mode == "Glow" }
    private val glowFade by IntegerValue("Glow-Fade", 10, 0..30) { mode == "Glow" }
    private val glowTargetAlpha by FloatValue("Glow-Target-Alpha", 0f, 0f..1f) { mode == "Glow" }

    private val colorRainbow by BoolValue("Rainbow", false)
    private val colorRed by IntegerValue("R", 0, 0..255) { !colorRainbow }
    private val colorGreen by IntegerValue("G", 90, 0..255) { !colorRainbow }
    private val colorBlue by IntegerValue("B", 255, 0..255) { !colorRainbow }


    override fun onDisable() {
        synchronized(blocks) { blocks.clear() }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val mode = mode
        for (entity in mc.theWorld.loadedEntityList) {
            if (mode != "Box" && mode != "OtherBox") break
            if (entity !is EntityFallingBlock) continue

            drawEntityBox(entity, getColor(), mode == "Box")
        }
        synchronized(blocks) {
            val iterator = blocks.entries.iterator()

            while (iterator.hasNext()) {
                val entry = iterator.next()

                if (System.currentTimeMillis() - entry.value > 2000L) {
                    iterator.remove()
                    continue
                }

                drawBlockBox(entry.key, getColor(), mode == "Box")
            }
        }
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val mode = mode.lowercase()
        val shader = if (mode == "glow") GlowShader.GLOW_SHADER else null ?: return
        val color = if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)

        if(mc.theWorld == null) return

        shader.startDraw(event.partialTicks, glowRenderScale)
        try {
            mc.theWorld.loadedEntityList.filterNot{ it !is EntityFallingBlock }.forEach { entity ->
                mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
            }
        } catch (ex: Exception) {
            LOGGER.error("An error occurred while rendering all entities for shader esp", ex)
        }


        shader.stopDraw(color, glowRadius, glowFade, glowTargetAlpha)
    }

    private fun getColor() = if (colorRainbow) rainbow() else Color(colorRed, colorGreen, colorBlue)

}
