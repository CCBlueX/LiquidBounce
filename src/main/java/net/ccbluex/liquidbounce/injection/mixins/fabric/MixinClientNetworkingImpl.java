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
package net.ccbluex.liquidbounce.injection.mixins.fabric;

import net.ccbluex.liquidbounce.features.client.ClientState;
import net.fabricmc.fabric.impl.networking.client.ClientConfigurationNetworkAddon;
import net.fabricmc.fabric.impl.networking.client.ClientNetworkingImpl;
import net.fabricmc.fabric.impl.networking.client.ClientPlayNetworkAddon;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fabric API keeps the network addon of the one connection there used to be in a static field. Clients
 * configuring at the same time would overwrite each other's, so every client gets its own.
 */
@Mixin(value = ClientNetworkingImpl.class, remap = false)
public abstract class MixinClientNetworkingImpl {

    @Unique
    private static final ClientState<ClientConfigurationNetworkAddon> CONFIGURATION_ADDON =
        new ClientState<>(() -> null);

    @Unique
    private static final ClientState<ClientPlayNetworkAddon> PLAY_ADDON = new ClientState<>(() -> null);

    @Redirect(method = "*", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
        target = "Lnet/fabricmc/fabric/impl/networking/client/ClientNetworkingImpl;currentConfigurationAddon:Lnet/fabricmc/fabric/impl/networking/client/ClientConfigurationNetworkAddon;"))
    private static ClientConfigurationNetworkAddon getConfigurationAddonOfClient() {
        return CONFIGURATION_ADDON.get();
    }

    @Redirect(method = "*", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC,
        target = "Lnet/fabricmc/fabric/impl/networking/client/ClientNetworkingImpl;currentConfigurationAddon:Lnet/fabricmc/fabric/impl/networking/client/ClientConfigurationNetworkAddon;"))
    private static void setConfigurationAddonOfClient(ClientConfigurationNetworkAddon addon) {
        CONFIGURATION_ADDON.set(addon);
    }

    @Redirect(method = "*", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
        target = "Lnet/fabricmc/fabric/impl/networking/client/ClientNetworkingImpl;currentPlayAddon:Lnet/fabricmc/fabric/impl/networking/client/ClientPlayNetworkAddon;"))
    private static ClientPlayNetworkAddon getPlayAddonOfClient() {
        return PLAY_ADDON.get();
    }

    @Redirect(method = "*", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC,
        target = "Lnet/fabricmc/fabric/impl/networking/client/ClientNetworkingImpl;currentPlayAddon:Lnet/fabricmc/fabric/impl/networking/client/ClientPlayNetworkAddon;"))
    private static void setPlayAddonOfClient(ClientPlayNetworkAddon addon) {
        PLAY_ADDON.set(addon);
    }

}
