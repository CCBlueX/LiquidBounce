/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.settings

interface IKeyBinding
{
    val keyCode: Int
    var pressed: Boolean
    val isKeyDown: Boolean

    fun onTick(keyCode: Int)
    fun unpressKey()
}
