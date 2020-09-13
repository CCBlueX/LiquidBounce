/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiChest
import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IIInventory
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.minecraft.client.gui.inventory.GuiChest

class GuiChestImpl<T : GuiChest>(wrapped: T) : GuiContainerImpl<T>(wrapped), IGuiChest {
    override val inventoryRows: Int
        get() = wrapped.inventoryRows
    override val lowerChestInventory: IIInventory?
        get() = wrapped.lowerChestInventory?.wrap()
    override val inventorySlots: IContainer?
        get() = wrapped.inventorySlots?.wrap()
}

inline fun IGuiChest.unwrap(): GuiChest = (this as GuiChestImpl<*>).wrapped
inline fun GuiChest.wrap(): IGuiChest = GuiChestImpl(this)