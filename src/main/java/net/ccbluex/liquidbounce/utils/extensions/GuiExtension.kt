package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.injection.implementations.IMixinGuiContainer
import net.minecraft.client.gui.inventory.GuiContainer

fun GuiContainer.highlight(slotNumber: Int, duration: Long, color: Int) = (this as IMixinGuiContainer).highlight(slotNumber, duration, color)

