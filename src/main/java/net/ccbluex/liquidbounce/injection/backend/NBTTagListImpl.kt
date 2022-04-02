/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTBase
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagList
import net.minecraft.nbt.NBTTagList

class NBTTagListImpl(wrapped: NBTTagList) : NBTBaseImpl<NBTTagList>(wrapped), INBTTagList {
    override fun hasNoTags(): Boolean = wrapped.hasNoTags()
    override fun tagCount(): Int = wrapped.tagCount()
    override fun getCompoundTagAt(index: Int): INBTTagCompound = wrapped.getCompoundTagAt(index).wrap()
    override fun appendTag(createNBTTagString: INBTBase) = wrapped.appendTag(createNBTTagString.unwrap())
}

inline fun INBTTagList.unwrap(): NBTTagList = (this as NBTTagListImpl).wrapped
inline fun NBTTagList.wrap(): INBTTagList = NBTTagListImpl(this)