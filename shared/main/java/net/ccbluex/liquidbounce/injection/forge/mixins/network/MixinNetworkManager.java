/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import java.util.Queue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.modules.misc.LagDetector;
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImplKt;
import net.ccbluex.liquidbounce.injection.implementations.IMixinNetworkManager;
import net.ccbluex.liquidbounce.utils.PacketCounter;
import net.ccbluex.liquidbounce.utils.PacketCounter.PacketType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkManager.InboundHandlerTuplePacketListener;
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
    private void handlePacketEvent_Receive(final ChannelHandlerContext context, final Packet<?> packet, final CallbackInfo callback)
    {
        // In-coming(Received from the server) packet name starts with 'S' (Server) (example: S00PacketKeepAlive)
        if (!Minecraft.getMinecraft().isIntegratedServerRunning() || packet.getClass().getSimpleName().charAt(0) == 'S')
        {
            final PacketEvent event = new PacketEvent(PacketImplKt.wrap(packet));
            LiquidBounce.eventManager.callEvent(event);

            LagDetector.Companion.onPacketReceived();

            if (event.isCancelled())
                callback.cancel();
            else
                PacketCounter.registerPacket(PacketType.INBOUND);
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void handlePacketEvent_Send(final Packet<?> packet, final CallbackInfo callback)
    {
        // Out-going(Sent to the server) packet name starts with 'C' (Client) (example: C00PacketKeepAlive)
        if (!Minecraft.getMinecraft().isIntegratedServerRunning() || packet.getClass().getSimpleName().charAt(0) == 'C')
        {
            final PacketEvent event = new PacketEvent(PacketImplKt.wrap(packet));
            LiquidBounce.eventManager.callEvent(event);

            if (event.isCancelled())
                callback.cancel();
            else
                PacketCounter.registerPacket(PacketType.OUTBOUND);
        }
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
