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
import net.ccbluex.liquidbounce.injection.backend.utils.toClickType
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.GameType
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.event.ForgeEventFactory

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
        wrapped.windowClick(windowId, slot, mouseButton, mode.toClickType(), player.unwrap())
    }

    override fun updateController() = wrapped.updateController()


    // This method is not present in 1.12.2 like it was in 1.8.9
    override fun sendUseItem(wPlayer: IEntityPlayer, wWorld: IWorld, wItemStack: IItemStack): Boolean {
        val player = wPlayer.unwrap()
        val world = wWorld.unwrap()
        val itemStack = wItemStack.unwrap()

        if (wrapped.currentGameType == GameType.SPECTATOR) {
            return false
        } else {
            wrapped.syncCurrentPlayItem()

            Minecraft.getMinecraft().connection!!.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))

            if (player.cooldownTracker.hasCooldown(itemStack.item)) {
                return false
            } else {
                val cancelResult = ForgeHooks.onItemRightClick(player, EnumHand.MAIN_HAND)

                if (cancelResult != null)
                    return cancelResult == EnumActionResult.SUCCESS

                val i = itemStack.count

                val result = itemStack.useItemRightClick(world, player, EnumHand.MAIN_HAND)

                val resultStack = result.result

                if (resultStack != itemStack || resultStack.count != i) {
                    player.setHeldItem(EnumHand.MAIN_HAND, resultStack)

                    if (resultStack.isEmpty) {
                        ForgeEventFactory.onPlayerDestroyItem(player, itemStack, EnumHand.MAIN_HAND)
                    }
                }

                return result.type == EnumActionResult.SUCCESS
            }
        }
    }

    override fun onPlayerRightClick(playerSP: IEntityPlayerSP, wWorld: IWorldClient, wItemStack: IItemStack?, wPosition: WBlockPos, wSideOpposite: IEnumFacing, wHitVec: WVec3): Boolean {
        return wrapped.processRightClickBlock(playerSP.unwrap(), wWorld.unwrap(), wPosition.unwrap(), wSideOpposite.unwrap(), wHitVec.unwrap(), EnumHand.MAIN_HAND) == EnumActionResult.SUCCESS
    }

    override fun onStoppedUsingItem(thePlayer: IEntityPlayerSP) = wrapped.onStoppedUsingItem(thePlayer.unwrap())

    override fun clickBlock(blockPos: WBlockPos, enumFacing: IEnumFacing) = wrapped.clickBlock(blockPos.unwrap(), enumFacing.unwrap())

    override fun onPlayerDestroyBlock(blockPos: WBlockPos, enumFacing: IEnumFacing): Boolean = wrapped.onPlayerDestroyBlock(blockPos.unwrap())


    override fun equals(other: Any?): Boolean {
        return other is PlayerControllerMPImpl && other.wrapped == this.wrapped
    }
}

inline fun IPlayerControllerMP.unwrap(): PlayerControllerMP = (this as PlayerControllerMPImpl).wrapped
inline fun PlayerControllerMP.wrap(): IPlayerControllerMP = PlayerControllerMPImpl(this)