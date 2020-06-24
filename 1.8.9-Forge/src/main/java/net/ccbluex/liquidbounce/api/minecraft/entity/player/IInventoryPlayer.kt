/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.entity.player

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack

interface IInventoryPlayer {
    var currentItem: Int

    fun getStackInSlot(slot: Int): IItemStack?
    fun armorItemInSlot(slot: Int): IItemStack?
    fun getCurrentItemInHand(): IItemStack?
}