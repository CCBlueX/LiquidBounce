/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui

import net.ccbluex.liquidbounce.LiquidBounce.clickGui
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.fadeSpeedValue
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.maxElementsValue
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scaleValue
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.Element
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.lwjgl.input.Mouse
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
abstract class Panel(val name: String, x: Int, y: Int, val width: Int, val height: Int, var open: Boolean) : MinecraftInstance() {
    abstract val elements: List<Element>

    var x2 = 0
    var y2 = 0

    var x = x
        get() {
            // Don't rearrange panels when not interacting with ClickGUI.
            if (!drag && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1)) return field

            val settingsWidth =
                if (open) elements.filterIsInstance<ModuleElement>().maxOfOrNull { if (it.showSettings) it.settingsWidth else 0 } ?: 0
                else 0

            return field.coerceIn(0, (mc.displayWidth / 2f / scaleValue.get() - width - settingsWidth).roundToInt().coerceAtLeast(0)).also {
                if (it != field) {
                    Mouse.setCursorPosition(Mouse.getX() + ((it - field) / scaleValue.get()).roundToInt(), Mouse.getY())
                    field = it
                }
            }
        }

    var y = y
        get() {
            if (!drag && !Mouse.isButtonDown(0) && !Mouse.isButtonDown(1)) return field

            var yPos = height + 4
            var panelHeight = height + fade

            if (open)
                for (element in elements) {
                    if (element.isVisible) {
                        if (element is ModuleElement && element.showSettings && element.settingsHeight != 0) {
                            val relativeSettingsHeight = yPos + element.settingsHeight
                            if (relativeSettingsHeight > panelHeight) panelHeight = relativeSettingsHeight
                        }
                        yPos += element.height + 1
                    }
                }

            return field.coerceIn(0, (mc.displayHeight / 2f / scaleValue.get() - panelHeight).roundToInt().coerceAtLeast(0)).also {
                if (it != field) {
                    Mouse.setCursorPosition(Mouse.getX(), Mouse.getY() + ((field - it) / scaleValue.get()).roundToInt())
                    field = it
                }
            }
        }

    var drag = false
    val scrollbar
        get() = elements.size > maxElementsValue.get()

    var isVisible = true
    var fade = 0
        set(value) {
            field = value.coerceIn(0, elementsHeight.coerceAtLeast(0))
        }
    private var elementsHeight = 0

    private var scroll = 0
        set(value) {
            // How many elements should be hidden
            val hiddenCount = elements.size - maxElementsValue.get()
            // Don't overscroll
            field = if (hiddenCount > 0) min(hiddenCount, value.coerceAtLeast(0)) else 0
        }

    fun drawScreenAndClick(mouseX: Int, mouseY: Int, mouseButton: Int? = null): Boolean {
        if (!isVisible) return false

        updateElementsHeight()

        // Drag
        if (drag) {
            x = x2 + mouseX
            y = y2 + mouseY
        }

        clickGui.style.drawPanel(mouseX, mouseY, this)

        var yPos = y + height - 2

        val visibleRange = getVisibleRange()

        elements.forEachIndexed { index, element ->
            if (index in visibleRange) {
                element.isVisible = true
                element.setLocation(x, yPos)
                element.width = width

                // If mouse wasn't hovering above any ButtonElement, drawScreenAndClick got called with mouseButton != null.
                // Mouse was detected to be hovering above a value while rendering it.
                // True was returned to stop any further values from getting clicked.
                if (yPos <= y + fade && element.drawScreenAndClick(mouseX, mouseY, mouseButton))
                    return true

                yPos += element.height + 1
                element.isVisible = true
            } else element.isVisible = false
        }

        return false
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (!isVisible) return false

        if (mouseButton == 1 && isHovered(mouseX, mouseY)) {
            open = !open
            clickGui.style.showSettingsSound()
            return true
        }

        if (elements.any { it.y <= y + fade && it.mouseClicked(mouseX, mouseY, mouseButton) })
            return true

        return drawScreenAndClick(mouseX, mouseY, mouseButton)
    }

    fun mouseReleased(mouseX: Int, mouseY: Int, state: Int): Boolean {
        if (!isVisible) return false

        drag = false

        if (!open) return false

        return elements.any { it.y <= y + fade && it.mouseReleased(mouseX, mouseY, state) }
    }

    fun handleScroll(mouseX: Int, mouseY: Int, wheel: Int): Boolean {
        if (mouseX in x..x + width && mouseY in y..y + height + elementsHeight) {
            if (wheel < 0) scroll++
            else scroll--

            return true
        }
        return false
    }

    fun updateFade(delta: Int) {
        fade += ((if (open) 0.4f else -0.4f) * delta * fadeSpeedValue.get()).roundToInt()
    }

    private fun updateElementsHeight() {
        var height = 0
        var count = 0

        for (element in elements) {
            if (count >= maxElementsValue.get()) break
            height += element.height + 1
            ++count
        }

        elementsHeight = height
    }

    fun getVisibleRange(): IntRange {
        val dropLastCount = elements.size - maxElementsValue.get() - scroll

        return max(scroll + min(dropLastCount, 0), 0)..elements.lastIndex - max(dropLastCount, 0)
    }

    fun isHovered(mouseX: Int, mouseY: Int) = mouseX in x..x + width && mouseY in y..y + height
}