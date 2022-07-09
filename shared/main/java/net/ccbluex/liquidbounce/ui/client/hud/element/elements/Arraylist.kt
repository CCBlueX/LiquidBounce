/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.assumeNonVolatile
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.easeOut
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import java.awt.Color

/**
 * CustomHUD Arraylist element
 *
 * Shows a list of enabled modules
 */
@ElementInfo(name = "Arraylist", single = true)
class Arraylist(x: Double = 1.0, y: Double = 2.0, scale: Float = 1F, side: Side = Side(Horizontal.RIGHT, Vertical.UP)) : Element(x, y, scale, side)
{
    private val textGroup = ValueGroup("Text")

    private val textColorGroup = ValueGroup("Color")
    private val textColorModeValue = ListValue("Mode", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Custom", "Text-Color")
    private val textColorValue = RGBColorValue("Color", 0, 111, 255, Triple("Text-R", "Text-G", "Text-B"))

    private val textShadowValue = BoolValue("Shadow", true, "ShadowText")
    private val textVarianceModeValue = ListValue("Variance", arrayOf("Original", "lowercase", "UPPERCASE", "UPPER-EXCEPT-i", "uPPER-eXCEPT-fIRST", "uPPER-eXCEPT-i-aND-fiRST"), "Original")

    private val spaceValue = FloatValue("Space", 0F, 0F, 5F, "Space")
    private val textHeightValue = FloatValue("Height", 11F, 1F, 20F, "TextHeight")
    private val textYValue = FloatValue("Y", 1F, 0F, 20F, "TextY")

    private val tagsGroup = ValueGroup("Tags")
    private val tagsModeValue = ListValue("Mode", arrayOf("Off", "Space-Only", "Square-Bracket", "Round-Bracket"), "Space-Only", "TagType")
    private val tagsSpaceValue = FloatValue("Space", 2.5F, 1F, 5F, "TagSpace")

    private val tagsColorGroup = ValueGroup("Color")
    private val tagsColorModeValue = ListValue("Tag-Color", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Custom", "Tag-Color")
    private val tagsColorValue = RGBColorValue("Color", 180, 180, 180, Triple("Tag-R", "Tag-G", "Tag-B"))

    private val rectGroup = ValueGroup("Rect")
    private val rectModeValue = ListValue("Mode", arrayOf("None", "Left", "Right", "Frame"), "None", "Rect")
    private val rectWidthValue = FloatValue("Width", 3F, 1.5F, 5F, "Rect-Width")
    private val rectFrameWidthValue = FloatValue("FrameWidth", 3F, 1.5F, 5F, "Rect-Frame-Width")

    private val rectColorGroup = ValueGroup("Color")
    private val rectColorModeValue = ListValue("Mode", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Rainbow", "Rect-Color")
    private val rectColorValue = RGBAColorValue("Color", 255, 255, 255, 255, listOf("Rect-R", "Rect-G", "Rect-B", "Rect-Alpha"))

    private val backgroundColorGroup = ValueGroup("BackgroundColor")
    private val backgroundColorModeValue = ListValue("Mode", arrayOf("Custom", "Random", "Rainbow", "RainbowShader"), "Custom", "Background-Color")
    private val backgroundColorValue = RGBAColorValue("Color", 0, 0, 0, 255, listOf("Background-R", "Background-G", "Background-B", "Background-Alpha"))

    private val rainbowGroup = object : ValueGroup("Rainbow")
    {
        override fun showCondition() = arrayOf(textColorModeValue.get(), tagsColorModeValue.get(), rectColorModeValue.get(), backgroundColorModeValue.get()).any { it.equals("Rainbow", ignoreCase = true) }
    }
    private val rainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
    private val rainbowOffsetValue = IntegerValue("IndexOffset", 0, -100, 100, "Rainbow-IndexOffset")
    private val rainbowSaturationValue = FloatValue("Saturation", 1f, 0f, 1f, "HSB-Saturation")
    private val rainbowBrightnessValue = FloatValue("Brightness", 1f, 0f, 1f, "HSB-Brightness")

    private val rainbowShaderGroup = object : ValueGroup("RainbowShader")
    {
        override fun showCondition() = arrayOf(textColorModeValue.get(), tagsColorModeValue.get(), rectColorModeValue.get(), backgroundColorModeValue.get()).any { it.equals("RainbowShader", ignoreCase = true) }
    }
    private val rainbowShaderXValue = FloatValue("X", -1000F, -2000F, 2000F, "RainbowShader-X")
    private val rainbowShaderYValue = FloatValue("Y", -1000F, -2000F, 2000F, "RainbowShader-Y")

    private val animationSpeedValue = FloatValue("AnimationSpeed", 17F, 0.1F, 20F)

    private val fontValue = FontValue("Font", Fonts.font40)

    private var x2 = 0
    private var y2 = 0F

    private var modules = emptyList<Module>()

    init
    {
        textColorGroup.addAll(textColorModeValue, textColorValue)
        textGroup.addAll(textColorGroup, textShadowValue, textVarianceModeValue)

        tagsColorGroup.addAll(tagsColorModeValue, tagsColorValue)
        tagsGroup.addAll(tagsModeValue, tagsSpaceValue, tagsColorGroup)

        rectColorGroup.addAll(rectColorModeValue, rectColorValue)
        rectGroup.addAll(rectModeValue, rectWidthValue, rectFrameWidthValue, rectColorGroup)

        backgroundColorGroup.addAll(backgroundColorModeValue, backgroundColorValue)

        rainbowGroup.addAll(rainbowSpeedValue, rainbowOffsetValue, rainbowSaturationValue, rainbowBrightnessValue)

        rainbowShaderGroup.addAll(rainbowShaderXValue, rainbowShaderYValue)
    }

    override fun drawElement(): Border?
    {
        val fontRenderer = fontValue.get()

        assumeNonVolatile {

            val tagSpace = tagsSpaceValue.get()

            // Slide animation - update every render
            val slideAnimationSpeed = 20.5F - animationSpeedValue.get().coerceIn(0.01f, 20f)
            val frameTime = RenderUtils.frameTime
            LiquidBounce.moduleManager.modules.filter(Module::array).filter { it.state || it.slide != 0F }.forEach { module ->
                val moduleName = applyVariance(module.name)
                val moduleTag = applyVariance(formatTag(module))

                val width = fontRenderer.getStringWidth(moduleName) + if (moduleTag.isEmpty()) 0f else tagSpace + fontRenderer.getStringWidth(moduleTag)

                if (module.state)
                {
                    if (module.slide < width)
                    {
                        module.slide = easeOut(module.slideStep, width) * width
                        module.slideStep += frameTime / slideAnimationSpeed
                    }
                }
                else if (module.slide > 0)
                {
                    module.slide = easeOut(module.slideStep, width) * width
                    module.slideStep -= frameTime / slideAnimationSpeed
                }

                module.slide = module.slide.coerceIn(0F, width)
                module.slideStep = module.slideStep.coerceIn(0F, width)
            }

            // Color
            val textColorMode = textColorModeValue.get()
            val textCustomColor = textColorValue.get()

            // Rect Mode & Color
            val rectMode = rectModeValue.get()
            val frame = rectMode.equals("Frame", ignoreCase = true)
            val rightRect = rectMode.equals("Right", ignoreCase = true)
            val leftRect = rectMode.equals("Left", ignoreCase = true)

            val rectWidth = rectWidthValue.get()
            val frameWidth = rectFrameWidthValue.get()

            val rectColorMode = rectColorModeValue.get()
            val rectCustomColor = rectColorValue.get()

            // Background Color
            val backgroundColorMode = backgroundColorModeValue.get()
            val backgroundCustomColor = backgroundColorValue.get()

            // Tag Mode & Color
            val tagColorMode = tagsColorModeValue.get()
            val tagCustomColor = tagsColorValue.get()

            // Text Shadow
            val textShadow = textShadowValue.get()

            // Text Y
            val textY = textYValue.get()

            // Text Spacer
            val space = spaceValue.get()
            val textHeight = textHeightValue.get()
            val textSpacer = textHeight + space

            // Rainbow
            val saturation = rainbowSaturationValue.get()
            val brightness = rainbowBrightnessValue.get()
            val rainbowSpeed = rainbowSpeedValue.get()
            val rainbowOffset = 400000000L + 40000000L * rainbowOffsetValue.get()

            // Rainbow Shader
            val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
            val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
            val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

            val horizontalSide = side.horizontal
            val isDownDir = side.vertical == Vertical.DOWN

            val renderText = { text: String, x: Float, y: Float, colorMode: String, index: Int, randomColorSupplier: () -> Int, customColor: Int ->
                var useRainbowShader = false
                val color = when (colorMode.toLowerCase())
                {
                    "rainbowshader" ->
                    {
                        useRainbowShader = true
                        16777215
                    }

                    "rainbow" -> ColorUtils.rainbowRGB(offset = rainbowOffset * index, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
                    "random" -> randomColorSupplier()
                    else -> customColor
                }

                RainbowFontShader.begin(useRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
                    fontRenderer.drawString(text, x, y, color, textShadow)
                }
            }

            val renderRect = { xStart: Float, yStart: Float, xEnd: Float, yEnd: Float, colorMode: String, index: Int, randomColorSupplier: () -> Int, customColor: Int, extra: (Int) -> Unit ->
                var useRainbowShader = false
                val color = when (colorMode.toLowerCase())
                {
                    "rainbowshader" ->
                    {
                        useRainbowShader = true
                        0
                    }

                    "rainbow" -> ColorUtils.rainbowRGB(alpha = customColor shr 24 and 0xFF, offset = rainbowOffset * index, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
                    "random" -> ColorUtils.applyAlphaChannel(randomColorSupplier(), customColor shr 24 and 0xFF)
                    else -> customColor
                }

                RainbowShader.begin(useRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
                    RenderUtils.drawRect(xStart, yStart, xEnd, yEnd, color)
                    extra(color)
                }
            }

            val glStateManager = classProvider.glStateManager

            val renderArraylist: (index: Int, slide: Float, randomColorSupplier: () -> Int, moduleName: String, moduleTag: String) -> Unit = when (horizontalSide)
            {
                Horizontal.RIGHT, Horizontal.MIDDLE -> { index, slide, randomColorSupplier, moduleName, moduleTag ->
                    val xPos = -slide - 2f
                    val yPos = (if (isDownDir) -textSpacer else textSpacer) * if (isDownDir) index + 1 else index

                    // Draw Background
                    renderRect(xPos - (2F + if (rightRect) rectWidth else 0F), yPos, if (rightRect) -rectWidth else 0F, yPos + textHeight, backgroundColorMode, index, randomColorSupplier, backgroundCustomColor) {}

                    glStateManager.resetColor()

                    // Draw Module Name
                    renderText(moduleName, xPos - if (rightRect) rectWidth else 0F, yPos + textY, textColorMode, index, randomColorSupplier, textCustomColor)

                    // Draw Tag
                    if (moduleTag.isNotBlank()) renderText(moduleTag, xPos + (fontRenderer.getStringWidth(moduleName) + tagSpace - if (rightRect) rectWidth else 0F), yPos + textY, tagColorMode, index, randomColorSupplier, tagCustomColor)

                    // Draw Rect
                    if (rightRect || leftRect || frame)
                    {
                        val frameStartX = xPos - 2F - rectWidth
                        val renderFrame = { color: Int ->
                            if (frame)
                            {
                                val nextModuleIndex = if (isDownDir) -1 else 1
                                val nextModuleXPos = if (index + nextModuleIndex == modules.size) 0F else -modules[(index + nextModuleIndex).coerceIn(0, modules.size - 1)].slide - 2F

                                val frameEndX = if (nextModuleXPos == 0F) 0F else nextModuleXPos - 2F - rectWidth
                                val frameEndY = yPos + textHeight + if (frameStartX > frameEndX) -frameWidth else frameWidth

                                RenderUtils.drawRect(frameStartX, yPos + textHeight, frameEndX, frameEndY, color)
                            }
                        }

                        when
                        {
                            leftRect || frame -> renderRect(frameStartX, yPos, xPos - 2F, yPos + textHeight, rectColorMode, index, randomColorSupplier, rectCustomColor, renderFrame)
                            rightRect -> renderRect(-rectWidth, yPos, 0F, yPos + textHeight, rectColorMode, index, randomColorSupplier, rectCustomColor, renderFrame)
                        }
                    }
                }

                Horizontal.LEFT -> { index, slide, randomColorSupplier, moduleName, moduleTag ->
                    val width = fontRenderer.getStringWidth(moduleName) + if (moduleTag.isBlank()) 0f else tagSpace + fontRenderer.getStringWidth(moduleTag)
                    val xPos = -(width - slide) + 2F + if (leftRect) rectWidth else 0F
                    val yPos = (if (isDownDir) -textSpacer else textSpacer) * if (isDownDir) index + 1 else index

                    // Draw Background
                    renderRect(0F, yPos, xPos + width + (2F + if (rightRect) rectWidth else 0F), yPos + textHeight, backgroundColorMode, index, randomColorSupplier, backgroundCustomColor) {}

                    glStateManager.resetColor()

                    // Draw Module Name
                    renderText(moduleName, xPos, yPos + textY, textColorMode, index, randomColorSupplier, textCustomColor)

                    // Draw Tag
                    if (moduleTag.isNotBlank()) renderText(moduleTag, xPos + fontRenderer.getStringWidth(moduleName) + tagSpace, yPos + textY, tagColorMode, index, randomColorSupplier, tagCustomColor)

                    // Draw Rect
                    if (rightRect || leftRect || frame)
                    {
                        val frameEndX = xPos + width + 2F + rectWidth
                        val renderFrame = { color: Int ->
                            if (frame)
                            {
                                val nextModuleIndex = if (isDownDir) -1 else 1
                                val isLastModule = index + nextModuleIndex == modules.size
                                val nextModule = modules[(index + nextModuleIndex).coerceIn(0, modules.size - 1)]

                                val nextModuleTag = applyVariance(formatTag(nextModule))

                                val nextModuleWidth = if (isLastModule) 0F else fontRenderer.getStringWidth(applyVariance(nextModule.name)) + if (nextModuleTag.isBlank()) 0f else tagSpace + fontRenderer.getStringWidth(nextModuleTag)
                                val nextModuleXPos = if (isLastModule) 0F else -(nextModuleWidth - nextModule.slide) + 2F

                                val frameStartX = if (nextModuleXPos == 0F) 0F else (nextModuleXPos + nextModuleWidth + rectWidth + 2F)

                                RenderUtils.drawRect(frameStartX, yPos + textHeight, frameEndX, yPos + textHeight + if (frameEndX < frameStartX) -frameWidth else frameWidth, color)
                            }
                        }

                        when
                        {
                            leftRect -> renderRect(0F, yPos - 1F, rectWidth, yPos + textHeight, rectColorMode, index, randomColorSupplier, rectCustomColor, renderFrame)
                            rightRect || frame -> renderRect(xPos + width + 2F, yPos, frameEndX, yPos + textHeight, rectColorMode, index, randomColorSupplier, rectCustomColor, renderFrame)
                        }
                    }
                }
            }

            modules.forEachIndexed { index, module ->
                renderArraylist(index, module.slide, { Color.getHSBColor(module.hue, saturation, brightness).rgb }, applyVariance(module.name), applyVariance(formatTag(module)))
            }

            // Calculate and draw border
            if (classProvider.isGuiHudDesigner(mc.currentScreen))
            {
                x2 = Int.MIN_VALUE

                if (modules.isEmpty()) return@drawElement if (horizontalSide == Horizontal.LEFT) Border(0F, -1F, 20F, 20F) else Border(0F, -1F, -20F, 20F)

                modules.map { it.slide.toInt() }.forEach { slide ->
                    when (horizontalSide)
                    {
                        Horizontal.RIGHT, Horizontal.MIDDLE ->
                        {
                            val xPos = -slide - 2
                            if (x2 == Int.MIN_VALUE || xPos < x2) x2 = xPos
                        }

                        Horizontal.LEFT ->
                        {
                            val xPos = slide + 14
                            if (x2 == Int.MIN_VALUE || xPos > x2) x2 = xPos
                        }
                    }
                }

                y2 = (if (isDownDir) -textSpacer else textSpacer) * modules.size

                return@drawElement Border(0F, 0F, x2 - 7F, y2 - if (isDownDir) 1F else 0F)
            }
        }

        RenderUtils.resetColor()

        return null
    }

    /**
     * @author eric0210
     */
    private fun applyVariance(string: String): String = when (textVarianceModeValue.get().toLowerCase())
    {
        "lowercase" -> string.toLowerCase()
        "uppercase" -> string.toUpperCase()
        "upper-except-i" -> string.toUpperCase().replace('I', 'i', ignoreCase = false)
        "upper-except-first" -> if (string.length <= 1) string.toLowerCase() else string[0].toLowerCase() + string.substring(1).toUpperCase()
        "upper-except-i-and-first" -> if (string.length <= 1) string.toLowerCase() else string[0].toLowerCase() + string.substring(1).toUpperCase().replace('I', 'i', ignoreCase = false)
        else -> string
    }

    /**
     * @author eric0210
     */
    private fun formatTag(module: Module): String
    {
        val originalTagString = if (!tagsModeValue.get().equals("Off", ignoreCase = true)) module.tag ?: return "" else return ""

        return if (originalTagString.isNotBlank()) when (tagsModeValue.get().toLowerCase())
        {
            "square-bracket" -> "[$originalTagString\u00A7r]"
            "round-bracket" -> "($originalTagString\u00A7r)"
            else -> "$originalTagString\u00A7r"
        }
        else originalTagString
    }

    override fun updateElement()
    {
        val font = fontValue.get()
        val tagSpace = tagsSpaceValue.get()

        modules = LiquidBounce.moduleManager.modules.filter(Module::array).filter { it.slide > 0 }.sortedBy {
            val tag = applyVariance(formatTag(it))

            -(font.getStringWidth(applyVariance(it.name)) + if (tag.isEmpty()) 0f else tagSpace + font.getStringWidth(tag))
        }
    }
}
