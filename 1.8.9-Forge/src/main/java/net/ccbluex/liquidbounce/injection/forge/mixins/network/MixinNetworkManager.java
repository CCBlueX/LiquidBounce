/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import java.lang.reflect.InvocationTargetException;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.injection.backend.PacketImplKt;
import net.ccbluex.liquidbounce.injection.implementations.IMixinNetworkManager;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.PPSCounter;
import net.ccbluex.liquidbounce.utils.PPSCounter.BoundType;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager implements IMixinNetworkManager
{
	@Shadow
	@Final
	protected ReentrantReadWriteLock readWriteLock;

	@Shadow
	@Final
	private Queue outboundPacketsQueue;

	@Shadow
	public abstract boolean isChannelOpen();

	@Shadow
	protected abstract void flushOutboundQueue();

	@Shadow
	protected abstract void dispatchPacket(Packet<?> inPacket, GenericFutureListener<? extends Future<? super Void>>[] futureListeners);

	private final MSTimer lastRead = new MSTimer();

	@Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
	private void read(final ChannelHandlerContext context, final Packet<?> packet, final CallbackInfo callback)
	{
		// In-bound(Coming from server) packet name starts with 'S' (Server) (example: S00PacketKeepAlive)
		if (!Minecraft.getMinecraft().isIntegratedServerRunning() || packet.getClass().getSimpleName().charAt(0) == 'S')
		{
			final PacketEvent event = new PacketEvent(PacketImplKt.wrap(packet));
			LiquidBounce.eventManager.callEvent(event);

			if (event.isCancelled())
				callback.cancel();
			else
				PPSCounter.registerPacket(BoundType.INBOUND);
		}
	}

	@Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
	private void send(final Packet<?> packet, final CallbackInfo callback)
	{
		// Out-bound(Outgoing to server) packet name starts with 'C' (Client) (example: C00PacketKeepAlive)
		if (!Minecraft.getMinecraft().isIntegratedServerRunning() || packet.getClass().getSimpleName().charAt(0) == 'C')
		{
			final PacketEvent event = new PacketEvent(PacketImplKt.wrap(packet));
			LiquidBounce.eventManager.callEvent(event);

			if (event.isCancelled())
				callback.cancel();
			else
				PPSCounter.registerPacket(BoundType.OUTBOUND);
		}
	}

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
				// I used java reflection api because of InboundHandlerTuplePacketListener is private inner class inside NetworkManager class.
				for (final Class nwmanInnerClasses : NetworkManager.class.getDeclaredClasses())
					if (nwmanInnerClasses.getSimpleName().equalsIgnoreCase("InboundHandlerTuplePacketListener"))
						outboundPacketsQueue.add(nwmanInnerClasses.getConstructor(Packet.class, GenericFutureListener[].class).newInstance(packetIn, null));

				PPSCounter.registerPacket(BoundType.OUTBOUND);
			}
			catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e)
			{
				ClientUtils.getLogger().error("[NetworkManager] InboundHandlerTuplePacketListener reflection failed: {}", e, e);
			}
			finally
			{
				readWriteLock.writeLock().unlock();
			}
		}
	}

	@Override
	public long lastPacket()
	{
		return lastRead.getTime();
	}
}
