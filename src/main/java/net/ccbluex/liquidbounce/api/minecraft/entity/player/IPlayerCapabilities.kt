/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.entity.player

interface IPlayerCapabilities {
    val allowFlying: Boolean
    var isFlying: Boolean
    val isCreativeMode: Boolean
}