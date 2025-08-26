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
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.authlib;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;

@Mixin(value = MinecraftClient.class, remap = false)
public abstract class MixinMinecraftClient {

    @ModifyExpressionValue(method = "createUrlConnection", at = @At(
            value = "FIELD",
            target = "Lcom/mojang/authlib/minecraft/client/MinecraftClient;proxy:Ljava/net/Proxy;",
            remap = false
    ))
    private Proxy hookClientProxy(Proxy proxy) {
        // We only want to use the proxy when connecting to a server
        if (!(net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof ConnectScreen)) {
            return Proxy.NO_PROXY;
        }

        var currentProxy = ProxyManager.INSTANCE.getCurrentProxy();
        if (currentProxy != null && currentProxy.getForwardAuthentication()) {
            var credentials = currentProxy.getCredentials();

            if (credentials != null) {
                Authenticator.setDefault(new Authenticator() {
                    @Override
                    public PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                credentials.getUsername(),
                                credentials.getPassword().toCharArray()
                        );
                    }
                });
            }

            return new Proxy(
                    Proxy.Type.SOCKS,
                    currentProxy.getAddress()
            );
        }

        return Proxy.NO_PROXY;
    }

}
