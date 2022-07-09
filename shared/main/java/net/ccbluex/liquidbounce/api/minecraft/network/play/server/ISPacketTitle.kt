package net.ccbluex.liquidbounce.api.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent

interface ISPacketTitle : IPacket
{
    val message: IIChatComponent?
}
