/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
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
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import java.awt.Color
import java.util.*

@ModuleInfo(name = "ProphuntESP", description = "Allows you to see disguised players in PropHunt.", category = ModuleCategory.RENDER)
class ProphuntESP : Module() {
    val blocks: MutableMap<WBlockPos, Long> = HashMap()

    private val colorRedValue = IntegerValue("R", 0, 0, 255)
    private val colorGreenValue = IntegerValue("G", 90, 0, 255)
    private val colorBlueValue = IntegerValue("B", 255, 0, 255)
    private val colorRainbow = BoolValue("Rainbow", true)
    private val modeValue = ListValue("Mode", arrayOf("box", "otherbox", "shaderoutline", "shaderglow"), "otherbox")

    override fun onDisable() {
        synchronized(blocks) { blocks.clear() }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        val color = if (colorRainbow.get()) rainbow() else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
        for (entity in mc.theWorld!!.loadedEntityList) {
            if(!modeValue.get().equals("box", true) || !modeValue.get().equals("otherbox", true)) break
            if (!classProvider.isEntityFallingBlock(entity)) continue

            RenderUtils.drawEntityBox(entity, color, modeValue.get().equals("box", true))
        }
        synchronized(blocks) {
            val iterator: MutableIterator<Map.Entry<WBlockPos, Long>> = blocks.entries.iterator()

            while (iterator.hasNext()) {
                val entry = iterator.next()

                if (System.currentTimeMillis() - entry.value > 2000L) {
                    iterator.remove()
                    continue
                }

                RenderUtils.drawBlockBox(entry.key, color, modeValue.get().equals("box", true))
            }
        }
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val shader = when(modeValue.get()) {
            "shaderoutline" -> OutlineShader.OUTLINE_SHADER
            "shaderglow" -> GlowShader.GLOW_SHADER
            else -> null
        } ?: return

        shader.startDraw(event.partialTicks)
        try {
            for (entity in mc.theWorld!!.loadedEntityList) {
                if (!classProvider.isEntityFallingBlock(entity)) continue
                mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
            }
        } catch (ex: Exception) {
            ClientUtils.getLogger().error("An error occurred while rendering all entities for shader esp", ex)
        }

        val color = if (colorRainbow.get()) rainbow() else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
        shader.stopDraw(color, 1.5F, 1f)
    }
}