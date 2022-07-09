/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.nbt

import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagString
import net.minecraft.nbt.NBTTagString

class NBTTagStringImpl<out T : NBTTagString>(wrapped: T) : NBTBaseImpl<T>(wrapped), INBTTagString

fun INBTTagString.unwrap(): NBTTagString = (this as NBTTagStringImpl<*>).wrapped
fun NBTTagString.wrap(): INBTTagString = NBTTagStringImpl(this)
