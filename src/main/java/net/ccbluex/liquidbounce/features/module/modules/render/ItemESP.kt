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
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.projectile.EntityArrow
import java.awt.Color

@ModuleInfo(name = "ItemESP", description = "Allows you to see items through walls.", category = ModuleCategory.RENDER)
class ItemESP : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "Glow"), "Box")

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

    private val colorRainbow = BoolValue("Rainbow", true)
    private val colorRedValue = object : IntegerValue("R", 0, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }
    private val colorGreenValue = object : IntegerValue("G", 255, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }
    private val colorBlueValue = object : IntegerValue("B", 0, 0, 255) {
        override fun isSupported() = !colorRainbow.get()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {

        if(mc.theWorld == null) return

        mc.theWorld.loadedEntityList.filter { it is EntityItem || it is EntityArrow }.forEach { entity ->
            when (modeValue.get().lowercase()) {
                "box" -> RenderUtils.drawEntityBox(entity, getColor(), true)
                "otherbox" -> RenderUtils.drawEntityBox(entity, getColor(), false)
            }
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        val mode = modeValue.get().lowercase()
        val partialTicks = event.partialTicks
        val shader = if (mode == "glow") GlowShader.GLOW_SHADER else null ?: return

        if(mc.theWorld == null) return

        shader.startDraw(partialTicks, glowRenderScale.get())
        try {
            mc.theWorld.loadedEntityList.filter { it is EntityItem || it is EntityArrow }.forEach { entity ->
                mc.renderManager.renderEntityStatic(entity, event.partialTicks, true)
            }
        } catch (ex: Exception) {
            ClientUtils.getLogger().error("An error occurred while rendering all item entities for shader esp", ex)
        }
        shader.stopDraw(getColor(), glowRadius.get(), glowFade.get(), glowTargetAlpha.get())
    }

    private fun getColor():Color{
        return if (colorRainbow.get()) rainbow() else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
    }

}