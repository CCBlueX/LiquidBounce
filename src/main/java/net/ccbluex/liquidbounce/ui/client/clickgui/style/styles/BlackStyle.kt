/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts.font35
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawFilledCircle
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRectNewInt
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.StringUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
object BlackStyle : Style() {
    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        drawBorderedRect(
            panel.x, panel.y - 3, panel.x + panel.width, panel.y + 17, 3, Color(20, 20, 20).rgb, Color(20, 20, 20).rgb
        )

        if (panel.fade > 0) {
            drawBorderedRect(
                panel.x,
                panel.y + 17,
                panel.x + panel.width,
                panel.y + 19 + panel.fade,
                3,
                Color(40, 40, 40).rgb,
                Color(40, 40, 40).rgb
            )
            drawBorderedRect(
                panel.x,
                panel.y + 17 + panel.fade,
                panel.x + panel.width,
                panel.y + 24 + panel.fade,
                3,
                Color(20, 20, 20).rgb,
                Color(20, 20, 20).rgb
            )
        }

        val xPos = panel.x - (font35.getStringWidth("§f" + StringUtils.stripControlCodes(panel.name)) - 100) / 2
        font35.drawString(panel.name, xPos, panel.y + 4, Color.WHITE.rgb)
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()

        val width =
            lines.maxOfOrNull { font35.getStringWidth(it) + 14 } ?: return // Makes no sense to render empty lines
        val height = (font35.fontHeight * lines.size) + 3

        // Don't draw hover text beyond window boundaries
        val (scaledWidth, scaledHeight) = ScaledResolution(mc)
        val x = mouseX.clamp(0, (scaledWidth / scale - width).roundToInt())
        val y = mouseY.clamp(0, (scaledHeight / scale - height).roundToInt())

        drawBorderedRect(x + 9, y, x + width, y + height, 3, Color(40, 40, 40).rgb, Color(40, 40, 40).rgb)

        lines.forEachIndexed { index, text ->
            font35.drawString(text, x + 12, y + 3 + (font35.fontHeight) * index, Color.WHITE.rgb)
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        drawRectNewInt(
            buttonElement.x - 1,
            buttonElement.y - 1,
            buttonElement.x + buttonElement.width + 1,
            buttonElement.y + buttonElement.height + 1,
            getHoverColor(
                if (buttonElement.color != Int.MAX_VALUE) Color(20, 20, 20) else Color(40, 40, 40),
                buttonElement.hoverTime
            )
        )

        font35.drawString(buttonElement.displayName, buttonElement.x + 5, buttonElement.y + 5, Color.WHITE.rgb)
    }

