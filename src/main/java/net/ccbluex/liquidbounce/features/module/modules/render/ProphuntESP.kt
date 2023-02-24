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

    private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "Glow"), "OtherBox")

    private val glowRenderScale = object : FloatValue("Glow-Renderscale", 1f, 0.1f, 2f) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowRadius = object : IntegerValue("Glow-Radius", 4, 1, 5) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowFade = object : IntegerValue("Glow-Fade", 10, 0, 30) {
        override fun isSupported() = modeValue.get() == "Glow"
    }
    private val glowTargetAlpha = object : FloatValue("Glow-Target-Alpha", 0f, 0f, 1f) {
        override fun isSupported() = modeValue.get() == "Glow"
    }

    private val colorRainbow = BoolValue("Rainbow", false)
    private val colorRedValue = object : IntegerValue("R", 0, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }
    private val colorGreenValue = object : IntegerValue("G", 90, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }
    private val colorBlueValue = object : IntegerValue("B", 255, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }


    override fun onDisable() {
        synchronized(blocks) { blocks.clear() }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        val mode = modeValue.get()
        for (entity in mc.theWorld!!.loadedEntityList) {
            if(!mode.equals("Box", true) || !mode.equals("OtherBox", true)) break
            if (entity !is EntityFallingBlock) continue

            RenderUtils.drawEntityBox(entity, getColor(), mode.equals("Box", true))
        }
        synchronized(blocks) {
            val iterator: MutableIterator<Map.Entry<BlockPos, Long>> = blocks.entries.iterator()

            while (iterator.hasNext()) {
                val entry = iterator.next()

                if (System.currentTimeMillis() - entry.value > 2000L) {
                    iterator.remove()
                    continue
                }

                RenderUtils.drawBlockBox(entry.key, getColor(), mode.equals("Box", true))
            }
        }
    }
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val mode = modeValue.get().lowercase()
        val shader = if (mode == "glow") GlowShader.GLOW_SHADER else null ?: return
        val color = if (colorRainbow.get()) rainbow() else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())

        if(mc.theWorld == null) return

        shader.startDraw(event.partialTicks, glowRenderScale.get())
        try {
            mc.theWorld.loadedEntityList.filterNot{ it !is EntityFallingBlock }.forEach { entity ->
                mc.renderManager.renderEntityStatic(entity, mc.timer.renderPartialTicks, true)
            }
        } catch (ex: Exception) {
            ClientUtils.getLogger().error("An error occurred while rendering all entities for shader esp", ex)
        }


        shader.stopDraw(color, glowRadius.get(), glowFade.get(), glowTargetAlpha.get())
    }

    private fun getColor():Color{
        return if (colorRainbow.get()) rainbow() else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
    }

}
