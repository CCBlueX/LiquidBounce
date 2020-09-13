/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.entity.player.IInventoryPlayer
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.util.IWrappedArray
import net.ccbluex.liquidbounce.api.util.WrappedListArrayAdapter
import net.minecraft.entity.player.InventoryPlayer

class InventoryPlayerImpl(val wrapped: InventoryPlayer) : IInventoryPlayer {
    override val mainInventory: IWrappedArray<IItemStack?>
        get() = WrappedListArrayAdapter(wrapped.mainInventory, { it?.unwrap() }, { it?.wrap() })
    override val armorInventory: IWrappedArray<IItemStack?>
        get() = WrappedListArrayAdapter(wrapped.armorInventory, { it?.unwrap() }, { it?.wrap() })
    override var currentItem: Int
        get() = wrapped.currentItem
        set(value) {
            wrapped.currentItem = value
        }

    override fun getStackInSlot(slot: Int): IItemStack? = wrapped.getStackInSlot(slot)?.wrap()

    override fun armorItemInSlot(slot: Int): IItemStack? = wrapped.armorItemInSlot(3 - slot)?.wrap()

    override fun getCurrentItemInHand(): IItemStack? = wrapped.getCurrentItem()?.wrap()

    override fun equals(other: Any?): Boolean {
        return other is InventoryPlayerImpl && other.wrapped == this.wrapped
    }
}

inline fun IInventoryPlayer.unwrap(): InventoryPlayer = (this as InventoryPlayerImpl).wrapped
inline fun InventoryPlayer.wrap(): IInventoryPlayer = InventoryPlayerImpl(this)