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
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.util.BlockPos

@ModuleInfo(name = "ProphuntESP", description = "Allows you to see disguised players in PropHunt.", category = ModuleCategory.RENDER)
class ProphuntESP : Module()
{
    private val modeGroup = ValueGroup("Mode")
    private val modeValue = ListValue("Mode", arrayOf("Box", "Hydra", "ShaderOutline", "ShaderGlow"), "OtherBox")
    private val modeBoxOutlineColorValue = object : RGBAColorValue("BoxOutlineColor", 255, 255, 255, 90, listOf("Box-Outline-Red", "Box-Outline-Green", "Box-Outline-Blue", "Box-Outline-Alpha"))
    {
        override fun showCondition() = !modeValue.get().startsWith("Shader", ignoreCase = true)
    }
    private val shaderOutlineRadius = object : FloatValue("ShaderOutlineRadius", 1.35f, 1f, 2f)
    {
        override fun showCondition() = modeValue.get().equals("ShaderOutline", ignoreCase = true)
    }
    private val shaderGlowRadius = object : FloatValue("ShaderGlowRadius", 2.3f, 2f, 3f)
    {
        override fun showCondition() = modeValue.get().equals("ShaderGlow", ignoreCase = true)
    }

    private val colorGroup = ValueGroup("Color")
    private val colorValue = RGBAColorValue("Color", 0, 90, 255, 255, listOf("R", "G", "B", "Alpha"))

    private val colorRainbowGroup = ValueGroup("Rainbow")
    private val colorRainbowEnabledValue = BoolValue("Enabled", true, "Rainbow")
    private val colorRainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
    private val colorRainbowSaturationValue = FloatValue("Saturation", 1.0f, 0.0f, 1.0f, "HSB-Saturation")
    private val colorRainbowBrightnessValue = FloatValue("Brightness", 1.0f, 0.0f, 1.0f, "HSB-Brightness")

    private val interpolateValue = BoolValue("Interpolate", true)

    /**
     * Variables
     */
    val blocks = hashMapOf<BlockPos, Long>()

    init
    {
        modeGroup.addAll(modeValue, modeBoxOutlineColorValue, shaderOutlineRadius, shaderGlowRadius)

        colorRainbowGroup.addAll(colorRainbowEnabledValue, colorRainbowSpeedValue, colorRainbowSaturationValue, colorRainbowBrightnessValue)
        colorGroup.addAll(colorValue, colorRainbowGroup)
    }

    override fun onDisable()
    {
        synchronized(blocks, blocks::clear)
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val mode = modeValue.get().lowercase()
        val hydraESP = mode == "hydra"
        val color = if (colorRainbowEnabledValue.get()) rainbowRGB(alpha = colorValue.getAlpha(), speed = colorRainbowSpeedValue.get(), saturation = colorRainbowSaturationValue.get(), brightness = colorRainbowBrightnessValue.get()) else colorValue.get()
        val boxOutlineColor = modeBoxOutlineColorValue.get()

        val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f

        if (mode == "box" || hydraESP) theWorld.loadedEntityList.filterIsInstance<EntityFallingBlock>().forEach { RenderUtils.drawEntityBox(it, color, boxOutlineColor, hydraESP, partialTicks) }

        synchronized(blocks) {
            val iterator: MutableIterator<Map.Entry<BlockPos, Long>> = blocks.entries.iterator()

            while (iterator.hasNext())
            {
                val entry = iterator.next()

                if (System.currentTimeMillis() - entry.value > 2000L)
                {
                    iterator.remove()
                    continue
                }

                RenderUtils.drawBlockBox(theWorld, thePlayer, entry.key, color, boxOutlineColor, hydraESP, partialTicks)
            }
        }
    }

    @EventTarget
    fun onRender2D(event: Render2DEvent)
    {
        val theWorld = mc.theWorld ?: return
        val renderManager = mc.renderManager
        val partialTicks = if (interpolateValue.get()) event.partialTicks else 1f

        val mode = modeValue.get().lowercase()
        val shader = when (mode)
        {
            "shaderoutline" -> OutlineShader.INSTANCE
            "shaderglow" -> GlowShader.INSTANCE
            else -> null
        } ?: return

        shader.startDraw(partialTicks)

        try
        {
            theWorld.loadedEntityList.filterIsInstance<EntityFallingBlock>().forEach { renderManager.renderEntityStatic(it, partialTicks, true) }
        }
        catch (ex: Exception)
        {
            ClientUtils.logger.error("An error occurred while rendering all entities for shader esp", ex)
        }

        shader.stopDraw(if (colorRainbowEnabledValue.get()) rainbowRGB(alpha = colorValue.getAlpha(), speed = colorRainbowSpeedValue.get(), saturation = colorRainbowSaturationValue.get(), brightness = colorRainbowBrightnessValue.get()) else colorValue.get(), when (mode)
        {
            "shaderoutline" -> shaderOutlineRadius.get()
            "shaderglow" -> shaderGlowRadius.get()
            else -> 1f
        }, 1f)
    }

    override val tag: String
        get() = modeValue.get()
}
