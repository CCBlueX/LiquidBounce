/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.minecraft.potion.PotionEffect

class PotionEffectImpl(val wrapped: PotionEffect) : IPotionEffect {
    override val amplifier: Int
        get() = wrapped.amplifier
    override val duration: Int
        get() = wrapped.duration
    override val potionID: Int
        get() = wrapped.potionID
}