/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.block.Block

class BlockImpl(val wrapped: Block) : IBlock {
    override val registryName: String
        get() = wrapped.registryName
    override var slipperiness: Float
        get() = wrapped.slipperiness
        set(value) {
            wrapped.slipperiness = value
        }
    override val defaultState: IIBlockState?
        get() = wrapped.defaultState?.wrap()
    override val localizedName: String
        get() = wrapped.localizedName

    override fun getSelectedBoundingBox(world: IWorld, blockState: IIBlockState, blockPos: WBlockPos): IAxisAlignedBB = AxisAlignedBBImpl(wrapped.getSelectedBoundingBox(world.unwrap(), blockPos.unwrap()))

    override fun getCollisionBoundingBox(world: IWorld, pos: WBlockPos, state: IIBlockState): IAxisAlignedBB? = wrapped.getCollisionBoundingBox(world?.unwrap(), pos.unwrap(), state?.unwrap())?.wrap()

    override fun canCollideCheck(state: IIBlockState?, hitIfLiquid: Boolean): Boolean = wrapped.canCollideCheck(state?.unwrap(), hitIfLiquid)

    override fun setBlockBoundsBasedOnState(world: IWorld, blockPos: WBlockPos) = wrapped.setBlockBoundsBasedOnState(world.unwrap(), blockPos.unwrap())

    override fun getPlayerRelativeBlockHardness(thePlayer: IEntityPlayerSP, theWorld: IWorld, blockPos: WBlockPos): Float = wrapped.getPlayerRelativeBlockHardness(thePlayer.unwrap(), theWorld.unwrap(), blockPos.unwrap())

    override fun getIdFromBlock(block: IBlock): Int = Block.getIdFromBlock(block.unwrap())
    override fun isTranslucent(blockState: IIBlockState): Boolean = wrapped.isTranslucent

    override fun getMapColor(blockState: IIBlockState, theWorld: IWorldClient, bp: WBlockPos): Int = wrapped.getMapColor(blockState.unwrap()).colorValue

    override fun getMaterial(state: IIBlockState): IMaterial? = wrapped.material?.wrap()

    override fun isFullCube(state: IIBlockState): Boolean = wrapped.isFullCube

    override fun equals(other: Any?): Boolean {
        return other is BlockImpl && other.wrapped == this.wrapped
    }
}

inline fun IBlock.unwrap(): Block = (this as BlockImpl).wrapped
inline fun Block.wrap(): IBlock = BlockImpl(this)