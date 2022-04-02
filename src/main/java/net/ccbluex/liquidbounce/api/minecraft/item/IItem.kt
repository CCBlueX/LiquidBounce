/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.item

interface IItem {
    val unlocalizedName: String

    fun asItemArmor(): IItemArmor
    fun asItemPotion(): IItemPotion
    fun asItemBlock(): IItemBlock
    fun asItemSword(): IItemSword
    fun asItemBucket(): IItemBucket
}