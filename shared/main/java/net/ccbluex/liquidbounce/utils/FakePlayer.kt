package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityOtherPlayerMP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient

class FakePlayer(val theWorld: IWorldClient, thePlayer: IEntityPlayer, val entityId: Int) : MinecraftInstance()
{
    private var fakePlayer: IEntityOtherPlayerMP

    init
    {
        require(entityId <= 0) { "entityId must be negative" }

        fakePlayer = classProvider.createEntityOtherPlayerMP(theWorld, thePlayer.gameProfile)
        fakePlayer.rotationYawHead = thePlayer.rotationYawHead
        fakePlayer.renderYawOffset = thePlayer.renderYawOffset
        fakePlayer.copyLocationAndAnglesFrom(thePlayer)
        fakePlayer.setCanBeCollidedWith(false)

        theWorld.addEntityToWorld(entityId, fakePlayer)
    }

    fun updatePositionAndRotation(thePlayer: IEntityPlayer)
    {
        fakePlayer.rotationYawHead = thePlayer.rotationYawHead
        fakePlayer.renderYawOffset = thePlayer.renderYawOffset
        fakePlayer.copyLocationAndAnglesFrom(thePlayer)
    }

    fun destroy()
    {
        theWorld.removeEntityFromWorld(entityId)
    }
}
