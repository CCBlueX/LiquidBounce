package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import net.ccbluex.liquidbounce.features.special.AntiModDisable;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.channel.ChannelHandlerContext;

@Mixin(FMLEventChannel.class)
public class MixinFMLEventChannel
{
    @Inject(method = "fireRead", at = @At("HEAD"), cancellable = true, remap = false)
    void injectAntiModDisable(final FMLProxyPacket msg, final ChannelHandlerContext ctx, final CallbackInfo callback)
    {
        if (AntiModDisable.canBlockForgeChannelPacket(msg.channel()))
            callback.cancel();
    }
}
