/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.minecraft.IArmorMaterial
import net.ccbluex.liquidbounce.injection.backend.utils.toEntityEquipmentSlot
import net.minecraft.item.ItemArmor

class ArmorMaterialImpl(val wrapped: ItemArmor.ArmorMaterial) : IArmorMaterial {
    override val enchantability: Int
        get() = wrapped.enchantability

    override fun getDamageReductionAmount(type: Int): Int = wrapped.getDamageReductionAmount(type.toEntityEquipmentSlot())


    override fun getDurability(type: Int): Int = wrapped.getDurability(type.toEntityEquipmentSlot())

    override fun equals(other: Any?): Boolean {
        return other is ArmorMaterialImpl && other.wrapped == this.wrapped
    }
}

inline fun IArmorMaterial.unwrap(): ItemArmor.ArmorMaterial = (this as ArmorMaterialImpl).wrapped
inline fun ItemArmor.ArmorMaterial.wrap(): IArmorMaterial = ArmorMaterialImpl(this)