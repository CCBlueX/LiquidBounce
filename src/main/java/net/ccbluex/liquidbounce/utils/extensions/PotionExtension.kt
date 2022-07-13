package net.ccbluex.liquidbounce.utils.extensions

import net.minecraft.entity.EntityLivingBase
import net.minecraft.potion.Potion

/**
 * @return The amplifier of Speed potion effect which applied on thePlayer (1~) (If thePlayer doesn't have Speed potion effect, it returns 0)
 */
val EntityLivingBase.speedEffectAmplifier: Int
    get() = getEffectAmplifier(Potion.moveSpeed)

fun EntityLivingBase.getEffectAmplifier(type: Potion): Int = getActivePotionEffect(type)?.amplifier?.plus(1) ?: 0
