/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.designer

import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.hud.HUD.ELEMENTS
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRectNew
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRectNewInt
import net.ccbluex.liquidbounce.utils.render.RenderUtils.makeScissorBox
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.util.MathHelper
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
import java.awt.Color

class EditorPanel(private val hudDesigner: GuiHudDesigner, var x: Int, var y: Int) : MinecraftInstance() {

    var width = 80
        private set
    var height = 20
        private set
    var realHeight = 20
        private set

    private var drag = false
    private var dragX = 0
    private var dragY = 0

    private var mouseDown = false
    private var rightMouseDown = false

    private var showConfirmation = false

    private var scroll = 0

    var create = false
    private var currentElement: Element? = null

    /**
     * Draw editor panel to screen
     *
     * @param mouseX
     * @param mouseY
     * @param wheel
     */
    fun drawPanel(mouseX: Int, mouseY: Int, wheel: Int) {
        // Drag panel
        drag(mouseX, mouseY)

        // Set current element
        if (currentElement != hudDesigner.selectedElement)
            scroll = 0
        currentElement = hudDesigner.selectedElement

        // Scrolling start
        var currMouseY = mouseY
        val shouldScroll = realHeight > 200

        if (shouldScroll) {
            glPushMatrix()
            makeScissorBox(x.toFloat(), y + 1F, x + width.toFloat(), y + 200F)
            glEnable(GL_SCISSOR_TEST)

            if (y + 200 < currMouseY)
                currMouseY = -1

            if (Mouse.hasWheel() && mouseX in x..x + width && currMouseY in y..y + 200) {
                if (wheel < 0 && -scroll + 205 <= realHeight) {
                    scroll -= 12
                } else if (wheel > 0) {
                    scroll += 12
                    if (scroll > 0) scroll = 0
                }
            }
        }

        // Draw panel
        drawRectNewInt(x, y + 12, x + width, y + realHeight, Color(0, 0, 0, 150).rgb)
        when {
            create -> drawCreate(mouseX, currMouseY)
            currentElement != null -> drawEditor(mouseX, currMouseY)
            else -> drawSelection(mouseX, currMouseY)
        }

        // Scrolling end
        if (shouldScroll) {
            drawRectNewInt(x + width - 5, y + 15, x + width - 2, y + 197,
                    Color(41, 41, 41).rgb)

            val v = 197 * (-scroll / (realHeight - 170F))
            drawRectNew(x + width - 5F, y + 15 + v, x + width - 2F, y + 20 + v,
                    Color(37, 126, 255).rgb)

            glDisable(GL_SCISSOR_TEST)
            glPopMatrix()
        }

        // Save mouse states
        mouseDown = Mouse.isButtonDown(0)
        rightMouseDown = Mouse.isButtonDown(1)
    }

    /**
     * Draw create panel
     */
    private fun drawCreate(mouseX: Int, mouseY: Int) {
        height = 15 + scroll
        realHeight = 15
        width = 90

        for (element in ELEMENTS) {
            val info = element.getAnnotation(ElementInfo::class.java) ?: continue

            if (info.single && HUD.elements.any { it.javaClass == element })
                continue

            val name = info.name

            Fonts.font35.drawString(name, x + 2f, y + height.toFloat(), Color.WHITE.rgb)

            val stringWidth = Fonts.font35.getStringWidth(name)
            if (width < stringWidth + 8)
                width = stringWidth + 8

            if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width && mouseY in y + height..y + height + 10) {
                try {
                    val newElement = element.newInstance()

                    if (newElement.createElement())
                        HUD.addElement(newElement)
                } catch (e: InstantiationException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                create = false
            }

            height += 10
            realHeight += 10
        }

        drawRectNewInt(x, y, x + width, y + 12, guiColor)
        Fonts.font35.drawString("§lCreate element", x + 2F, y + 3.5F, Color.WHITE.rgb)
    }

