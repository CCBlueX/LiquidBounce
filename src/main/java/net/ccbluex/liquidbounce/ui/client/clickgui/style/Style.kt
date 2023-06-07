/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style

import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.value.Value
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import java.math.BigDecimal

abstract class Style : MinecraftInstance() {
    protected var sliderValueHeld: Value<*>? = null
        get() {
            if (!Mouse.isButtonDown(0)) field = null
            return field
        }

    abstract fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel)
    abstract fun drawHoverText(mouseX: Int, mouseY: Int, text: String)
    abstract fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement)
    abstract fun drawModuleElementAndClick(mouseX: Int, mouseY: Int, moduleElement: ModuleElement, mouseButton: Int?): Boolean

    fun clickSound() = mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("gui.button.press"), 1f))

    fun showSettingsSound() = mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("random.bow"), 1f))

    fun round(v: Float): Float {
        var bigDecimal = BigDecimal(v.toString())
        bigDecimal = bigDecimal.setScale(2, 4)
        return bigDecimal.toFloat()
    }
}