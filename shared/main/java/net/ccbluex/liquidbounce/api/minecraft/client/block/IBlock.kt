/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.block

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld

@Suppress("INAPPLICABLE_JVM_NAME")
interface IBlock {
    val registryName: String
    var slipperiness: Float

    val defaultState: IIBlockState?
    val localizedName: String

    fun getSelectedBoundingBox(world: IWorld, blockState: IIBlockState, blockPos: WBlockPos): IAxisAlignedBB
    fun getCollisionBoundingBox(world: IWorld, pos: WBlockPos, state: IIBlockState): IAxisAlignedBB?
    fun canCollideCheck(state: IIBlockState?, hitIfLiquid: Boolean): Boolean
    fun setBlockBoundsBasedOnState(world: IWorld, blockPos: WBlockPos)
    fun getPlayerRelativeBlockHardness(thePlayer: IEntityPlayerSP, theWorld: IWorld, blockPos: WBlockPos): Float
    fun getIdFromBlock(block: IBlock): Int
    fun isTranslucent(blockState: IIBlockState): Boolean
    fun getMapColor(blockState: IIBlockState, theWorld: IWorldClient, bp: WBlockPos): Int
    fun getMaterial(state: IIBlockState): IMaterial?
    fun isFullCube(state: IIBlockState): Boolean
}