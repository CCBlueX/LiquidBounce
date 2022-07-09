package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityOtherPlayerMP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient

class FakePlayer(val theWorld: WorldClient, thePlayer: EntityPlayer, val entityId: Int) : MinecraftInstance()
{
    private var fakePlayer: EntityOtherPlayerMP

    init
    {
        require(entityId <= 0) { "entityId must be negative" }

        fakePlayer = EntityOtherPlayerMP(theWorld, thePlayer.gameProfile)
        fakePlayer.rotationYawHead = thePlayer.rotationYawHead
        fakePlayer.renderYawOffset = thePlayer.renderYawOffset
        fakePlayer.copyLocationAndAnglesFrom(thePlayer)
        fakePlayer.setCanBeCollidedWith(false)

        theWorld.addEntityToWorld(entityId, fakePlayer)
    }

    fun updatePositionAndRotation(thePlayer: EntityPlayer)
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
