/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.injection.backend.PacketImplKt;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;

@Mixin(NetworkManager.class)
public class MixinNetworkManager
{

	@Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
	private void read(final ChannelHandlerContext context, final Packet<?> packet, final CallbackInfo callback)
	{
		final PacketEvent event = new PacketEvent(PacketImplKt.wrap(packet));
		LiquidBounce.eventManager.callEvent(event);

		if (event.isCancelled())
			callback.cancel();
	}

	@Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void send(final Packet<?> packet, final CallbackInfo callback)
	{
		final PacketEvent event = new PacketEvent(PacketImplKt.wrap(packet));
		LiquidBounce.eventManager.callEvent(event);

		if (event.isCancelled())
			callback.cancel();
	}
}
