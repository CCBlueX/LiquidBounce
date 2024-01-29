/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.misc.GameDetector
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Side.Vertical
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color

/**
 * CustomHUD Arraylist element
 *
 * Shows a list of enabled modules
 */
@ElementInfo(name = "Arraylist", single = true)
class Arraylist(
    x: Double = 1.0, y: Double = 2.0, scale: Float = 1F,
    side: Side = Side(Horizontal.RIGHT, Vertical.UP)
) : Element(x, y, scale, side) {

    private val textColorMode by ListValue("Text-Color", arrayOf("Custom", "Random", "Rainbow"), "Custom")
    private val textRed by IntegerValue("Text-R", 0, 0..255) { textColorMode == "Custom" }
    private val textGreen by IntegerValue("Text-G", 111, 0..255) { textColorMode == "Custom" }
    private val textBlue by IntegerValue("Text-B", 255, 0..255) { textColorMode == "Custom" }

    private val rectMode by ListValue("Rect", arrayOf("None", "Left", "Right"), "None")
    private val rectColorMode by ListValue(
        "Rect-Color",
        arrayOf("Custom", "Random", "Rainbow"),
        "Rainbow"
    ) { rectMode != "None" }
    private val isCustomRectSupported = { rectMode != "None" && rectColorMode == "Custom" }
    private val rectRed by IntegerValue("Rect-R", 255, 0..255, isSupported = isCustomRectSupported)
    private val rectGreen by IntegerValue("Rect-G", 255, 0..255, isSupported = isCustomRectSupported)
    private val rectBlue by IntegerValue("Rect-B", 255, 0..255, isSupported = isCustomRectSupported)
    private val rectAlpha by IntegerValue("Rect-Alpha", 255, 0..255, isSupported = isCustomRectSupported)

    private val backgroundMode by ListValue("Background-Color", arrayOf("Custom", "Random", "Rainbow"), "Custom")
    private val backgroundRed by IntegerValue("Background-R", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundGreen by IntegerValue("Background-G", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundBlue by IntegerValue("Background-B", 0, 0..255) { backgroundMode == "Custom" }
    private val backgroundAlpha by IntegerValue("Background-Alpha", 0, 0..255) { backgroundMode == "Custom" }

    private fun isColorModeUsed(value: String) = textColorMode == value || rectMode == value || backgroundMode == value

    private val saturation by FloatValue("Random-Saturation", 0.9f, 0f..1f) { isColorModeUsed("Random") }
    private val brightness by FloatValue("Random-Brightness", 1f, 0f..1f) { isColorModeUsed("Random") }
    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }

    private val tags by BoolValue("Tags", true)
    private val tagsStyle by object : ListValue("TagsStyle", arrayOf("[]", "()", "<>", "-", "|", "Space"), "Space") {
        override fun isSupported() = tags

        // onUpdate - updates tag onInit and onChanged
        override fun onUpdate(value: String) = updateTagDetails()
    }
    private val tagsCase by ListValue("TagsCase", arrayOf("Normal", "Uppercase", "Lowercase"), "Normal") { tags }
    private val tagsArrayColor by object : BoolValue("TagsArrayColor", false) {
        override fun isSupported() = tags
        override fun onUpdate(value: Boolean) = updateTagDetails()
    }

    private val font by FontValue("Font", Fonts.font40)
    private val textShadow by BoolValue("ShadowText", true)
    private val moduleCase by ListValue("ModuleCase", arrayOf("Normal", "Uppercase", "Lowercase"), "Normal")
    private val space by FloatValue("Space", 0F, 0F..5F)
    private val textHeight by FloatValue("TextHeight", 11F, 1F..20F)
    private val textY by FloatValue("TextY", 1F, 0F..20F)

    private val animation by ListValue("Animation", arrayOf("Slide", "Smooth"), "Smooth") { tags }
    private val animationSpeed by FloatValue("AnimationSpeed", 0.2F, 0.01F..1F) { animation == "Smooth" }

    companion object {
        val spacedModules by BoolValue("SpacedModules", false)
        val inactiveStyle by ListValue("InactiveModulesStyle", arrayOf("Normal", "Color", "Hide"), "Color")
        { GameDetector.state }
    }

    private var x2 = 0
    private var y2 = 0F

    private lateinit var tagPrefix: String

    private lateinit var tagSuffix: String

    private var modules = emptyList<Module>()

    private val inactiveColor = Color(255, 255, 255, 100).rgb

    init {
        updateTagDetails()
    }

    fun updateTagDetails() {
        val pair: Pair<String, String> = when (tagsStyle) {
            "[]", "()", "<>" -> tagsStyle[0].toString() to tagsStyle[1].toString()
            "-", "|" -> tagsStyle[0] + " " to ""
            else -> "" to ""
        }

        tagPrefix = (if (tagsArrayColor) " " else " §7") + pair.first
        tagSuffix = pair.second
    }

    private fun getDisplayString(module: Module): String {
        val moduleName = when (moduleCase) {
            "Uppercase" -> module.getName().uppercase()
            "Lowercase" -> module.getName().lowercase()
            else -> module.getName()
        }

        var tag = module.tag ?: ""

        tag = when (tagsCase) {
            "Uppercase" -> tag.uppercase()
            "Lowercase" -> tag.lowercase()
            else -> tag
        }

        val moduleTag = if (tags && !module.tag.isNullOrEmpty()) tagPrefix + tag + tagSuffix else ""

        return moduleName + moduleTag
    }

    override fun drawElement(): Border? {

        AWTFontRenderer.assumeNonVolatile = true

        // Slide animation - update every render
        val delta = deltaTime

        for (module in moduleManager.modules) {
            val shouldShow = (module.inArray && module.state && (inactiveStyle != "Hide" || module.isActive))

            if (!shouldShow && module.slide <= 0f)
                continue

            val displayString = getDisplayString(module)

            val width = font.getStringWidth(displayString)

            when (animation) {
                "Slide" -> {
                    // If modules become inactive because they only work when in game, animate them as if they got disabled
                    module.slideStep = if (shouldShow) delta / 4F else -delta / 4F
                    if (shouldShow) {
                        if (module.slide < width) {
                            module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                        }
                    } else {
                        module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                    }

                    module.slide = module.slide.coerceIn(0F, width.toFloat())
                    module.slideStep = module.slideStep.coerceIn(0F, width.toFloat())
                }

                "Smooth" -> {
                    val target = if (shouldShow) width.toDouble() else -width / 5.0
                    module.slide =
                        AnimationUtil.base(module.slide.toDouble(), target, animationSpeed.toDouble()).toFloat()
                }
            }
        }
        // Draw arraylist
        val textCustomColor = Color(textRed, textGreen, textBlue, 1).rgb
        val rectCustomColor = Color(rectRed, rectGreen, rectBlue, rectAlpha).rgb
        val backgroundCustomColor = Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha).rgb
        val textSpacer = textHeight + space

        val rainbowOffset = System.currentTimeMillis() % 10000 / 10000F
        val rainbowX = if (rainbowX == 0f) 0f else 1f / rainbowX
        val rainbowY = if (rainbowY == 0f) 0f else 1f / rainbowY


        modules.forEachIndexed { index, module ->
            var yPos = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) *
                    if (side.vertical == Vertical.DOWN) index + 1 else index
            if (animation == "Smooth") {
                module.yAnim = AnimationUtil.base(module.yAnim.toDouble(), yPos.toDouble(), 0.2).toFloat()
                yPos = module.yAnim
            }
            val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb

            val markAsInactive = inactiveStyle == "Color" && !module.isActive

            val displayString = getDisplayString(module)

            when (side.horizontal) {
                Horizontal.RIGHT, Horizontal.MIDDLE -> {
                    val xPos = -module.slide - 2

                    RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                        drawRect(
                            xPos - if (rectMode == "Right") 5 else 2,
                            yPos,
                            if (rectMode == "Right") -3F else 0F,
                            yPos + textHeight,
                            when (backgroundMode) {
                                "Rainbow" -> 0
                                "Random" -> moduleColor
                                else -> backgroundCustomColor
                            }
                        )
                    }

                    RainbowFontShader.begin(
                        !markAsInactive && textColorMode == "Rainbow",
                        rainbowX,
                        rainbowY,
                        rainbowOffset
                    ).use {
                        font.drawString(
                            displayString, xPos - if (rectMode == "Right") 3 else 0, yPos + textY,
                            if (markAsInactive) inactiveColor
                            else when (textColorMode) {
                                "Rainbow" -> 0
                                "Random" -> moduleColor
                                else -> textCustomColor
                            },
                            textShadow
                        )
                    }

                    if (rectMode != "None") {
                        RainbowShader.begin(
                            !markAsInactive && rectColorMode == "Rainbow",
                            rainbowX,
                            rainbowY,
                            rainbowOffset
                        ).use {
                            val rectColor =
                                if (markAsInactive) inactiveColor
                                else when (rectColorMode) {
                                    "Rainbow" -> 0
                                    "Random" -> moduleColor
                                    else -> rectCustomColor
                                }

                            when (rectMode) {
                                "Left" -> drawRect(xPos - 5, yPos, xPos - 2, yPos + textHeight, rectColor)
                                "Right" -> drawRect(-3F, yPos, 0F, yPos + textHeight, rectColor)
                            }
                        }
                    }
                }

                Horizontal.LEFT -> {
                    val width = font.getStringWidth(displayString)
                    val xPos = -(width - module.slide) + if (rectMode == "Left") 5 else 2

                    RainbowShader.begin(backgroundMode == "Rainbow", rainbowX, rainbowY, rainbowOffset).use {
                        drawRect(
                            0F, yPos, xPos + width + if (rectMode == "Right") 5 else 2, yPos + textHeight,
                            when (backgroundMode) {
                                "Rainbow" -> 0
                                "Random" -> moduleColor
                                else -> backgroundCustomColor
                            }
                        )
                    }

                    RainbowFontShader.begin(
                        !markAsInactive && textColorMode == "Rainbow",
                        rainbowX,
                        rainbowY,
                        rainbowOffset
                    ).use {
                        font.drawString(
                            displayString, xPos, yPos + textY,
                            if (markAsInactive) inactiveColor
                            else when (textColorMode) {
                                "Rainbow" -> 0
                                "Random" -> moduleColor
                                else -> textCustomColor
                            },
                            textShadow
                        )
                    }

                    RainbowShader.begin(
                        !markAsInactive && rectColorMode == "Rainbow",
                        rainbowX,
                        rainbowY,
                        rainbowOffset
                    ).use {
                        if (rectMode != "None") {
                            val rectColor =
                                if (markAsInactive) inactiveColor
                                else when (rectColorMode) {
                                    "Rainbow" -> 0
                                    "Random" -> moduleColor
                                    else -> rectCustomColor
                                }

                            when (rectMode) {
                                "Left" -> drawRect(0F, yPos - 1, 3F, yPos + textHeight, rectColor)
                                "Right" -> drawRect(
                                    xPos + width + 2,
                                    yPos,
                                    xPos + width + 2 + 3,
                                    yPos + textHeight,
                                    rectColor
                                )
                            }
                        }
                    }
                }
            }
        }


        // Draw border
        if (mc.currentScreen is GuiHudDesigner) {
            x2 = Int.MIN_VALUE

            if (modules.isEmpty()) {
                return if (side.horizontal == Horizontal.LEFT)
                    Border(0F, -1F, 20F, 20F)
                else
                    Border(0F, -1F, -20F, 20F)
            }

            for (module in modules) {
                when (side.horizontal) {
                    Horizontal.RIGHT, Horizontal.MIDDLE -> {
                        val xPos = -module.slide.toInt() - 2
                        if (x2 == Int.MIN_VALUE || xPos < x2) x2 = xPos
                    }

                    Horizontal.LEFT -> {
                        val xPos = module.slide.toInt() + 14
                        if (x2 == Int.MIN_VALUE || xPos > x2) x2 = xPos
                    }
                }
            }

            y2 = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) * modules.size

            return Border(0F, 0F, x2 - 7F, y2 - if (side.vertical == Vertical.DOWN) 1F else 0F)
        }

        AWTFontRenderer.assumeNonVolatile = false
        glColor4f(1f, 1f, 1f, 1f)
        return null
    }

    override fun updateElement() {
        modules = moduleManager.modules
            .filter { it.inArray && it.slide > 0 }
            .sortedBy { -font.getStringWidth(getDisplayString(it)) }
    }
}
