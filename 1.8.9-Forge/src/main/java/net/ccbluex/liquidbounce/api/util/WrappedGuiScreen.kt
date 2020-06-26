/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.util

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.utils.MinecraftInstance

abstract class WrappedGuiScreen : MinecraftInstance() {
    lateinit var representedScreen: IGuiScreen

    open fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {

    }

    open fun initGui() {}
    open fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {}
}