/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.LiquidBounce.wrapper

open class MinecraftInstance
{
	companion object
	{
		@JvmField
		var mc = wrapper.minecraft

		@JvmField
		val classProvider = wrapper.classProvider

		@JvmField
		val functions = wrapper.functions
	}
}
