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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.common.ChunkUpdateFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.triggers.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.disablers.DisablerSpigotSpam;
import net.ccbluex.liquidbounce.features.module.modules.misc.betterchat.ModuleBetterChat;
import net.ccbluex.liquidbounce.features.module.modules.player.Limit;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiExploit;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoRotateSet;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.kotlin.Priority;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

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

    @Inject(method = "onEntityPosition", at = @At("RETURN"))
    private void hookOnEntityPosition(EntityPositionS2CPacket packet, CallbackInfo ci) {
        EntityMoveTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "onBlockUpdate", at = @At("RETURN"))
    private void hookOnBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        BlockChangeTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void hookOnChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        BlockChangeTrigger.INSTANCE.postChunkUpdateHandler(packet);
    }

    @Inject(method = "onEntitySpawn", at = @At("RETURN"))
    private void hookOnEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        CrystalSpawnTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "onPlaySoundFromEntity", at = @At("RETURN"))
    private void hookOnPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet, CallbackInfo ci) {
        ExplodeSoundTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "onEntitiesDestroy", at = @At("RETURN"))
    private void hookOnEntitiesDestroy(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        CrystalDestroyTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
    private void onChunkDeltaUpdateEnd(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
        var chunkPosition = packet.sectionPos.toChunkPos();
        EventManager.INSTANCE.callEvent(new ChunkDeltaUpdateEvent(chunkPosition.x, chunkPosition.z));
        ChunkUpdateFlag.chunkUpdate = false;
    }

    @ModifyExpressionValue(method = "onTitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/TitleS2CPacket;text()Lnet/minecraft/text/Text;"))
    private @Nullable Text hookOnTitle(@Nullable Text original, @Cancellable CallbackInfo ci) {
        var event = new TitleEvent.Title(original);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
        return event.getText();
    }

    @ModifyExpressionValue(method = "onSubtitle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/SubtitleS2CPacket;text()Lnet/minecraft/text/Text;"))
    private @Nullable Text hookOnSubtitle(@Nullable Text original, @Cancellable CallbackInfo ci) {
        var event = new TitleEvent.Subtitle(original);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
        return event.getText();
    }

    @ModifyArgs(method = "onTitleFade", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;setTitleTicks(III)V"))
    private void hookOnTitleFade(Args args, @Cancellable CallbackInfo ci) {
        var event = new TitleEvent.Fade(args.get(0), args.get(1), args.get(2));
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
        args.set(0, event.getFadeInTicks());
        args.set(1, event.getStayTicks());
        args.set(2, event.getFadeOutTicks());
    }

    /**
     * This injection rewrites the method!!!
     */
    @Inject(method = "onTitleClear", at = @At(value = "HEAD"), cancellable = true)
    private void hookOnTitleClear(ClearTitleS2CPacket packet, CallbackInfo ci) {
        NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler) (Object) this, this.client);
        var event = new TitleEvent.Clear(packet.shouldReset());
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        this.client.inGameHud.clearTitle();
        if (event.getReset()) {
            this.client.inGameHud.setDefaultTitleFade();
        }
        ci.cancel();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ModifyExpressionValue(method = "onExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ExplosionS2CPacket;playerKnockback()Ljava/util/Optional;"))
    private Optional<Vec3d> onExplosionVelocity(Optional<Vec3d> original) {
        var present = original.isPresent();
        if (present && ModuleAntiExploit.canLimit(Limit.EXPLOSION_STRENGTH)) {
            var vec = original.get();
            double fixedX = MathHelper.clamp(vec.x, -10.0, 10.0);
            double fixedY = MathHelper.clamp(vec.y, -10.0, 10.0);
            double fixedZ = MathHelper.clamp(vec.z, -10.0, 10.0);

            if (fixedX != vec.x || fixedY != vec.y || fixedZ != vec.z) {
                ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too strong explosion",
                        true);
                return Optional.of(new Vec3d(fixedX, fixedY, fixedZ));
            }
        }

        return original;
    }

    @ModifyExpressionValue(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getCount()I", ordinal = 1))
    private int onParticleAmount(int original) {
        if (ModuleAntiExploit.canLimit(Limit.PARTICLES_AMOUNT) && 500 <= original) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too many particles", true);
            return 100;
        }
        return original;
    }

    @ModifyExpressionValue(method = "onParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;getSpeed()F"))
    private float onParticleSpeed(float original) {
        if (ModuleAntiExploit.canLimit(Limit.PARTICLES_SPEED) && 10.0f <= original) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too fast particles speed", true);
            return 10.0f;
        }
        return original;
    }

    @ModifyExpressionValue(method = "onGameStateChange", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket;getReason()Lnet/minecraft/network/packet/s2c/play/GameStateChangeS2CPacket$Reason;"))
    private GameStateChangeS2CPacket.Reason onGameStateChange(GameStateChangeS2CPacket.Reason original) {
        if (ModuleAntiExploit.INSTANCE.getRunning() && original == GameStateChangeS2CPacket.DEMO_MESSAGE_SHOWN && ModuleAntiExploit.INSTANCE.getCancelDemo()) {
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
            EventManager.INSTANCE.callEvent(DeathEvent.INSTANCE);
        }
    }

    private ThreadLocal<Rotation> rotationThreadLocal = ThreadLocal.withInitial(() -> null);

    @Inject(method = "onPlayerPositionLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;setPosition(Lnet/minecraft/entity/player/PlayerPosition;Ljava/util/Set;Lnet/minecraft/entity/Entity;Z)Z"))
    private void injectPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci, @Local PlayerEntity playerEntity) {
        rotationThreadLocal.set(new Rotation(playerEntity.getYaw(), playerEntity.getPitch(), true));
    }

    @Inject(method = "onPlayerPositionLook", at = @At("RETURN"))
    private void injectNoRotateSet(PlayerPositionLookS2CPacket packet, CallbackInfo ci, @Local PlayerEntity playerEntity) {
        if (!ModuleNoRotateSet.INSTANCE.getRunning() || MinecraftClient.getInstance().currentScreen instanceof DownloadingTerrainScreen) {
            return;
        }

        var prevRotation = this.rotationThreadLocal.get();
        if (prevRotation == null) {
            return;
        }
        this.rotationThreadLocal.remove();

        if (ModuleNoRotateSet.INSTANCE.getMode().getActiveChoice() == ModuleNoRotateSet.ResetRotation.INSTANCE) {
            // Changes your server side rotation and then resets it with provided settings
            var rotationTarget = ModuleNoRotateSet.ResetRotation.INSTANCE.getRotationsConfigurable().toRotationTarget(
                    new Rotation(playerEntity.getYaw(), playerEntity.getPitch(), true),
                    null,
                    true,
                    null
            );
            RotationManager.INSTANCE.setRotationTarget(rotationTarget, Priority.NOT_IMPORTANT, ModuleNoRotateSet.INSTANCE);
        }

        // Increase yaw and pitch by a value so small that the difference cannot be seen,
        // just to update the rotation server-side.
        playerEntity.setYaw(prevRotation.getYaw() + 0.000001f);
        playerEntity.setPitch(prevRotation.getPitch() + 0.000001f);
    }

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String handleSendMessage(String content) {
        var result = ModuleBetterChat.INSTANCE.modifyMessage(content);

        if (DisablerSpigotSpam.INSTANCE.getRunning()) {
            return DisablerSpigotSpam.INSTANCE.getMessage() + " " + result;
        }

        return result;
    }

}
