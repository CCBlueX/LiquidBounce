/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.item

import net.ccbluex.liquidbounce.api.minecraft.item.IArmorMaterial
import net.minecraft.item.ItemArmor

class ArmorMaterialImpl(val wrapped: ItemArmor.ArmorMaterial) : IArmorMaterial
{
    override val enchantability: Int
        get() = wrapped.enchantability

    override fun getDamageReductionAmount(type: Int): Int = wrapped.getDamageReductionAmount(type)

    override fun getDurability(type: Int): Int = wrapped.getDurability(type)

    override fun equals(other: Any?): Boolean = other is ArmorMaterialImpl && other.wrapped == wrapped
}

fun IArmorMaterial.unwrap(): ItemArmor.ArmorMaterial = (this as ArmorMaterialImpl).wrapped
fun ItemArmor.ArmorMaterial.wrap(): IArmorMaterial = ArmorMaterialImpl(this)
