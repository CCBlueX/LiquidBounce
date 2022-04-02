/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.multiplayer

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.api.minecraft.world.IWorldSettings

interface IPlayerControllerMP {
    val isNotCreative: Boolean
    val blockReachDistance: Float
    val currentGameType: IWorldSettings.WGameType
    val isInCreativeMode: Boolean
    var curBlockDamageMP: Float
    var blockHitDelay: Int

    fun windowClick(windowId: Int, slot: Int, mouseButton: Int, mode: Int, player: IEntityPlayerSP)
    fun updateController()
    fun sendUseItem(playerSP: IEntityPlayer, theWorld: IWorld, itemStack: IItemStack): Boolean
    fun onPlayerRightClick(playerSP: IEntityPlayerSP, theWorld: IWorldClient, itemStack: IItemStack?, position: WBlockPos, sideOpposite: IEnumFacing, hitVec: WVec3): Boolean
    fun onStoppedUsingItem(thePlayer: IEntityPlayerSP)
    fun clickBlock(blockPos: WBlockPos, enumFacing: IEnumFacing): Boolean
    fun onPlayerDestroyBlock(blockPos: WBlockPos, enumFacing: IEnumFacing): Boolean
}