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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.util.BlockPos
import java.awt.Color

@ModuleInfo(name = "ProphuntESP", description = "Allows you to see disguised players in PropHunt.", category = ModuleCategory.RENDER)
class ProphuntESP : Module() {
    val blocks: MutableMap<BlockPos, Long> = HashMap()

    private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "ShaderOutline", "ShaderGlow"), "OtherBox")
    private val shaderOutlineRadius = FloatValue("ShaderOutline-Radius", 1.35f, 1f, 2f)
    private val shaderGlowRadius = FloatValue("ShaderGlow-Radius", 2.3f, 2f, 3f)
    private val colorRedValue = IntegerValue("R", 0, 0, 255)
    private val colorGreenValue = IntegerValue("G", 90, 0, 255)
    private val colorBlueValue = IntegerValue("B", 255, 0, 255)
    private val colorRainbow = BoolValue("Rainbow", false)

    override fun onDisable() {
        synchronized(blocks) { blocks.clear() }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        val mode = modeValue.get()
        val color = if (colorRainbow.get()) rainbow() else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
        for (entity in mc.theWorld!!.loadedEntityList) {
            if(!mode.equals("Box", true) || !mode.equals("OtherBox", true)) break
            if (entity !is EntityFallingBlock) continue

            RenderUtils.drawEntityBox(entity, color, mode.equals("Box", true))
        }
        synchronized(blocks) {
            val iterator: MutableIterator<Map.Entry<BlockPos, Long>> = blocks.entries.iterator()

            while (iterator.hasNext()) {
                val entry = iterator.next()

                if (System.currentTimeMillis() - entry.value > 2000L) {
                    iterator.remove()
                    continue
                }

                RenderUtils.drawBlockBox(entry.key, color, mode.equals("Box", true))
            }
        }
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val mode = modeValue.get()
        val shader = when(mode) {
            "ShaderOutline" -> OutlineShader.OUTLINE_SHADER
            "ShaderGlow" -> GlowShader.GLOW_SHADER
            else -> null
        } ?: return

        shader.startDraw(event.partialTicks)
        try {
            for (entity in mc.theWorld!!.loadedEntityList) {
                if (entity !is EntityFallingBlock) continue
                mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
            }
        } catch (ex: Exception) {
            ClientUtils.getLogger().error("An error occurred while rendering all entities for shader esp", ex)
        }

        val color = if (colorRainbow.get()) rainbow() else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
        val radius = if (mode.equals("ShaderOutline", ignoreCase = true)) shaderOutlineRadius.get() else if (mode.equals("ShaderGlow", ignoreCase = true)) shaderGlowRadius.get() else 1f
        shader.stopDraw(color, radius, 1f)
    }
}
