/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.designer

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import kotlin.math.min

class GuiHudDesigner : GuiScreen()
{

    private var editorPanel = EditorPanel(this, 2, 2)

    var selectedElement: Element? = null
    private var buttonAction = false

    override fun initGui()
    {
        Keyboard.enableRepeatEvents(true)
        editorPanel = EditorPanel(this, width shr 1, height shr 1)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        LiquidBounce.hud.render(true)
        LiquidBounce.hud.handleMouseMove(mouseX, mouseY)

        if (!LiquidBounce.hud.elements.contains(selectedElement)) selectedElement = null

        val wheel = Mouse.getDWheel()

        editorPanel.drawPanel(mouseX, mouseY, wheel)

        if (wheel != 0) LiquidBounce.hud.elements.firstOrNull { element -> element.isInBorder(mouseX / element.scale - element.renderX, mouseY / element.scale - element.renderY) }?.let {
            it.scale = it.scale + if (wheel > 0) 0.05f else -0.05f
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    {
        super.mouseClicked(mouseX, mouseY, mouseButton)

        if (buttonAction)
        {
            buttonAction = false
            return
        }

        LiquidBounce.hud.handleMouseClick(mouseX, mouseY, mouseButton)

        if (!(mouseX >= editorPanel.x && mouseX <= editorPanel.x + editorPanel.width && mouseY >= editorPanel.y && mouseY <= editorPanel.y + min(editorPanel.realHeight, 200)))
        {
            selectedElement = null
            editorPanel.create = false
        }

        if (mouseButton == 0) LiquidBounce.hud.elements.firstOrNull { it.isInBorder(mouseX / it.scale - it.renderX, mouseY / it.scale - it.renderY) }?.let { selectedElement = it }
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int)
    {
        super.mouseReleased(mouseX, mouseY, state)

        LiquidBounce.hud.handleMouseReleased()
    }

    override fun onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false)
        FileManager.saveConfig(LiquidBounce.fileManager.hudConfig)

        super.onGuiClosed()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        when (keyCode)
        {
            Keyboard.KEY_DELETE -> selectedElement?.let(LiquidBounce.hud::removeElement)

            Keyboard.KEY_ESCAPE ->
            {
                selectedElement = null
                editorPanel.create = false
            }

            else -> LiquidBounce.hud.handleKey(typedChar, keyCode)
        }

        super.keyTyped(typedChar, keyCode)
    }
}
