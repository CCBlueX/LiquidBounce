/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.nbt

interface INBTTagList : INBTBase {
    fun hasNoTags(): Boolean
    fun tagCount(): Int
    fun getCompoundTagAt(index: Int): INBTTagCompound
    fun appendTag(createNBTTagString: INBTBase)
}