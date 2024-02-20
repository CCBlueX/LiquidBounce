/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
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
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRectNewInt
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.StringUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
object LiquidBounceStyle : Style() {
    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        drawBorderedRect(panel.x - if (panel.scrollbar) 4 else 0, panel.y, panel.x + panel.width, panel.y + panel.height + panel.fade, 1, Color.GRAY.rgb, Int.MIN_VALUE)

        val xPos = panel.x - (font35.getStringWidth(StringUtils.stripControlCodes(panel.name)) - 100) / 2
        font35.drawString(panel.name, xPos, panel.y + 7, Color.WHITE.rgb)

        if (panel.scrollbar && panel.fade > 0) {
            drawRectNewInt(panel.x - 2, panel.y + 21, panel.x, panel.y + 16 + panel.fade, Color.DARK_GRAY.rgb)

            val visibleRange = panel.getVisibleRange()
            val minY = panel.y + 21 + panel.fade *
                    if (visibleRange.first > 0) visibleRange.first / panel.elements.lastIndex.toFloat()
                    else 0f
            val maxY = panel.y + 16 + panel.fade *
                    if (visibleRange.last > 0) visibleRange.last / panel.elements.lastIndex.toFloat()
                    else 0f

            drawRectNewInt(panel.x - 2, minY.roundToInt(), panel.x, maxY.roundToInt(), Color.GRAY.rgb)
        }
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.lines()

        val width = lines.maxOfOrNull { font35.getStringWidth(it) + 14 } ?: return // Makes no sense to render empty lines
        val height = font35.fontHeight * lines.size + 3

        // Don't draw hover text beyond window boundaries
        val (scaledWidth, scaledHeight) = ScaledResolution(mc)
        val x = mouseX.clamp(0, (scaledWidth / scale - width).roundToInt())
        val y = mouseY.clamp(0, (scaledHeight / scale - height).roundToInt())

        drawBorderedRect(x + 9, y, x + width, y + height, 1, Color.GRAY.rgb, Int.MIN_VALUE)
        lines.forEachIndexed { index, text ->
            font35.drawString(text, x + 12, y + 3 + (font35.fontHeight) * index, Int.MAX_VALUE)
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        val xPos = buttonElement.x - (font35.getStringWidth(buttonElement.displayName) - 100) / 2
        font35.drawString(buttonElement.displayName, xPos, buttonElement.y + 6, buttonElement.color)
    }