    /**
     * Draw selection panel
     */
    private fun drawSelection(mouseX: Int, mouseY: Int) {
        height = 15 + scroll
        realHeight = 15
        width = 120

        Fonts.font35.drawString("§lCreate element", x + 2f, y.toFloat() + height, Color.WHITE.rgb)
        if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width && mouseY >= y + height
                && mouseY <= y + height + 10)
            create = true

        height += 10
        realHeight += 10

        Fonts.font35.drawString("§lReset", x + 2f, y.toFloat() + height, Color.WHITE.rgb)
        if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width && mouseY in y + height..y + height + 10) {
            showConfirmation = true // Show confirmation button
        }

        height += 15
        realHeight += 15

        Fonts.font35.drawString("§lAvailable Elements", x + 2f, y + height.toFloat(), Color.WHITE.rgb)
        height += 10
        realHeight += 10

        for (element in HUD.elements) {
            Fonts.font35.drawString(element.name, x + 2, y + height, Color.WHITE.rgb)

            val stringWidth = Fonts.font35.getStringWidth(element.name)
            if (width < stringWidth + 8)
                width = stringWidth + 8

            if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width && mouseY in y + height..y + height + 10)
                hudDesigner.selectedElement = element

            height += 10
            realHeight += 10
        }

        drawRectNewInt(x, y, x + width, y + 12, guiColor)
        glColor4f(1f, 1f, 1f, 1f)
        Fonts.font35.drawString("§lEditor", x + 2F, y + 3.5f, Color.WHITE.rgb)

        if (showConfirmation) {
            val dialogX = x
            val dialogY = y + height
            val dialogWidth = width
            val dialogHeight = 30

            drawRectNewInt(dialogX, dialogY + 10, dialogX + dialogWidth, dialogY + dialogHeight + 10, Color(0, 0, 0, 150).rgb)

            val confirmationMessage = "You sure you want to reset?"
            Fonts.font35.drawString(confirmationMessage, dialogX + 8f, dialogY.toFloat() + 12, Color.WHITE.rgb)

            val buttonWidth = 30
            val buttonHeight = 15
            val yesButtonX = dialogX + 15
            val noButtonX = dialogX + dialogWidth - buttonWidth - 18
            val buttonY = dialogY + dialogHeight - buttonHeight + 8

            // Yes button
            drawRectNewInt(yesButtonX, buttonY, yesButtonX + buttonWidth, buttonY + buttonHeight, Color.GREEN.rgb)
            Fonts.font35.drawString("Yes", yesButtonX + 8f, buttonY.toFloat() + 5, Color.WHITE.rgb)

            // No button
            drawRectNewInt(noButtonX, buttonY, noButtonX + buttonWidth, buttonY + buttonHeight, Color.RED.rgb)
            Fonts.font35.drawString("No", noButtonX + 10f, buttonY.toFloat() + 5, Color.WHITE.rgb)

            if (Mouse.isButtonDown(0) && !mouseDown) {
                if (mouseX in yesButtonX..(yesButtonX + buttonWidth) && mouseY in buttonY..(buttonY + buttonHeight)) {
                    HUD.setDefault()
                    showConfirmation = false
                } else if (mouseX in noButtonX..(noButtonX + buttonWidth) && mouseY in buttonY..(buttonY + buttonHeight)) {
                    showConfirmation = false
                }
            }
        }
    }

    /**
     * Draw editor panel
     */
    private fun drawEditor(mouseX: Int, mouseY: Int) {
        height = scroll + 15
        realHeight = 15

        val prevWidth = width
        width = 100

        val element = currentElement ?: return

        // X
        Fonts.font35.drawString("X: ${"%.2f".format(element.renderX)} (${"%.2f".format(element.x)})", x + 2, y + height, Color.WHITE.rgb)
        height += 10
        realHeight += 10

        // Y
        Fonts.font35.drawString("Y: ${"%.2f".format(element.renderY)} (${"%.2f".format(element.y)})", x + 2, y + height, Color.WHITE.rgb)
        height += 10
        realHeight += 10

        // Scale
        Fonts.font35.drawString("Scale: ${"%.2f".format(element.scale)}", x + 2, y + height, Color.WHITE.rgb)
        height += 10
        realHeight += 10

        // Horizontal
        Fonts.font35.drawString("H:", x + 2, y + height, Color.WHITE.rgb)
        Fonts.font35.drawString(element.side.horizontal.sideName,
                x + 12, y + height, Color.GRAY.rgb)

        if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width && mouseY in y + height..y + height + 10) {
            val values = Side.Horizontal.values()
            val currIndex = values.indexOf(element.side.horizontal)

            val x = element.renderX

            element.side.horizontal = values[(currIndex + 1) % values.size]
            element.x = when (element.side.horizontal) {
                Side.Horizontal.LEFT -> x
                Side.Horizontal.MIDDLE -> (ScaledResolution(mc).scaledWidth / 2) - x
                Side.Horizontal.RIGHT -> ScaledResolution(mc).scaledWidth - x
            }
        }

        height += 10
        realHeight += 10

        // Vertical
        Fonts.font35.drawString("V:", x + 2, y + height, Color.WHITE.rgb)
        Fonts.font35.drawString(element.side.vertical.sideName,
                x + 12, y + height, Color.GRAY.rgb)

        if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width && mouseY in y + height..y + height + 10) {
            val values = Side.Vertical.values()
            val currIndex = values.indexOf(element.side.vertical)

            val y = element.renderY

            element.side.vertical = values[(currIndex + 1) % values.size]
            element.y = when (element.side.vertical) {
                Side.Vertical.UP -> y
                Side.Vertical.MIDDLE -> (ScaledResolution(mc).scaledHeight / 2) - y
                Side.Vertical.DOWN -> ScaledResolution(mc).scaledHeight - y
            }

        }

        height += 10
        realHeight += 10

        // Values
        for (value in element.values) {
            if (!value.isSupported()) continue

            when (value) {
                is BoolValue -> {
                    // Title
                    Fonts.font35.drawString(value.name, x + 2, y + height, if (value.get()) Color.WHITE.rgb else Color.GRAY.rgb)

                    val stringWidth = Fonts.font35.getStringWidth(value.name)
                    if (width < stringWidth + 8)
                        width = stringWidth + 8

                    // Toggle value
                    if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width && mouseY in y + height..y + height + 10) {
                        value.toggle()
                        element.updateElement()
                        ClickGui.style.clickSound()
                    }

                    // Change pos
                    height += 10
                    realHeight += 10
                }

                is FloatValue -> {
                    val current = value.get()
                    val min = value.minimum
                    val max = value.maximum

                    // Title
                    val text = "${value.name}: §c${"%.2f".format(current)}"

                    Fonts.font35.drawString(text, x + 2, y + height, Color.WHITE.rgb)

                    val stringWidth = Fonts.font35.getStringWidth(text)
                    if (width < stringWidth + 8)
                        width = stringWidth + 8

                    // Slider
                    drawRect(x + 8F, y + height + 12F, x + prevWidth - 8F, y + height + 13F, Color.WHITE)

                    // Slider mark
                    val sliderValue = x + ((prevWidth - 18F) * (current - min) / (max - min))
                    drawRectNew(8F + sliderValue, y + height + 9F, sliderValue + 11F, y + height
                            + 15F, Color(37, 126, 255).rgb)

                    // Slider changer
                    if (Mouse.isButtonDown(0) && mouseX in x + 8..x + prevWidth && mouseY in y + height + 9..y + height + 15) {
                        val curr = MathHelper.clamp_float((mouseX - x - 8F) / (prevWidth - 18F), 0F, 1F)

                        value.set(min + (max - min) * curr)
                        element.updateElement()
                    }

                    // Change pos
                    height += 20
                    realHeight += 20
                }

                is IntegerValue -> {
                    val current = value.get()
                    val min = value.minimum
                    val max = value.maximum

                    // Title
                    val text = "${value.name}: §c$current"

                    Fonts.font35.drawString(text, x + 2, y + height, Color.WHITE.rgb)

                    val stringWidth = Fonts.font35.getStringWidth(text)
                    if (width < stringWidth + 8)
                        width = stringWidth + 8

                    // Slider
                    drawRect(x + 8F, y + height + 12F, x + prevWidth - 8F, y + height + 13F, Color.WHITE)

                    // Slider mark
                    val sliderValue = x + ((prevWidth - 18F) * (current - min) / (max - min))
                    drawRectNew(8F + sliderValue, y + height + 9F, sliderValue + 11F, y + height
                            + 15F, Color(37, 126, 255).rgb)

                    // Slider changer
                    if (Mouse.isButtonDown(0) && mouseX in x + 8..x + prevWidth && mouseY in y + height + 9..y + height + 15) {
                        val curr = MathHelper.clamp_float((mouseX - x - 8F) / (prevWidth - 18F), 0F, 1F)

                        value.set((min + (max - min) * curr).toInt())
                        element.updateElement()
                    }

                    // Change pos
                    height += 20
                    realHeight += 20
                }

                is ListValue -> {
                    // Title
                    Fonts.font35.drawString(value.name, x + 2, y + height, Color.WHITE.rgb)

                    height += 10
                    realHeight += 10

                    // Selectable values
                    for (s in value.values) {
                        // Value title
                        val text = "§c> §r$s"
                        Fonts.font35.drawString(text, x + 2, y + height, if (s == value.get()) Color.WHITE.rgb else Color.GRAY.rgb)

                        val stringWidth = Fonts.font35.getStringWidth(text)
                        if (width < stringWidth + 8)
                            width = stringWidth + 8

                        // Select value
                        if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width&& mouseY in y + height..y + height + 10) {
                            value.set(s)
                            element.updateElement()
                            ClickGui.style.clickSound()
                        }

                        // Change pos
                        height += 10
                        realHeight += 10
                    }
                }

                is FontValue -> {
                    // Title
                    val displayString = value.displayName

                    Fonts.font35.drawString(displayString, x + 2, y + height, Color.WHITE.rgb)

                    val stringWidth = Fonts.font35.getStringWidth(displayString)
                    if (width < stringWidth + 8)
                        width = stringWidth + 8

                    if (((Mouse.isButtonDown(0) && !mouseDown) || (Mouse.isButtonDown(1) && !rightMouseDown))
                        && mouseX in x..x + width && mouseY in y + height..y + height + 10
                    ) {
                        if (Mouse.isButtonDown(0)) value.next()
                        else value.previous()
                        element.updateElement()
                        ClickGui.style.clickSound()
                    }

                    height += 10
                    realHeight += 10
                }
            }
        }

        // Header
        drawRectNewInt(x, y, x + width, y + 12, guiColor)
        Fonts.font35.drawString("§l${element.name}", x + 2F, y + 3.5F, Color.WHITE.rgb)

        // Delete button
        if (!element.info.force) {
            val deleteWidth = x + width - Fonts.font35.getStringWidth("§lDelete") - 2
            Fonts.font35.drawString("§lDelete", deleteWidth.toFloat(), y + 3.5F, Color.WHITE.rgb)
            if (Mouse.isButtonDown(0) && !mouseDown && mouseX in deleteWidth..x + width && mouseY in y..y + 10)
                HUD.removeElement(element)
        }
    }

    /**
     * Drag panel
     */
    private fun drag(mouseX: Int, mouseY: Int) {
        if (Mouse.isButtonDown(0) && !mouseDown && mouseX in x..x + width && mouseY in y..y + 12) {
            drag = true
            dragX = mouseX - x
            dragY = mouseY - y
        }

        if (Mouse.isButtonDown(0) && drag) {
            x = mouseX - dragX
            y = mouseY - dragY
        } else drag = false
    }

}