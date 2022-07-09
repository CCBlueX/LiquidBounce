/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.nbt

import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTBase
import net.minecraft.nbt.NBTBase

open class NBTBaseImpl<out T : NBTBase>(val wrapped: T) : INBTBase
{
    override fun equals(other: Any?): Boolean = other is NBTBaseImpl<*> && other.wrapped == wrapped
}

fun INBTBase.unwrap(): NBTBase = (this as NBTBaseImpl<*>).wrapped
fun NBTBase.wrap(): INBTBase = NBTBaseImpl(this)
