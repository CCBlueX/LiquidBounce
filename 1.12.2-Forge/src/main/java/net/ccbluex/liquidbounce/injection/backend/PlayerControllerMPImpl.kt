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
import net.minecraft.block.BlockCommandBlock
import net.minecraft.block.BlockStructure
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.world.GameType
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.event.ForgeEventFactory
import net.minecraftforge.fml.common.eventhandler.Event

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
        val player = playerSP.unwrap()
        val world = wWorld.unwrap()
        val itemStack = wItemStack?.unwrap()
        val hand = EnumHand.MAIN_HAND
        val direction = wSideOpposite.unwrap()
        val vec = wHitVec.unwrap()
        val pos = wPosition.unwrap()

        wrapped.syncCurrentPlayItem()

        val itemstack: ItemStack = player.getHeldItem(hand)
        val f = (vec.x - pos.x.toDouble())
        val f1 = (vec.y - pos.y.toDouble())
        val f2 = (vec.z - pos.z.toDouble())
        var flag = false

        return EnumActionResult.SUCCESS == if (!world.worldBorder.contains(pos)) {
            return false
        } else {
            val event = ForgeHooks
                    .onRightClickBlock(player, hand, pos, direction, ForgeHooks.rayTraceEyeHitVec(player, wrapped.blockReachDistance + 1.toDouble()))
            if (event.isCanceled) {
                // Give the server a chance to fire event as well. That way server event is not dependant on client event.
                player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos, direction, hand, f.toFloat(), f1.toFloat(), f2.toFloat()))
                return event.cancellationResult == EnumActionResult.SUCCESS
            }
            var result: EnumActionResult? = EnumActionResult.PASS

            if (wrapped.currentGameType != GameType.SPECTATOR) {
                val ret = itemstack.onItemUseFirst(player, world, pos, hand, direction, f.toFloat(), f1.toFloat(), f2.toFloat())

                if (ret != EnumActionResult.PASS) {
                    // The server needs to process the item use as well. Otherwise onItemUseFirst won't ever be called on the server without causing weird bugs
                    player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos, direction, hand, f.toFloat(), f1.toFloat(), f2.toFloat()))
                    return ret == EnumActionResult.SUCCESS
                }

                val iblockstate: IBlockState = world.getBlockState(pos)
                val bypass = player.heldItemMainhand.doesSneakBypassUse(world, pos, player) && player.heldItemOffhand.doesSneakBypassUse(world, pos, player)
                if (!player.isSneaking || bypass || event.useBlock == Event.Result.ALLOW) {
                    if (event.useBlock != Event.Result.DENY) flag = iblockstate.block.onBlockActivated(world, pos, iblockstate, player, hand, direction, f.toFloat(), f1.toFloat(), f2.toFloat())
                    if (flag) result = EnumActionResult.SUCCESS
                }
                if (!flag && itemstack.item is ItemBlock) {
                    val itemblock = itemstack.item as ItemBlock
                    if (!itemblock.canPlaceBlockOnSide(world, pos, direction, player, itemstack)) {
                        return false
                    }
                }
            }
            player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(pos, direction, hand, f.toFloat(), f1.toFloat(), f2.toFloat()))

            if (!flag && currentGameType != GameType.SPECTATOR || event.useItem == Event.Result.ALLOW) {
                if (itemstack.isEmpty) {
                    return false
                } else if (player.cooldownTracker.hasCooldown(itemstack.item)) {
                    return false
                } else {
                    if (itemstack.item is ItemBlock && !player.canUseCommandBlock()) {
                        val block = (itemstack.item as ItemBlock).block
                        if (block is BlockCommandBlock || block is BlockStructure) {
                            return false
                        }
                    }
                    if (wrapped.currentGameType.isCreative) {
                        val i = itemstack.metadata
                        val j = itemstack.count
                        if (event.useItem != Event.Result.DENY) {
                            val enumactionresult = itemstack.onItemUse(player, world, pos, hand, direction, f.toFloat(), f1.toFloat(), f2.toFloat())
                            itemstack.itemDamage = i
                            itemstack.count = j

                            enumactionresult
                        } else result
                    } else {
                        val copyForUse = itemstack.copy()
                        if (event.useItem != Event.Result.DENY) result = itemstack.onItemUse(player, world, pos, hand, direction, f.toFloat(), f1.toFloat(), f2.toFloat())
                        if (itemstack.isEmpty) ForgeEventFactory.onPlayerDestroyItem(player, copyForUse, hand)
                        result
                    }
                }
            } else {
                EnumActionResult.SUCCESS
            }
        }
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