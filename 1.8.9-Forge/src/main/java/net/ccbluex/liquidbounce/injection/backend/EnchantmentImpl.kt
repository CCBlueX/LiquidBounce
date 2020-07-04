/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.enchantments.IEnchantment
import net.minecraft.enchantment.Enchantment

class EnchantmentImpl(val wrapped: Enchantment) : IEnchantment {
    override val effectId: Int
        get() = wrapped.effectId

    override fun getTranslatedName(level: Int): String = wrapped.getTranslatedName(level)

}

inline fun IEnchantment.unwrap(): Enchantment = (this as EnchantmentImpl).wrapped
inline fun Enchantment.wrap(): IEnchantment = EnchantmentImpl(this)