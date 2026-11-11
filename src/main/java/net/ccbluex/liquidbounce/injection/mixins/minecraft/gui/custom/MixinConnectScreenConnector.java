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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.ccbluex.liquidbounce.features.session.ClientSession;
import net.ccbluex.liquidbounce.features.session.SessionContext;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The thread that opens the connection of a connect screen.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public abstract class MixinConnectScreenConnector {

    @Unique
    private final ClientSession liquid_bounce$session = SessionContext.getCurrent();

    /**
     * The connection belongs to the session that asked for it, and logs in as its account.
     */
    @WrapMethod(method = "run")
    private void connectWithinSession(Operation<Void> original) {
        var previous = SessionContext.enter(this.liquid_bounce$session);
        try {
            original.call();
        } finally {
            SessionContext.exit(previous);
        }
    }

    @WrapWithCondition(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/server/DownloadedPackSource;configureForServerControl(Lnet/minecraft/network/Connection;Lnet/minecraft/client/resources/server/ServerPackManager$PackPromptStatus;)V"))
    private boolean onlyMainSessionLoadsServerPacks(DownloadedPackSource source, Connection connection, ServerPackManager.PackPromptStatus status) {
        return this.liquid_bounce$session.isMain();
    }

}
