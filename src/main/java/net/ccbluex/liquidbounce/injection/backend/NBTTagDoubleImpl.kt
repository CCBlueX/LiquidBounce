/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagDouble
import net.minecraft.nbt.NBTTagDouble

class NBTTagDoubleImpl<T : NBTTagDouble>(wrapped: T) : NBTBaseImpl<T>(wrapped), INBTTagDouble

inline fun INBTTagDouble.unwrap(): NBTTagDouble = (this as NBTTagDoubleImpl<*>).wrapped
inline fun NBTTagDouble.wrap(): INBTTagDouble = NBTTagDoubleImpl(this)