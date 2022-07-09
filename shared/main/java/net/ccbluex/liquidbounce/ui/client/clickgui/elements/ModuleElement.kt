/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import org.lwjgl.input.Mouse

class ModuleElement(val module: Module) : ButtonElement(module.name)
{
    var showSettings = false
    var settingsWidth = 0f
    private var wasLeftClickPressed = false
    private var wasRightClickPressed = false
    var slowlySettingsYPos = 0
    var slowlyFade = 0

    val isntLeftPressed: Boolean
        get() = !wasLeftClickPressed
    val isntRightPressed: Boolean
        get() = !wasRightClickPressed

    override fun drawScreen(mouseX: Int, mouseY: Int, button: Float)
    {
        LiquidBounce.clickGui.style.drawModuleElement(mouseX, mouseY, this)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    {
        if (isHovering(mouseX, mouseY) && isVisible && mouseButton in 0..1)
        {
            if (mouseButton == 0) module.toggle() else showSettings = !showSettings

            mc.soundHandler.playSound("gui.button.press", 1.0f)
        }
    }

    fun updatePressed()
    {
        wasLeftClickPressed = Mouse.isButtonDown(0)
        wasRightClickPressed = Mouse.isButtonDown(1)
    }
}
