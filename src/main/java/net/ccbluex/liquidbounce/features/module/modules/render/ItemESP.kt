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
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbowRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.*

@ModuleInfo(name = "ItemESP", description = "Allows you to see items through walls.", category = ModuleCategory.RENDER)
class ItemESP : Module()
{
    private val modeGroup = ValueGroup("Mode")
    private val modeValue = ListValue("Mode", arrayOf("Box", "Hydra", "ShaderOutline"), "Box", "Mode")
    private val modeBoxOutlineColorValue = object : RGBAColorValue("BoxOutlineColor", 255, 255, 255, 90, listOf("Box-Outline-Red", "Box-Outline-Green", "Box-Outline-Blue", "Box-Outline-Alpha"))
    {
        override fun showCondition() = !modeValue.get().equals("ShaderOutline", ignoreCase = true)
    }
    private val shaderRadiusValue = object : FloatValue("ShaderRadius", 2f, 0.5f, 5f, "ShaderRadius")
    {
        override fun showCondition() = modeValue.get().equals("ShaderOutline", ignoreCase = true)
    }

    private val colorGroup = ValueGroup("Color")
    private val colorValue = RGBAColorValue("Color", 255, 255, 255, 30, listOf("R", "G", "B", "Alpha"))

    private val colorRainbowGroup = ValueGroup("Rainbow")
    private val colorRainbowEnabledValue = BoolValue("Enabled", true, "Rainbow")
    private val colorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
    private val colorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "HSB-Saturation")
    private val colorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "HSB-Brightness")

    private val interpolateValue = BoolValue("Interpolate", true)

    init
    {
        modeGroup.addAll(modeValue, modeBoxOutlineColorValue, shaderRadiusValue)

        colorRainbowGroup.addAll(colorRainbowEnabledValue, colorRainbowSpeedValue, colorRainbowSaturationValue, colorRainbowBrightnessValue)
        colorGroup.addAll(colorValue, colorRainbowGroup)
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val mode = modeValue.get().toLowerCase()
        if (mode != "shaderoutline")
        {
            val theWorld = mc.theWorld ?: return
            val provider = classProvider

            val color = if (colorRainbowEnabledValue.get()) rainbowRGB(alpha = colorValue.getAlpha(), speed = colorRainbowSpeedValue.get(), saturation = colorRainbowSaturationValue.get(), brightness = colorRainbowBrightnessValue.get()) else colorValue.get()
            val hydraESP = mode == "hydra"
            val boxOutlineColor = modeBoxOutlineColorValue.get()

            val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f

            theWorld.loadedEntityList.filter { it is EntityItem || it is EntityArrow }.forEach { RenderUtils.drawEntityBox(it, color, boxOutlineColor, hydraESP, partialTicks) }
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent)
    {
        val theWorld = mc.theWorld ?: return

        if (modeValue.get().equals("ShaderOutline", ignoreCase = true))
        {
            val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f

            OutlineShader.INSTANCE.startDraw(partialTicks)

            val renderManager = mc.renderManager
            val provider = classProvider

            try
            {
                theWorld.loadedEntityList.filter { it is EntityItem || it is EntityArrow }.forEach { renderManager.renderEntityStatic(it, partialTicks, true) }
            }
            catch (ex: Exception)
            {
                ClientUtils.logger.error("An error occurred while rendering all item entities for shader esp", ex)
            }

            OutlineShader.INSTANCE.stopDraw(if (colorRainbowEnabledValue.get()) rainbowRGB(speed = colorRainbowSpeedValue.get(), saturation = colorRainbowSaturationValue.get(), brightness = colorRainbowBrightnessValue.get()) else colorValue.get(255), shaderRadiusValue.get(), 1f)
        }
    }

    override val tag: String
        get() = modeValue.get()
}