    override fun drawModuleElementAndClick(mouseX: Int, mouseY: Int, moduleElement: ModuleElement, mouseButton: Int?): Boolean {
        drawRectNewInt(
            moduleElement.x - 1, moduleElement.y - 1, moduleElement.x + moduleElement.width + 1, moduleElement.y + moduleElement.height + 1,
            getHoverColor(Color(40, 40, 40), moduleElement.hoverTime)
        )
        drawRectNewInt(
            moduleElement.x - 1, moduleElement.y - 1, moduleElement.x + moduleElement.width + 1, moduleElement.y + moduleElement.height + 1,
            getHoverColor(Color(20, 20, 20, moduleElement.slowlyFade), moduleElement.hoverTime, !moduleElement.module.isActive)
        )

        font35.drawString(moduleElement.displayName, moduleElement.x + 5, moduleElement.y + 5,
            if (moduleElement.module.state && !moduleElement.module.isActive) Color(255, 255, 255, 128).rgb
            else Color.WHITE.rgb
        )

        // Draw settings
        val moduleValues = moduleElement.module.values.filter { it.isSupported() }
        if (moduleValues.isNotEmpty()) {
            font35.drawString(
                if (moduleElement.showSettings) "<" else ">",
                moduleElement.x + moduleElement.width - 8,
                moduleElement.y + 5,
                Color.WHITE.rgb
            )

            if (moduleElement.showSettings) {
                var yPos = moduleElement.y + 6

                val minX = moduleElement.x + moduleElement.width + 4
                val maxX = moduleElement.x + moduleElement.width + moduleElement.settingsWidth

                if (moduleElement.settingsWidth > 0 && moduleElement.settingsHeight > 0) drawBorderedRect(
                    minX,
                    yPos,
                    maxX,
                    yPos + moduleElement.settingsHeight,
                    3,
                    Color(20, 20, 20).rgb,
                    Color(40, 40, 40).rgb
                )

                for (value in moduleValues) {
                    assumeNonVolatile = value.get() is Number

                    when (value) {
                        is BoolValue -> {
                            val text = value.name

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos..yPos + 12) {
                                value.toggle()
                                clickSound()
                                return true
                            }

                            font35.drawString(
                                text, minX + 2, yPos + 2, if (value.get()) Color.WHITE.rgb else Int.MAX_VALUE
                            )

                            yPos += 11
                        }

                        is ListValue -> {
                            val text = value.name

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 16

                            if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos..yPos + font35.fontHeight) {
                                value.openList = !value.openList
                                clickSound()
                                return true
                            }

                            font35.drawString(text, minX + 2, yPos + 2, Color.WHITE.rgb)
                            font35.drawString(
                                if (value.openList) "-" else "+",
                                (maxX - if (value.openList) 5 else 6),
                                yPos + 2,
                                Color.WHITE.rgb
                            )

                            yPos += font35.fontHeight + 1

                            for (valueOfList in value.values) {
                                moduleElement.settingsWidth = font35.getStringWidth("> $valueOfList") + 12

                                if (value.openList) {
                                    if (mouseButton == 0 && mouseX in minX..maxX && mouseY in yPos..yPos + 9) {
                                        value.set(valueOfList)
                                        clickSound()
                                        return true
                                    }

                                    font35.drawString(
                                        "> $valueOfList",
                                        minX + 2,
                                        yPos + 2,
                                        if (value.get() == valueOfList) Color.WHITE.rgb else Int.MAX_VALUE
                                    )

                                    yPos += font35.fontHeight + 1
                                }
                            }
                            if (!value.openList) {
                                yPos += 1
                            }
                        }

                        is FloatValue -> {
                            val text = value.name + "§f: " + round(value.get())

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            val x = minX + 4
                            val y = yPos + 14
                            val width = moduleElement.settingsWidth - 12
                            val color = Color(20, 20, 20)

                            val displayValue = value.get().coerceIn(value.range)
                            val sliderValue =
                                (x + width * (displayValue - value.minimum) / (value.maximum - value.minimum)).roundToInt()

                            if ((mouseButton == 0 || sliderValueHeld == value) && mouseX in x..x + width && mouseY in y - 2..y + 5) {
                                val percentage = (mouseX - x) / width.toFloat()
                                value.set(
                                    round(value.minimum + (value.maximum - value.minimum) * percentage).coerceIn(
                                        value.range
                                    )
                                )

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

                            drawRectNewInt(x, y, x + width, y + 2, Int.MAX_VALUE)
                            drawRectNewInt(x, y, sliderValue, y + 2, color.rgb)
                            drawFilledCircle(sliderValue, y + 1, 3f, color)

                            font35.drawString(text, minX + 2, yPos + 3, Color.WHITE.rgb)

                            yPos += 19
                        }

                        is IntegerValue -> {
                            val text =
                                value.name + "§f: " + if (value is BlockValue) getBlockName(value.get()) + " (" + value.get() + ")" else value.get()

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            val x = minX + 4
                            val y = yPos + 14
                            val width = moduleElement.settingsWidth - 12
                            val color = Color(20, 20, 20)

                            val displayValue = value.get().coerceIn(value.range)
                            val sliderValue =
                                x + width * (displayValue - value.minimum) / (value.maximum - value.minimum)

                            if ((mouseButton == 0 || sliderValueHeld == value) && mouseX in x..x + width && mouseY in y - 2..y + 5) {
                                val percentage = (mouseX - x) / width.toFloat()
                                value.set(
                                    (value.minimum + (value.maximum - value.minimum) * percentage).roundToInt()
                                        .coerceIn(value.range)
                                )

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

                            drawRectNewInt(x, y, x + width, y + 2, Int.MAX_VALUE)
                            drawRectNewInt(x, y, sliderValue, y + 2, color.rgb)
                            drawFilledCircle(sliderValue, y + 1, 3f, color)

                            font35.drawString(text, minX + 2, yPos + 3, Color.WHITE.rgb)

                            yPos += 19
                        }

                        is FontValue -> {
                            val displayString = value.displayName
                            moduleElement.settingsWidth = font35.getStringWidth(displayString) + 8

                            font35.drawString(displayString, minX + 2, yPos + 2, Color.WHITE.rgb)

                            if (mouseButton != null && mouseX in minX..maxX && mouseY in yPos..yPos + 12) {
                                // Cycle to next font when left-clicked, previous when right-clicked.
                                if (mouseButton == 0) value.next()
                                else value.previous()
                                clickSound()
                                return true
                            }

                            yPos += 11
                        }

                        else -> {
                            val text = value.name + "§f: " + value.get()

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 12
                        }
                    }
                }

                moduleElement.settingsHeight = yPos - moduleElement.y - 6

                if (mouseButton != null && mouseX in minX..maxX && mouseY in moduleElement.y + 6..yPos + 2) return true
            }
        }

        return false
    }
}