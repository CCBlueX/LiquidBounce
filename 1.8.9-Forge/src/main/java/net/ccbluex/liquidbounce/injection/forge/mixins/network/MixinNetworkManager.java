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
import net.minecraft.util.LazyLoadBase;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager implements IMixinNetworkManager
{
	@Shadow
	@Final
	private static Logger logger;

	@Shadow
	@Final
	public static LazyLoadBase<NioEventLoopGroup> CLIENT_NIO_EVENTLOOP;

	@Shadow
	@Final
	public static LazyLoadBase<EpollEventLoopGroup> CLIENT_EPOLL_EVENTLOOP;

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

	// FIXME: BUG (Can't ping and connect to any server in serverlist when enable it)
	// @Overwrite
	// @Inject(method = "createNetworkManagerAndConnect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/NetworkManager;", at = @At("HEAD"), cancellable = true)
	// private static void createNetworkManagerAndConnect(final InetAddress address, final int serverPort, final boolean useNativeTransport, final CallbackInfoReturnable<NetworkManager> callback)
	// {
	// final NetworkManager networkmanager = new NetworkManager(EnumPacketDirection.CLIENTBOUND);
	//
	// final Class<? extends Channel> channelClass;
	// final LazyLoadBase<? extends EventLoopGroup> eventLoopGroupLazy;
	// if (Epoll.isAvailable() && useNativeTransport)
	// {
	// channelClass = EpollSocketChannel.class;
	// eventLoopGroupLazy = CLIENT_EPOLL_EVENTLOOP;
	// }
	// else
	// {
	// channelClass = NioSocketChannel.class;
	// eventLoopGroupLazy = CLIENT_NIO_EVENTLOOP;
	// }
	//
	// final EventLoopGroup eventLoopGroup = eventLoopGroupLazy.getValue();
	// if (eventLoopGroup != null)
	// logger.info("using EventLoopGroup " + eventLoopGroup.getClass().getSimpleName() + " to connect " + address);
	// else
	// logger.warn("eventLoopGroup is null!!! This can't be happened!");
	//
	// // Connect to the server
	// new Bootstrap().group(eventLoopGroup).handler(new ChannelInitializer<Channel>()
	// {
	// protected void initChannel(final Channel channel)
	// {
	// // Enable TCPNoDelay
	// try
	// {
	// channel.config().setOption(ChannelOption.TCP_NODELAY, true);
	// }
	// catch (final ChannelException var3)
	// {
	// }
	//
	// channel.pipeline() // Build pipeline
	// .addLast("timeout", new ReadTimeoutHandler(30)) // Timeout
	// .addLast("splitter", new MessageDeserializer2()) // Splitter
	// .addLast("decoder", new MessageDeserializer(EnumPacketDirection.CLIENTBOUND)) // Decoder
	// .addLast("prepender", new MessageSerializer2()) // Prepender
	// .addLast("encoder", new MessageSerializer(EnumPacketDirection.SERVERBOUND)) // Encoder
	// .addLast("packet_handler", networkmanager); // PacketHandler (NetworkManager)
	// }
	// }).channel(channelClass).connect(address, serverPort).syncUninterruptibly();
	//
	// callback.setReturnValue(networkmanager);
	// }

	@SuppressWarnings("unchecked")
	@Override
	public void sendPacketWithoutEvent(final Packet<?> packet)
	{
		if (isChannelOpen())
		{
			flushOutboundQueue();
			dispatchPacket(packet, null);
		}
		else
		{
			readWriteLock.writeLock().lock();

			try
			{
				// I used java reflection api because of InboundHandlerTuplePacketListener is private inner class inside NetworkManager class.
				for (final Class nwmanInnerClasses : NetworkManager.class.getDeclaredClasses())
					if ("InboundHandlerTuplePacketListener".equalsIgnoreCase(nwmanInnerClasses.getSimpleName()))
						outboundPacketsQueue.add(nwmanInnerClasses.getConstructor(Packet.class, GenericFutureListener[].class).newInstance(packet, null));
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

		PPSCounter.registerPacket(BoundType.OUTBOUND);
	}

	@Override
	public long lastPacket()
	{
		return lastRead.getTime();
	}
}
