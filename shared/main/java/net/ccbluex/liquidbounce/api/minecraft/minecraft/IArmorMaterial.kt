package net.ccbluex.liquidbounce.api.minecraft.minecraft

interface IArmorMaterial {
    val enchantability: Int

    fun getDamageReductionAmount(type: Int): Int
    fun getDurability(type: Int): Int
}