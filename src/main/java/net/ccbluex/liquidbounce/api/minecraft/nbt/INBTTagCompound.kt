/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.nbt

interface INBTTagCompound : INBTBase {
    fun hasKey(name: String): Boolean
    fun getShort(name: String): Short
    fun setString(key: String, value: String)
    fun setTag(key: String, tag: INBTBase)
    fun setInteger(key: String, value: Int)
}