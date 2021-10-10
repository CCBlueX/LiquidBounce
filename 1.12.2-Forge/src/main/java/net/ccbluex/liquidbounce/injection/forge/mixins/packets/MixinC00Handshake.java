/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.packets;

import net.ccbluex.liquidbounce.features.special.AntiModDisable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@SideOnly(Side.CLIENT)
@Mixin(C00Handshake.class)
public class MixinC00Handshake
{
	@Shadow
	public int port;
	@Shadow
	public String ip;
	@Shadow
	private int protocolVersion;
	@Shadow
	private EnumConnectionState requestedState;

	/**
	 * @author CCBlueX
	 * @reason AntiModDisable
	 * @see AntiModDisable
	 */
	@Overwrite
	public void writePacketData(final PacketBuffer buf)
	{
		buf.writeVarInt(protocolVersion);
		buf.writeString(ip + (AntiModDisable.Companion.getEnabled() && AntiModDisable.Companion.getBlockFMLPackets() && !Minecraft.getMinecraft().isIntegratedServerRunning() ? "" : "\0FML\0"));
		buf.writeShort(port);
		buf.writeVarInt(requestedState.getId());
	}
}
