/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiExploit;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {

    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "onChunkData", at = @At("RETURN"))
    private void injectChunkLoadEvent(final ChunkDataS2CPacket packet, final CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkLoadEvent(packet.getX(), packet.getZ()));
    }

    @Inject(method = "onUnloadChunk", at = @At("RETURN"))
    private void injectUnloadEvent(final UnloadChunkS2CPacket packet, final CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkUnloadEvent(packet.getX(), packet.getZ()));
    }

    @Redirect(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;add(DDD)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d onExplosionVelocity(final Vec3d instance, final double x, final double y, final double z) {
        final Vec3d originalVector = new Vec3d(x, y, z);
        if (ModuleAntiExploit.INSTANCE.getEnabled() && ModuleAntiExploit.INSTANCE.getLimitExplosionStrength()) {
            final double fixedX = MathHelper.clamp(x, -1000.0, 1000.0);
            final double fixedY = MathHelper.clamp(y, -1000.0, 1000.0);
            final double fixedZ = MathHelper.clamp(z, -1000.0, 1000.0);
            final Vec3d newVector = new Vec3d(fixedX, fixedY, fixedZ);
            if (!originalVector.equals(newVector)) {
                ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too strong explosion", true);
                return instance.add(newVector);
            }
        }
        return instance.add(x, y, z);
    }

    @Redirect(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getCount()I"))
    private int onParticleAmount(final ParticleS2CPacket instance) {
        if (ModuleAntiExploit.INSTANCE.getEnabled() && ModuleAntiExploit.INSTANCE.getLimitParticlesAmount() && 500 <= instance.getCount()) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too many particles", true);
            return 100;
        }
        return instance.getCount();
    }

    @Redirect(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getSpeed()F"))
    private float onParticleSpeed(final ParticleS2CPacket instance) {
        if (ModuleAntiExploit.INSTANCE.getEnabled() && ModuleAntiExploit.INSTANCE.getLimitParticlesSpeed() && 10.0f <= instance.getSpeed()) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too fast particles speed", true);
            return 10.0f;
        }
        return instance.getCount();
    }

    @Redirect(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ExplosionS2CPacket;getRadius()F"))
    private float onExplosionWorld(final ExplosionS2CPacket instance) {
        if (ModuleAntiExploit.INSTANCE.getLimitExplosionRange()) {
            final float radius = MathHelper.clamp(instance.getRadius(), -1000.0f, 1000.0f);
            if (radius != instance.getRadius()) {
                ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too big TNT explosion radius", true);
                return radius;
            }
        }
        return instance.getRadius();
    }

    @Redirect(method = "onGameStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket;getReason()Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket$Reason;"))
    private GameStateChangeS2CPacket.Reason onGameStateChange(final GameStateChangeS2CPacket instance) {
        if (ModuleAntiExploit.INSTANCE.getEnabled() && instance.getReason() == GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN && ModuleAntiExploit.INSTANCE.getCancelDemo()) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Cancelled demo GUI (just annoying thing)", false);
            return null;
        } else {
            return instance.getReason();
        }
    }

    @Inject(method = "onHealthUpdate", at = @At("RETURN"))
    private void injectHealthUpdate(HealthUpdateS2CPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new HealthUpdateEvent(packet.getHealth(), packet.getFood(), packet.getSaturation()));
        if (packet.getHealth() == 0) {
            EventManager.INSTANCE.callEvent(new DeathEvent());
        }
    }

    @Inject(method = "onPlayerPositionLook", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(DDD)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void injectNoRotateSet(final PlayerPositionLookS2CPacket packet, final CallbackInfo ci, final PlayerEntity playerEntity, final Vec3d vec3d, final boolean bl, final boolean bl2, final boolean bl3, final double d, final double e, final double f, final double g, final double h, final double i) {
        final float j = packet.getYaw();
        final float k = packet.getPitch();

        if (!ModuleNoRotateSet.INSTANCE.getEnabled() || MinecraftClient.getInstance().currentScreen instanceof DownloadingTerrainScreen) {
            return;
        }

        // Confirm teleport
        this.connection.send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
        // Silently accept yaw and pitch values requested by the server.
        this.connection.send(new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), j, k, false));
        final Choice activeChoice = ModuleNoRotateSet.INSTANCE.getMode().getActiveChoice();
        if (activeChoice.equals(ModuleNoRotateSet.ResetRotation.INSTANCE)) {
            // Changes you server side rotation and then resets it with provided settings
            RotationManager.INSTANCE.setTicksUntilReset(ModuleNoRotateSet.ResetRotation.INSTANCE.getRotationsConfigurable().getKeepRotationTicks());
            RotationManager.INSTANCE.setActiveConfigurable(ModuleNoRotateSet.ResetRotation.INSTANCE.getRotationsConfigurable());
            RotationManager.INSTANCE.setCurrentRotation(new Rotation(j, k));
            RotationManager.INSTANCE.setTargetRotation(new Rotation(j, k));
        } else {
            // Increase yaw and pitch by a value so small that the difference cannot be seen, just to update the rotations server-side.
            playerEntity.setYaw(playerEntity.prevYaw + 0.000001f);
            playerEntity.setPitch(playerEntity.prevPitch + 0.000001f);
        }

        ci.cancel();
    }

}
