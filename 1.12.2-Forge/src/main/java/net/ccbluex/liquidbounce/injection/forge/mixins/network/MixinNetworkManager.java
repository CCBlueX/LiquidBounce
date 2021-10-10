/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.injection.backend.PacketImplKt;
import net.ccbluex.liquidbounce.injection.implementations.IMixinNetworkManager;
import net.ccbluex.liquidbounce.utils.PacketCounter;
import net.ccbluex.liquidbounce.utils.PacketCounter.PacketType;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager implements IMixinNetworkManager
{
	@Shadow
	@Final
	private static Logger LOGGER;

	@Shadow
	@Final
	private ReentrantReadWriteLock readWriteLock;

	@Shadow
	@Final
	private Queue<InboundHandlerTuplePacketListener> outboundPacketsQueue;

	@Shadow
	public abstract boolean isChannelOpen();

	@Shadow
	protected abstract void flushOutboundQueue();

	@Shadow
	protected abstract void dispatchPacket(Packet<?> inPacket, GenericFutureListener<? extends Future<? super Void>>[] futureListeners);

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


	@SuppressWarnings("unchecked")
	@Override
	public void sendPacketWithoutEvent(final Packet<?> packetIn)
	{
		if (isChannelOpen())
		{
			flushOutboundQueue();
			dispatchPacket(packetIn, null);
		}
		else
		{
			readWriteLock.writeLock().lock();

			try
			{
				outboundPacketsQueue.add(new InboundHandlerTuplePacketListener(packetIn, (GenericFutureListener[]) null));
			}
			finally
			{
				readWriteLock.writeLock().unlock();
			}
		}

		PacketCounter.registerPacket(PacketType.OUTBOUND);
	}
}
