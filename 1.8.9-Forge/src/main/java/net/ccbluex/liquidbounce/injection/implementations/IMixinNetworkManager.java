package net.ccbluex.liquidbounce.injection.implementations;

import net.minecraft.network.Packet;

public interface IMixinNetworkManager
{
	void sendPacketWithoutEvent(final Packet<?> packet);
}
