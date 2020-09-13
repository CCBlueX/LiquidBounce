/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

class PotionEffectImpl(val wrapped: PotionEffect) : IPotionEffect {
    override fun getDurationString(): String = Potion.getDurationString(wrapped)

    override val amplifier: Int
        get() = wrapped.amplifier
    override val duration: Int
        get() = wrapped.duration
    override val potionID: Int
        get() = wrapped.potionID

    override fun equals(other: Any?): Boolean {
        return other is PotionEffectImpl && other.wrapped == this.wrapped
    }
}

inline fun IPotionEffect.unwrap(): PotionEffect = (this as PotionEffectImpl).wrapped
inline fun PotionEffect.wrap(): IPotionEffect = PotionEffectImpl(this)