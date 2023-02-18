/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import net.ccbluex.liquidbounce.event.ChunkLoadEvent;
import net.ccbluex.liquidbounce.event.ChunkUnloadEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void injectChunkLoadEvent(ChunkDataS2CPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkLoadEvent(packet.getX(), packet.getZ()));
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void injectUnloadEvent(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkUnloadEvent(packet.getX(), packet.getZ()));
    }

    @Inject(method = "onPlayerPositionLook", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;updatePositionAndAngles(DDDFF)V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectNoRotateSet(PlayerPositionLookS2CPacket packet, CallbackInfo ci, PlayerEntity playerEntity, Vec3d vec3d, boolean bl, boolean bl2, boolean bl3, double d, double e, double f, double g, double h, double i, float j, float k) {
        if (!ModuleNoRotateSet.INSTANCE.getEnabled() || MinecraftClient.getInstance().currentScreen instanceof DownloadingTerrainScreen) {
            return;
        }

        // Accept the requested position only.
        playerEntity.updatePosition(e, g, i);
        this.connection.send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
        // Silently accept yaw and pitch values requested by the server.
        this.connection.send(new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), j, k, false));
        // Increase yaw and pitch by a value so small that the difference cannot be seen, just to update the rotations server-side.
        playerEntity.setYaw(playerEntity.prevYaw + 0.000001f);
        playerEntity.setPitch(playerEntity.prevPitch + 0.000001f);

        ci.cancel();
    }

}