    override fun drawModuleElementAndClick(mouseX: Int, mouseY: Int, moduleElement: ModuleElement, mouseButton: Int?): Boolean {
        val xPos = moduleElement.x - (font35.getStringWidth(moduleElement.displayName) - 100) / 2
        font35.drawString(
            moduleElement.displayName, xPos,
            moduleElement.y + 6,
            if (moduleElement.module.state) {
                if (moduleElement.module.isActive) guiColor
                // Make inactive modules have alpha set to 100
                else (guiColor and 0x00FFFFFF) or (0x64 shl 24)
            } else Int.MAX_VALUE
        )

        val moduleValues = moduleElement.module.values.filter { it.isSupported() }
        if (moduleValues.isNotEmpty()) {
            font35.drawString(
                if (moduleElement.showSettings) "-" else "+",
                moduleElement.x + moduleElement.width - 8, moduleElement.y + moduleElement.height / 2, Color.WHITE.rgb
            )

            if (moduleElement.showSettings) {
                var yPos = moduleElement.y + 4

                val minX = moduleElement.x + moduleElement.width + 4
                val maxX = moduleElement.x + moduleElement.width + moduleElement.settingsWidth

                for (value in moduleValues) {
                    assumeNonVolatile = value.get() is Number

                    when (value) {
                        is BoolValue -> {
                            val text = value.name

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            drawRectNewInt(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                            if (mouseButton == 0
                                && mouseX in minX..maxX
                                && mouseY in yPos + 2..yPos + 14
                            ) {
                                value.toggle()
                                clickSound()
                                return true
                            }

                            font35.drawString(text, minX + 2, yPos + 4,
                                if (value.get()) guiColor else Int.MAX_VALUE
                            )

                            yPos += 12
                        }
                        is ListValue -> {
                            val text = value.name

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 16

                            if (mouseButton == 0
                                && mouseX in minX..maxX
                                && mouseY in yPos + 2..yPos + 14
                            ) {
                                value.openList = !value.openList
                                clickSound()
                                return true
                            }

                            drawRectNewInt(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                            font35.drawString("§c$text", minX + 2, yPos + 4, Color.WHITE.rgb)
                            font35.drawString(
                                if (value.openList) "-" else "+",
                                maxX - if (value.openList) 5 else 6, yPos + 4, Color.WHITE.rgb
                            )

                            yPos += 12

                            for (valueOfList in value.values) {
                                moduleElement.settingsWidth = font35.getStringWidth(valueOfList) + 16

                                if (value.openList) {
                                    drawRectNewInt(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                                    if (mouseButton == 0
                                        && mouseX in minX..maxX
                                        && mouseY in yPos + 2..yPos + 14
                                    ) {
                                        value.set(valueOfList)
                                        clickSound()
                                        return true
                                    }

                                    font35.drawString(">", minX + 2, yPos + 4,
                                        if (value.get() == valueOfList) guiColor else Int.MAX_VALUE
                                    )
                                    font35.drawString(valueOfList, minX + 10, yPos + 4,
                                        if (value.get() == valueOfList) guiColor else Int.MAX_VALUE
                                    )

                                    yPos += 12
                                }
                            }
                        }
                        is FloatValue -> {
                            val text = value.name + "§f: §c" + round(value.get())
                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8


                            if ((mouseButton == 0 || sliderValueHeld == value)
                                && mouseX in minX..maxX
                                && mouseY in yPos + 15..yPos + 21
                            ) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.set(round(value.minimum + (value.maximum - value.minimum) * percentage).coerceIn(value.range))

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

                            drawRectNewInt(minX, yPos + 2, maxX, yPos + 24, Int.MIN_VALUE)
                            drawRectNewInt(minX + 4, yPos + 18, maxX - 4, yPos + 19, Int.MAX_VALUE)

                            val displayValue = value.get().coerceIn(value.range)
                            val sliderValue = (moduleElement.x + moduleElement.width + (moduleElement.settingsWidth - 12) * (displayValue - value.minimum) / (value.maximum - value.minimum)).roundToInt()
                            drawRectNewInt(8 + sliderValue, yPos + 15, sliderValue + 11, yPos + 21, guiColor)

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 22
                        }
                        is IntegerValue -> {
                            val text = value.name + "§f: §c" + if (value is BlockValue) getBlockName(value.get()) + " (" + value.get() + ")" else value.get()

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            if ((mouseButton == 0 || sliderValueHeld == value)
                                && mouseX in minX..maxX
                                && mouseY in yPos + 15..yPos + 21
                            ) {
                                val percentage = (mouseX - minX - 4) / (maxX - minX - 8).toFloat()
                                value.set((value.minimum + (value.maximum - value.minimum) * percentage).roundToInt().coerceIn(value.range))

                                // Keep changing this slider until mouse is unpressed.
                                sliderValueHeld = value

                                // Stop rendering and interacting only when this event was triggered by a mouse click.
                                if (mouseButton == 0) return true
                            }

                            drawRectNewInt(minX, yPos + 2, maxX, yPos + 24, Int.MIN_VALUE)
                            drawRectNewInt(minX + 4, yPos + 18, maxX - 4, yPos + 19, Int.MAX_VALUE)

                            val displayValue = value.get().coerceIn(value.range)
                            val sliderValue = moduleElement.x + moduleElement.width + (moduleElement.settingsWidth - 12) * (displayValue - value.minimum) / (value.maximum - value.minimum)
                            drawRectNewInt(8 + sliderValue, yPos + 15, sliderValue + 11, yPos + 21, guiColor)

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 22
                        }
                        is FontValue -> {
                            val displayString = value.displayName
                            moduleElement.settingsWidth = font35.getStringWidth(displayString) + 8

                            if (mouseButton != null
                                && mouseX in minX..maxX
                                && mouseY in yPos + 4..yPos + 12
                            ) {
                                // Cycle to next font when left-clicked, previous when right-clicked.
                                if (mouseButton == 0) value.next()
                                else value.previous()
                                clickSound()
                                return true
                            }

                            drawRectNewInt(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                            font35.drawString(displayString, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 11
                        }
                        else -> {
                            val text = value.name + "§f: §c" + value.get()

                            moduleElement.settingsWidth = font35.getStringWidth(text) + 8

                            drawRectNewInt(minX, yPos + 2, maxX, yPos + 14, Int.MIN_VALUE)

                            font35.drawString(text, minX + 2, yPos + 4, Color.WHITE.rgb)

                            yPos += 12
                        }
                    }
                }

                moduleElement.settingsHeight = yPos - moduleElement.y - 4

                if (moduleElement.settingsWidth > 0 && yPos > moduleElement.y + 4) {
                    if (mouseButton != null
                        && mouseX in minX..maxX
                        && mouseY in moduleElement.y + 6..yPos + 2) return true

                    drawBorderedRect(minX, moduleElement.y + 6, maxX, yPos + 2, 1, Color.GRAY.rgb, 0)
                }
            }
        }
        return false
    }
}