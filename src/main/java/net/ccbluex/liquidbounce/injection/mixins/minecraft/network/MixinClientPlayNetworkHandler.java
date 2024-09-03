/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.common.ChunkUpdateFlag;
import net.ccbluex.liquidbounce.config.Choice;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.ModuleDisabler;
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.disablers.DisablerSpigotSpam;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiExploit;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.kotlin.Priority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler extends ClientCommonNetworkHandler {

    protected MixinClientPlayNetworkHandler(MinecraftClient client, ClientConnection connection, ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void injectChunkLoadEvent(ChunkDataS2CPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkLoadEvent(packet.getChunkX(), packet.getChunkZ()));
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void injectUnloadEvent(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkUnloadEvent(packet.pos().x, packet.pos().z));
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("HEAD"))
    private void onChunkDeltaUpdateStart(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        ChunkUpdateFlag.chunkUpdate = true;
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void onChunkDeltaUpdateEnd(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        var chunkPosition = packet.sectionPos.toChunkPos();
        EventManager.INSTANCE.callEvent(new ChunkDeltaUpdateEvent(chunkPosition.x, chunkPosition.z));
        ChunkUpdateFlag.chunkUpdate = false;
    }

    @ModifyArgs(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private void onExplosionVelocity(Args args) {
        double x = args.get(0);
        double y = args.get(1);
        double z = args.get(2);

        if (ModuleAntiExploit.INSTANCE.getEnabled() && ModuleAntiExploit.INSTANCE.getLimitExplosionStrength()) {
            double fixedX = MathHelper.clamp(x, -10.0, 10.0);
            double fixedY = MathHelper.clamp(y, -10.0, 10.0);
            double fixedZ = MathHelper.clamp(z, -10.0, 10.0);

            if (fixedX != x || fixedY != y || fixedZ != z) {
                ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too strong explosion",
                        true);
                args.set(0, fixedX);
                args.set(1, fixedY);
                args.set(2, fixedZ);
            }
        }
    }

    @ModifyExpressionValue(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getCount()I", ordinal = 1))
    private int onParticleAmount(int original) {
        if (ModuleAntiExploit.INSTANCE.getEnabled() && ModuleAntiExploit.INSTANCE.getLimitParticlesAmount() && 500 <= original) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too many particles", true);
            return 100;
        }
        return original;
    }

    @ModifyExpressionValue(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getSpeed()F"))
    private float onParticleSpeed(float original) {
        if (ModuleAntiExploit.INSTANCE.getEnabled() && ModuleAntiExploit.INSTANCE.getLimitParticlesSpeed() && 10.0f <= original) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too fast particles speed", true);
            return 10.0f;
        }
        return original;
    }

    @ModifyExpressionValue(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ExplosionS2CPacket;getRadius()F"))
    private float onExplosionWorld(float original) {
        if (ModuleAntiExploit.INSTANCE.getEnabled() && ModuleAntiExploit.INSTANCE.getLimitExplosionRange()) {
            float radius = MathHelper.clamp(original, -1000.0f, 1000.0f);
            if (radius != original) {
                ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too big TNT explosion radius", true);
                return radius;
            }
        }
        return original;
    }

    @ModifyExpressionValue(method = "onGameStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket;getReason()Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket$Reason;"))
    private GameStateChangeS2CPacket.Reason onGameStateChange(GameStateChangeS2CPacket.Reason original) {
        if (ModuleAntiExploit.INSTANCE.getEnabled() && original == GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN && ModuleAntiExploit.INSTANCE.getCancelDemo()) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Cancelled demo GUI (just annoying thing)", false);
            return null;
        }
        
        return original;
    }

    @Inject(method = "onHealthUpdate", at = @At("RETURN"))
    private void injectHealthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        ClientPlayerEntity player = this.client.player;

        if (player == null) {
            return;
        }

        EventManager.INSTANCE.callEvent(new HealthUpdateEvent(packet.getHealth(), packet.getFood(), packet.getSaturation(), player.getHealth()));

        if (packet.getHealth() == 0) {
            EventManager.INSTANCE.callEvent(new DeathEvent());
        }
    }

    @Inject(method = "onPlayerPositionLook", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(DDD)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectNoRotateSet(PlayerPositionLookS2CPacket packet, CallbackInfo ci, PlayerEntity playerEntity, Vec3d vec3d, boolean bl, boolean bl2, boolean bl3, double d, double e, double f, double g, double h, double i) {
        float j = packet.getYaw();
        float k = packet.getPitch();

        if (!ModuleNoRotateSet.INSTANCE.getEnabled() || MinecraftClient.getInstance().currentScreen instanceof DownloadingTerrainScreen) {
            return;
        }

        // Confirm teleport
        this.connection.send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
        // Silently accept yaw and pitch values requested by the server.
        this.connection.send(new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), j, k, false));
        Choice activeChoice = ModuleNoRotateSet.INSTANCE.getMode().getActiveChoice();
        if (activeChoice.equals(ModuleNoRotateSet.ResetRotation.INSTANCE)) {
            // Changes your server side rotation and then resets it with provided settings
            var aimPlan = ModuleNoRotateSet.ResetRotation.INSTANCE.getRotationsConfigurable().toAimPlan(new Rotation(j, k), null, null, true);
            RotationManager.INSTANCE.aimAt(aimPlan, Priority.NOT_IMPORTANT, ModuleNoRotateSet.INSTANCE);
        } else {
            // Increase yaw and pitch by a value so small that the difference cannot be seen, just to update the rotation server-side.
            playerEntity.setYaw(playerEntity.prevYaw + 0.000001f);
            playerEntity.setPitch(playerEntity.prevPitch + 0.000001f);
        }

        ci.cancel();
    }

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String handleSendMessage(String content) {
        if (ModuleDisabler.INSTANCE.getEnabled() && DisablerSpigotSpam.INSTANCE.getEnabled()) {
            return DisablerSpigotSpam.INSTANCE.getMessage() + " " + content;
        }
        return content;
    }

}
