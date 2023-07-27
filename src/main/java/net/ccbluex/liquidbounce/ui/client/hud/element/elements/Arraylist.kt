/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.features.module.Module
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
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.renderer.GlStateManager.resetColor
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color

/**
 * CustomHUD Arraylist element
 *
 * Shows a list of enabled modules
 */
@ElementInfo(name = "Arraylist", single = true)
class Arraylist(x: Double = 1.0, y: Double = 2.0, scale: Float = 1F,
                side: Side = Side(Horizontal.RIGHT, Vertical.UP)) : Element(x, y, scale, side) {

    private val textColor by ListValue("Text-Color", arrayOf("Custom", "Random", "Rainbow"), "Custom")
    private val textAlpha by IntegerValue("Text-Alpha", 255, 0..255) { textColor == "Custom" }
    private val textRed by IntegerValue("Text-R", 0, 0..255) { textColor == "Custom" && textAlpha > 0 }
    private val textGreen by IntegerValue("Text-G", 111, 0..255) { textColor == "Custom" && textAlpha > 0 }
    private val textBlue by IntegerValue("Text-B", 255, 0..255) { textColor == "Custom" && textAlpha > 0 }

    private val rectMode by ListValue("Rect", arrayOf("None", "Left", "Right"), "None")
    private val rectColor by ListValue("Rect-Color", arrayOf("Custom", "Random", "Rainbow"), "Rainbow") { rectMode != "None" }
    
    private val rectAlpha by IntegerValue("Rect-Alpha", 255, 0..255) { rectMode != "None" && rectColor == "Custom" }
    private val rectRed by IntegerValue("Rect-R", 255, 0..255) { rectMode != "None" && rectColor == "Custom" && rectAlpha > 0 }
    private val rectGreen by IntegerValue("Rect-G", 255, 0..255) { rectMode != "None" && rectColor == "Custom" && rectAlpha > 0 }
    private val rectBlue by IntegerValue("Rect-B", 255, 0..255) { rectMode != "None" && rectColor == "Custom" && rectAlpha > 0 }

    private val backgroundColor by ListValue("Background-Color", arrayOf("Custom", "Random", "Rainbow"), "Custom")
    private val backgroundAlpha by IntegerValue("Background-Alpha", 0, 0..255) { backgroundColor == "Custom" }
    private val backgroundRed by IntegerValue("Background-R", 0, 0..255) { backgroundColor == "Custom" && backgroundAlpha > 0 }
    private val backgroundGreen by IntegerValue("Background-G", 0, 0..255) { backgroundColor == "Custom" && backgroundAlpha > 0 }
    private val backgroundBlue by IntegerValue("Background-B", 0, 0..255) { backgroundColor == "Custom" && backgroundAlpha > 0 }

    private fun isColorModeUsed(value: String) = textColor == value || rectMode == value || backgroundColor == value
    private val saturation by FloatValue("Random-Saturation", 0.9f, 0f..1f) { isColorModeUsed("Random") }
    private val brightness by FloatValue("Random-Brightness", 1f, 0f..1f) { isColorModeUsed("Random") }
    private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }
    private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { isColorModeUsed("Rainbow") }

    private val tagsColor by ListValue("Tags-Color", arrayOf("Custom", "Random", "Rainbow"), "Custom")
    private val tagsAlpha by IntegerValue("Tags-Alpha", 0, 0..255) { tagsColor == "Custom" }
    private val tagsRed by IntegerValue("Tags-R", 0, 0..255) { tagsColor == "Custom" && tagsAlpha > 0 }
    private val tagsGreen by IntegerValue("Tags-G", 0, 0..255) { tagsColor == "Custom" && tagsAlpha > 0 }
    private val tagsBlue by IntegerValue("Tags-B", 0, 0..255) { tagsColor == "Custom" && tagsAlpha > 0 }
    private val tagsPrefix by TextValue("Tags-Prefix", " §7") { tagsAlpha > 0 }
    private val tagsSuffix by TextValue("Tags-Suffix", "") { tagsAlpha > 0 }


    private val font by FontValue("Font", Fonts.font40)
    private val textShadow by BoolValue("Shadow", true)
    private val upperCase by BoolValue("UpperCase", false)
    private val space by FloatValue("Space", 0F, 0F..5F)
    private val textHeight by FloatValue("TextHeight", 11F, 1F..20F)
    private val textY by FloatValue("TextY", 1F, 0F..20F)

    companion object {
        val spacedModules by BoolValue("SpacedModules", false)
    }

    private var x2 = 0
    private var y2 = 0F

    private var modules = emptyList<Module>()

    init {
        updateTagDetails()
    }

    fun updateTagDetails() {
    }

    private fun getDisplayString(module: Module): String {
        val displayString = when {
            tagsAlpha > 0 && !module.tag.isNullOrEmpty() -> module.getName() + tagsPrefix + module.tag + tagsSuffix
            else -> module.getName()
        }

        return if (upperCase) displayString.uppercase() else displayString
    }

    override fun drawElement(): Border? {

        AWTFontRenderer.assumeNonVolatile = true

        // Slide animation - update every render
        val delta = deltaTime

        for (module in moduleManager.modules) {
            if (!module.inArray || (!module.state && module.slide == 0F)) continue

            // TODO
            val displayString = getDisplayString(module)

            val width = font.getStringWidth(displayString)

            if (module.state) {
                if (module.slide < width) {
                    module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                    module.slideStep += delta / 4F
                }
            } else if (module.slide > 0) {
                module.slide = AnimationUtils.easeOut(module.slideStep, width.toFloat()) * width
                module.slideStep -= delta / 4F
            }

            module.slide = module.slide.coerceIn(0F, width.toFloat())
            module.slideStep = module.slideStep.coerceIn(0F, width.toFloat())
        }

        // Draw arraylist
        val customColor = Color(textRed, textGreen, textBlue, textAlpha).rgb
        val rectCustomColor = Color(rectRed, rectGreen, rectBlue, rectAlpha).rgb
        val backgroundCustomColor = Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha).rgb
        val tagsCustomColor = Color(tagsRed, tagsGreen, tagsBlue, tagsAlpha).rgb
        val textSpacer = textHeight + space

        when (side.horizontal) {
            Horizontal.RIGHT, Horizontal.MIDDLE -> {
                modules.forEachIndexed { index, module ->
                    val displayString = getDisplayString(module)

                    val xPos = -module.slide - 2
                    val yPos = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) *
                            if (side.vertical == Vertical.DOWN) index + 1 else index
                    val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb

                    RainbowShader.begin(backgroundColor == "Rainbow", if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                        drawRect(xPos - if (rectMode == "Right") 5 else 2, yPos, if (rectMode == "Right") -3F else 0F, yPos + textHeight,
                            when (backgroundColor) {
                                "Rainbow" -> 0xFF shl 24
                                "Random" -> moduleColor
                                else -> backgroundCustomColor
                            }
                        )
                    }

                    resetColor()
                    RainbowFontShader.begin(textColor == "Rainbow", if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                        font.drawString(displayString, xPos - if (rectMode == "Right") 3 else 0, yPos + textY,
                            when (textColor) {
                                "Rainbow" -> 0
                                "Random" -> moduleColor
                                else -> customColor
                            }, textShadow
                        )
                    }

                    if (rectMode != "None") {
                        RainbowShader.begin(rectColor == "Rainbow", if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                            val rectColor = when (rectColor) {
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
            }

            Horizontal.LEFT -> {
                modules.forEachIndexed { index, module ->
                    val displayString = getDisplayString(module)

                    val width = font.getStringWidth(displayString)
                    val xPos = -(width - module.slide) + if (rectMode == "Left") 5 else 2
                    val yPos = (if (side.vertical == Vertical.DOWN) -textSpacer else textSpacer) *
                            if (side.vertical == Vertical.DOWN) index + 1 else index
                    val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb

                    RainbowShader.begin(backgroundColor == "Rainbow", if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                        drawRect(0F, yPos, xPos + width + if (rectMode == "Right") 5 else 2, yPos + textHeight,
                            when (backgroundColor) {
                                "Rainbow" -> 0
                                "Random" -> moduleColor
                                else -> backgroundCustomColor
                            }
                        )
                    }

                    resetColor()
                    RainbowFontShader.begin(textColor == "Rainbow", if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                        font.drawString(displayString, xPos, yPos + textY,
                            when (textColor) {
                                "Rainbow" -> 0
                                "Random" -> moduleColor
                                else -> customColor
                            }, textShadow
                        )
                    }

                    RainbowShader.begin(rectColor == "Rainbow", if (rainbowX == 0f) 0f else 1f / rainbowX, if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
                        if (rectMode != "None") {
                            val rectColor = when (rectColor) {
                                "Rainbow" -> 0
                                "Random" -> moduleColor
                                else -> rectCustomColor
                            }

                            when (rectMode) {
                                "Left" -> drawRect(0F, yPos - 1, 3F, yPos + textHeight, rectColor)
                                "Right" -> drawRect(xPos + width + 2, yPos, xPos + width + 2 + 3, yPos + textHeight, rectColor)
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
