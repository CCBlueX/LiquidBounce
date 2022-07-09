/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.potion

interface IPotionEffect
{
    val effectName: String
    val potionID: Int
    val amplifier: Int
    val duration: Int

    fun getDurationString(): String
}
