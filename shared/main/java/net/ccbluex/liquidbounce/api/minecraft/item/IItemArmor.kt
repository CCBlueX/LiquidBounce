/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.item

interface IItemArmor : IItem
{
    val damageReduceAmount: Int
    val armorMaterial: IArmorMaterial
    val armorType: Int

    fun getColor(item: IItemStack): Int
}
