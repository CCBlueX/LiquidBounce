/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiChest
import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IIInventory
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.inventory.ISlot
import net.minecraft.client.gui.inventory.GuiChest

class GuiChestImpl<T : GuiChest>(wrapped: T) : GuiScreenImpl<T>(wrapped), IGuiChest {
    override val inventoryRows: Int
        get() = wrapped.inventoryRows
    override val lowerChestInventory: IIInventory?
        get() = wrapped.lowerChestInventory?.wrap()
    override val inventorySlots: IContainer?
        get() = wrapped.inventorySlots?.wrap()

    override fun handleMouseClick(slot: ISlot, slotNumber: Int, clickedButton: Int, clickType: Int) = wrapped.handleMouseClick(slot.unwrap(), slotNumber, clickedButton, clickType)

}

inline fun IGuiChest.unwrap(): GuiChest = (this as GuiChestImpl<*>).wrapped
inline fun GuiChest.wrap(): IGuiChest = GuiChestImpl(this)