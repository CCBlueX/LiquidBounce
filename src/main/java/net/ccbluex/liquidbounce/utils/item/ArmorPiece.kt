/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack

class ArmorPiece(val itemStack: IItemStack?, val slot: Int)
{
	val armorType: Int
		get() = itemStack?.item?.asItemArmor()?.armorType!!
}
