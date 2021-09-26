/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.utils.MinecraftInstance

typealias Collidable = (IIBlockState) -> Boolean

object BlockUtils : MinecraftInstance()
{
	/**
	 * Get block name by [id]
	 */
	@JvmStatic
	fun getBlockName(id: Int): String = functions.getBlockById(id)?.localizedName ?: ""
}
