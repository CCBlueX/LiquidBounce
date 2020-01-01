package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager.getModules
import net.ccbluex.liquidbounce.ui.client.hud.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Facing.Horizontal
import net.ccbluex.liquidbounce.ui.client.hud.element.Facing.Vertical
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ElementInfo(name = "Arraylist")
class Arraylist : Element() {

    private val colorModeValue = ListValue("Text-Color", arrayOf("Custom", "Random", "Rainbow"), "Custom")
    private val colorRedValue = IntegerValue("Text-R", 0, 0, 255)
    private val colorGreenValue = IntegerValue("Text-G", 111, 0, 255)
    private val colorBlueValue = IntegerValue("Text-B", 255, 0, 255)
    private val rectColorModeValue = ListValue("Rect-Color", arrayOf("Custom", "Random", "Rainbow"), "Rainbow")
    private val rectColorRedValue = IntegerValue("Rect-R", 255, 0, 255)
    private val rectColorGreenValue = IntegerValue("Rect-G", 255, 0, 255)
    private val rectColorBlueValue = IntegerValue("Rect-B", 255, 0, 255)
    private val rectColorBlueAlpha = IntegerValue("Rect-Alpha", 255, 0, 255)
    private val saturationValue = FloatValue("Random-Saturation", 0.9f, 0f, 1f)
    private val brightnessValue = FloatValue("Random-Brightness", 1f, 0f, 1f)
    private val tags = BoolValue("Tags", true)
    private val shadow = BoolValue("ShadowText", true)
    private val backgroundColorModeValue = ListValue("Background-Color", arrayOf("Custom", "Random", "Rainbow"), "Custom")
    private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
    private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
    private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
    private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 0, 0, 255)
    private val rectValue = ListValue("Rect", arrayOf("None", "Left", "Right"), "None")
    private val upperCaseValue = BoolValue("UpperCase", false)
    private val spaceValue = FloatValue("Space", 0F, 0F, 5F)
    private val textHeightValue = FloatValue("TextHeight", 11F, 1F, 20F)
    private val textYValue = FloatValue("TextY", 1F, 0F, 20F)
    private val tagsArrayColor = BoolValue("TagsArrayColor", false)

    var fontRenderer: FontRenderer = Fonts.font40
        private set

    private var x2 = 0
    private var y2 = 0F

    private var modules = emptyList<Module>()

    override fun drawElement() {
        // Slide animation - update every render
        val delta = RenderUtils.deltaTime

        for (module in getModules()) {
            if (module.state && module.showArray()) {
                var displayString = if (!tags.get())
                    module.name
                else if (tagsArrayColor.get())
                    module.colorlessTagName
                else module.tagName

                if (upperCaseValue.get()) displayString = displayString.toUpperCase()

                val width = fontRenderer.getStringWidth(displayString)

                if (module.slide < width)
                    module.slide += 0.15F * delta else if (module.slide > width) module.slide -= 0.15F * delta

                if (module.slide > width)
                    module.slide = width.toFloat()
            } else if (module.slide > 0)
                module.slide -= 0.15F * delta

            if (module.slide < 0)
                module.slide = 0f
        }

        // Draw arraylist
        val location = locationFromFacing
        val colorMode = colorModeValue.get()
        val rectColorMode = rectColorModeValue.get()
        val backgroundColorMode = backgroundColorModeValue.get()
        val customColor = Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get()).rgb
        val rectCustomColor = Color(rectColorRedValue.get(), rectColorGreenValue.get(), rectColorBlueValue.get(),
                rectColorBlueAlpha.get()).rgb
        val space = spaceValue.get()
        val textHeight = textHeightValue.get()
        val textY = textYValue.get()
        val rectMode = rectValue.get()
        val backgroundCustomColor = Color(backgroundColorRedValue.get(), backgroundColorGreenValue.get(),
                backgroundColorBlueValue.get(), backgroundColorAlphaValue.get()).rgb
        val textShadow = shadow.get()
        val textSpacer = textHeight + space
        val saturation = saturationValue.get()
        val brightness = brightnessValue.get()


        when (facing.horizontal!!) {
            Horizontal.RIGHT, Horizontal.MIDDLE -> {
                modules.forEachIndexed { index, module ->
                    var displayString = if (!tags.get())
                        module.name
                    else if (tagsArrayColor.get())
                        module.colorlessTagName
                    else module.tagName

                    if (upperCaseValue.get())
                        displayString = displayString.toUpperCase()

                    val xPos = location[0] - module.slide - 2
                    val yPos = location[1] + (if (facing.vertical == Vertical.DOWN) -textSpacer else textSpacer) *
                            if (facing.vertical == Vertical.DOWN) index + 1 else index
                    val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb

                    RenderUtils.drawRect(
                            xPos - if (rectMode.equals("right", true)) 5 else 2,
                            yPos - if (index == 0) 1 else 0,
                            location[0] - (if (rectMode.equals("right", true)) 3 else 0).toFloat(),
                            yPos + textHeight, when {
                        backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(400000000L * index).rgb
                        backgroundColorMode.equals("Random", ignoreCase = true) -> moduleColor
                        else -> backgroundCustomColor
                    }
                    )

                    fontRenderer.drawString(displayString, xPos - if (rectMode.equals("right", true)) 3 else 0, yPos + textY, when {
                        colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(400000000L * index).rgb
                        colorMode.equals("Random", ignoreCase = true) -> moduleColor
                        else -> customColor
                    }, textShadow)

                    if (!rectMode.equals("none", true)) {
                        val rectColor = when {
                            rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(400000000L * index).rgb
                            rectColorMode.equals("Random", ignoreCase = true) -> moduleColor
                            else -> rectCustomColor
                        }

                        when {
                            rectMode.equals("left", true) -> RenderUtils.drawRect(xPos - 5, yPos - 1, xPos - 2, yPos + textHeight,
                                    rectColor)
                            rectMode.equals("right", true) -> RenderUtils.drawRect(location[0] - 3F, yPos - 1F, location[0].toFloat(),
                                    yPos + textHeight, rectColor)
                        }
                    }
                }
            }

            Horizontal.LEFT -> {
                modules.forEachIndexed { index, module ->
                    var displayString = if (!tags.get())
                        module.name
                    else if (tagsArrayColor.get())
                        module.colorlessTagName
                    else module.tagName

                    if (upperCaseValue.get())
                        displayString = displayString.toUpperCase()

                    val width = fontRenderer.getStringWidth(displayString)
                    val xPos = location[0] - (width - module.slide) + if (rectMode.equals("left", true)) 5 else 2
                    val yPos = location[1] + (if (facing.vertical == Vertical.DOWN) -textSpacer else textSpacer) *
                            if (facing.vertical == Vertical.DOWN) index + 1 else index
                    val moduleColor = Color.getHSBColor(module.hue, saturation, brightness).rgb

                    RenderUtils.drawRect(
                            location[0].toFloat(),
                            yPos - if (index == 0) 1 else 0,
                            xPos + width + if (rectMode.equals("right", true)) 5 else 2,
                            yPos + textHeight, when {
                        backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(400000000L * index).rgb
                        backgroundColorMode.equals("Random", ignoreCase = true) -> moduleColor
                        else -> backgroundCustomColor
                    }
                    )

                    fontRenderer.drawString(displayString, xPos, yPos + textY, when {
                        colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(400000000L * index).rgb
                        colorMode.equals("Random", ignoreCase = true) -> moduleColor
                        else -> customColor
                    }, textShadow)

                    if (!rectMode.equals("none", true)) {
                        val rectColor = when {
                            rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(400000000L * index).rgb
                            rectColorMode.equals("Random", ignoreCase = true) -> moduleColor
                            else -> rectCustomColor
                        }

                        when {
                            rectMode.equals("left", true) -> RenderUtils.drawRect(location[0].toFloat(),
                                    yPos - 1, location[0] + 3F, yPos + textHeight, rectColor)
                            rectMode.equals("right", true) ->
                                RenderUtils.drawRect(xPos + width + 2, yPos - 1, xPos + width + 2 + 3,
                                        yPos + textHeight, rectColor)
                        }
                    }
                }
            }
        }

        // Draw border
        if (mc.currentScreen is GuiHudDesigner) {
            x2 = Int.MIN_VALUE

            for (module in modules) {
                when (facing.horizontal!!) {
                    Horizontal.RIGHT, Horizontal.MIDDLE -> {
                        val xPos = location[0] - module.slide.toInt() - 2
                        if (x2 == Int.MIN_VALUE || xPos < x2) x2 = xPos
                    }
                    Horizontal.LEFT -> {
                        val xPos = location[0] + module.slide.toInt() + 14
                        if (x2 == Int.MIN_VALUE || xPos > x2) x2 = xPos
                    }
                }
            }
            y2 = location[1] + (if (facing.vertical == Vertical.DOWN) -textSpacer else textSpacer) * modules.size

            RenderUtils.drawBorderedRect(location[0].toFloat(), location[1] - 1F, x2 - 7F, y2, 3F, Int.MIN_VALUE, 0)
        }
        GlStateManager.resetColor()
    }

    override fun updateElement() {
        modules = getModules()
                .filter { it.showArray() && it.slide > 0 }
                .sortedBy { -fontRenderer.getStringWidth(if (upperCaseValue.get()) (if (!tags.get()) it.name else if (tagsArrayColor.get()) it.colorlessTagName else it.tagName).toUpperCase() else if (!tags.get()) it.name else if (tagsArrayColor.get()) it.colorlessTagName else it.tagName) }
    }

    override fun destroyElement() {}

    override fun handleMouseClick(mouseX: Int, mouseY: Int, mouseButton: Int) {}

    override fun handleKey(c: Char, keyCode: Int) {}

    override fun isMouseOverElement(mouseX: Int, mouseY: Int): Boolean {
        val location = locationFromFacing
        val widthCollide = when (facing.horizontal!!) {
            Horizontal.RIGHT, Horizontal.MIDDLE ->
                mouseX >= x2 - 7 && mouseX <= location[0]
            Horizontal.LEFT ->
                mouseX <= x2 - 7 && mouseX >= location[0]
        }

        val heightCollide = when (facing.vertical!!) {
            Vertical.UP, Vertical.MIDDLE ->
                mouseY >= location[1] - 2 && mouseY <= y2
            Vertical.DOWN ->
                mouseY <= location[1] - 2 && mouseY >= y2
        }

        return widthCollide && heightCollide
    }

    fun setFontRenderer(fontRenderer: FontRenderer): Arraylist {
        this.fontRenderer = fontRenderer
        return this
    }
}