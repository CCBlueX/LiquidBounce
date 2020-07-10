/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.block

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld

@Suppress("INAPPLICABLE_JVM_NAME")
interface IBlock {
    val registryName: String
    var slipperiness: Float

    val blockBoundsMinY: Double
    val blockBoundsMaxY: Double
    val defaultState: IIBlockState?
    val localizedName: String
    val material: IMaterial?

    @get:JvmName("isFullCube")
    val fullCube: Boolean

    fun getSelectedBoundingBox(world: IWorld, blockPos: WBlockPos): IAxisAlignedBB
    fun getCollisionBoundingBox(world: IWorld, pos: WBlockPos, state: IIBlockState?): IAxisAlignedBB?
    fun canCollideCheck(state: IIBlockState?, hitIfLiquid: Boolean): Boolean
    fun setBlockBoundsBasedOnState(world: IWorld, blockPos: WBlockPos)
    fun getPlayerRelativeBlockHardness(thePlayer: IEntityPlayerSP, theWorld: IWorld, blockPos: WBlockPos): Float
    fun getIdFromBlock(block: IBlock): Int
    fun isTranslucent(blockState: IIBlockState): Boolean
}