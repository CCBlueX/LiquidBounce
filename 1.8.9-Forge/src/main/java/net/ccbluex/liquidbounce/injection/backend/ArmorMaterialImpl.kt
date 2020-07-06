package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.minecraft.IArmorMaterial
import net.minecraft.item.ItemArmor

class ArmorMaterialImpl(val wrapped: ItemArmor.ArmorMaterial) : IArmorMaterial {
    override val enchantability: Int
        get() = wrapped.enchantability

    override fun getDamageReductionAmount(type: Int): Int = wrapped.getDamageReductionAmount(type)

    override fun getDurability(type: Int): Int = wrapped.getDurability(type)

}

inline fun IArmorMaterial.unwrap(): ItemArmor.ArmorMaterial = (this as ArmorMaterialImpl).wrapped
inline fun ItemArmor.ArmorMaterial.wrap(): IArmorMaterial = ArmorMaterialImpl(this)