/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IPlayerControllerMP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.client.multiplayer.PlayerControllerMP

class PlayerControllerMPImpl(val wrapped: PlayerControllerMP) : IPlayerControllerMP {
    override val isNotCreative: Boolean
        get() = wrapped.isNotCreative
    override val blockReachDistance: Float
        get() = wrapped.blockReachDistance
    override val currentGameType: IWorldSettings.WGameType
        get() = wrapped.currentGameType.wrap()
    override val isInCreativeMode: Boolean
        get() = wrapped.isInCreativeMode
    override var curBlockDamageMP: Float
        get() = wrapped.curBlockDamageMP
        set(value) {
            wrapped.curBlockDamageMP = value
        }
    override var blockHitDelay: Int
        get() = wrapped.blockHitDelay
        set(value) {
            wrapped.blockHitDelay = value
        }

    override fun windowClick(windowId: Int, slot: Int, mouseButton: Int, mode: Int, player: IEntityPlayerSP) {
        wrapped.windowClick(windowId, slot, mouseButton, mode, player.unwrap())
    }

    override fun updateController() = wrapped.updateController()

    override fun sendUseItem(playerSP: IEntityPlayer, theWorld: IWorld, itemStack: IItemStack) = wrapped.sendUseItem(playerSP.unwrap(), theWorld.unwrap(), itemStack.unwrap())

    override fun onPlayerRightClick(playerSP: IEntityPlayerSP, theWorld: IWorldClient, itemStack: IItemStack?, position: WBlockPos, sideOpposite: IEnumFacing, hitVec: WVec3) = wrapped.onPlayerRightClick(playerSP.unwrap(), theWorld.unwrap(), itemStack?.unwrap(), position.unwrap(), sideOpposite.unwrap(), hitVec.unwrap())
    override fun onStoppedUsingItem(thePlayer: IEntityPlayerSP) = wrapped.onStoppedUsingItem(thePlayer.unwrap())

    override fun clickBlock(blockPos: WBlockPos, enumFacing: IEnumFacing) = wrapped.clickBlock(blockPos.unwrap(), enumFacing.unwrap())

    override fun onPlayerDestroyBlock(blockPos: WBlockPos, enumFacing: IEnumFacing): Boolean = wrapped.onPlayerDestroyBlock(blockPos.unwrap(), enumFacing.unwrap())


    override fun equals(other: Any?): Boolean {
        return other is PlayerControllerMPImpl && other.wrapped == this.wrapped
    }
}

inline fun IPlayerControllerMP.unwrap(): PlayerControllerMP = (this as PlayerControllerMPImpl).wrapped
inline fun PlayerControllerMP.wrap(): IPlayerControllerMP = PlayerControllerMPImpl(this)