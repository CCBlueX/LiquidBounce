/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.item.IItemArmor
import net.ccbluex.liquidbounce.api.minecraft.minecraft.IArmorMaterial
import net.minecraft.item.ItemArmor

class ItemArmorImpl(wrapped: ItemArmor) : ItemImpl<ItemArmor>(wrapped), IItemArmor {
    override val armorMaterial: IArmorMaterial
        get() = wrapped.armorMaterial.wrap()
    override val armorType: Int
        get() = wrapped.armorType
    override val unlocalizedName: String
        get() = wrapped.unlocalizedName
}