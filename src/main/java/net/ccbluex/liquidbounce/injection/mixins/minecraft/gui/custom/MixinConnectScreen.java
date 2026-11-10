/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import net.ccbluex.liquidbounce.additions.ConnectScreenAddition;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ConnectionDetailsEvent;
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent;
import net.ccbluex.liquidbounce.features.misc.SelfDestruct;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinScreen;
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ConnectionDetails;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen extends MixinScreen implements ConnectScreenAddition {

    @Shadow
    private volatile @Nullable Connection connection;

    @Shadow
    public abstract void connect(
        Minecraft client, ServerAddress address, ServerData info, @Nullable TransferState cookieStorage);

    @Unique
    private ServerAddress serverAddress = null;

    @Unique
    private volatile @Nullable ConnectionDetails connectionDetails = null;

    @Inject(method = "connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V", at = @At("HEAD"), cancellable = true)
    private void injectConnect(Minecraft client, ServerAddress address, ServerData info, TransferState cookieStorage, CallbackInfo ci) {
        this.serverAddress = address;
        var event = EventManager.INSTANCE.callEvent(new ServerConnectEvent((ConnectScreen) (Object) this, address, info, cookieStorage));

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * The cancel button sits at a quarter of the height, the status at half of it, so on tall GUIs the
     * connection details under the status would run into the button. Theme pages place them from this offset.
     */
    @ModifyConstant(method = "extractRenderState", constant = @Constant(intValue = 50))
    private int moveStatusUp(int original) {
        return SelfDestruct.INSTANCE.isDestructed() ? original : original + 30;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void updateConnectionDetails(CallbackInfo ci) {
        var clientConnection = this.connection;
        var serverAddress = this.serverAddress;

        if (clientConnection == null || serverAddress == null) {
            return;
        }

        var details = ConnectionDetails.of(clientConnection, serverAddress);
        if (!details.equals(this.connectionDetails)) {
            this.connectionDetails = details;
            EventManager.INSTANCE.callEvent(new ConnectionDetailsEvent(details));
        }
    }

    @Override
    public @Nullable ConnectionDetails liquidbounce$getConnectionDetails() {
        return this.connectionDetails;
    }

}
