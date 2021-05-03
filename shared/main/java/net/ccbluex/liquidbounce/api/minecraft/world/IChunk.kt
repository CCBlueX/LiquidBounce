/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.world

import com.google.common.base.Predicate
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos

interface IChunk
{
	val x: Int
	val z: Int
	val isLoaded: Boolean

	fun getEntitiesWithinAABBForEntity(entity: IEntity, arrowBox: IAxisAlignedBB, collidedEntities: MutableList<IEntity>, predicate: Predicate<IEntity>?)
	fun getHeightValue(x: Int, z: Int): Int
	fun getBlockState(blockPos: WBlockPos): IIBlockState
}
