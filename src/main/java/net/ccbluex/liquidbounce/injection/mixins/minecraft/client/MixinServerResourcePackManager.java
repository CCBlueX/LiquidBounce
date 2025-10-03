/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer;
import net.ccbluex.liquidbounce.utils.client.ClientUtilsKt;
import net.minecraft.client.resource.server.PackStateChangeCallback;
import net.minecraft.client.resource.server.ServerResourcePackManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.UUID;

@Mixin(ServerResourcePackManager.class)
public abstract class MixinServerResourcePackManager {

    @Shadow
    @Final
    PackStateChangeCallback stateChangeCallback;

    @Inject(method = "addResourcePack(Ljava/util/UUID;Ljava/net/URL;Lcom/google/common/hash/HashCode;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resource/server/ServerResourcePackManager;onAdd(Ljava/util/UUID;Lnet/minecraft/client/resource/server/ServerResourcePackManager$PackEntry;)V"), cancellable = true)
    private void injectConnectionRefuse(CallbackInfo callbackInfo, @Local(argsOnly = true) UUID id, @Local(argsOnly = true) URL url) {
        var port = url.getPort();
        if (port != ClientInteropServer.INSTANCE.getPort()) {
            return;
        }

        try {
            var address = InetAddress.getByName(url.getHost());
            if (!address.isLoopbackAddress()) {
                return;
            }

            this.stateChangeCallback.onFinish(id, PackStateChangeCallback.FinishState.DOWNLOAD_FAILED);
            callbackInfo.cancel();
            ClientUtilsKt.getLogger().info("Refused resource pack download from interop server. URL: {}", url);
        } catch (UnknownHostException e) {
            ClientUtilsKt.getLogger().error("Failed to check connection refuse for resource pack.", e);
        }
    }
}
