/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTBase
import net.ccbluex.liquidbounce.api.minecraft.nbt.INBTTagCompound
import net.minecraft.nbt.NBTTagCompound

class NBTTagCompoundImpl(wrapped: NBTTagCompound) : NBTBaseImpl<NBTTagCompound>(wrapped), INBTTagCompound
{
	// <editor-fold desc="Contains">
	override fun hasKey(name: String): Boolean = wrapped.hasKey(name)
	// </editor-fold>

	// <editor-fold desc="Getter">
	override fun getShort(name: String): Short = wrapped.getShort(name)
	// </editor-fold>

	// <editor-fold desc="Setter">
	override fun setString(key: String, value: String) = wrapped.setString(key, value)

	override fun setTag(key: String, tag: INBTBase) = wrapped.setTag(key, tag.unwrap())

	override fun setInteger(key: String, value: Int) = wrapped.setInteger(key, value)
	// </editor-fold>
}

fun INBTTagCompound.unwrap(): NBTTagCompound = (this as NBTTagCompoundImpl).wrapped
fun NBTTagCompound.wrap(): INBTTagCompound = NBTTagCompoundImpl(this)
