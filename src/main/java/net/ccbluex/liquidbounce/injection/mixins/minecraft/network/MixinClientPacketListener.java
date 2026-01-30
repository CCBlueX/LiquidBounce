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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Optional;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener extends ClientCommonPacketListenerImpl {

    protected MixinClientPacketListener(Minecraft client, Connection connection, CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    private void injectChunkLoadEvent(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkLoadEvent(packet.getX(), packet.getZ()));
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private void injectUnloadEvent(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkUnloadEvent(packet.pos()));
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("HEAD"))
    private void onChunkDeltaUpdateStart(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        ChunkUpdateFlag.chunkDeltaUpdating = true;
    }

    @Inject(method = "handleTeleportEntity", at = @At("RETURN"))
    private void hookOnEntityPosition(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        EntityMoveTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "handleBlockUpdate", at = @At("RETURN"))
    private void hookOnBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        BlockChangeTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
    private void hookOnChunkDeltaUpdate(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        BlockChangeTrigger.INSTANCE.postChunkUpdateHandler(packet);
    }

    @Inject(method = "handleAddEntity", at = @At("RETURN"))
    private void hookOnEntitySpawn(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        CrystalSpawnTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "handleSoundEntityEvent", at = @At("RETURN"))
    private void hookOnPlaySoundFromEntity(ClientboundSoundEntityPacket packet, CallbackInfo ci) {
        ExplodeSoundTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "handleRemoveEntities", at = @At("RETURN"))
    private void hookOnEntitiesDestroy(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        CrystalDestroyTrigger.INSTANCE.notify(packet);
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At("RETURN"))
    private void onChunkDeltaUpdateEnd(ClientboundSectionBlocksUpdatePacket packet, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ChunkDeltaUpdateEvent(packet));
        ChunkUpdateFlag.chunkDeltaUpdating = false;
    }

    @ModifyExpressionValue(method = "setTitleText", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundSetTitleTextPacket;text()Lnet/minecraft/network/chat/Component;"))
    private @Nullable Component hookOnTitle(@Nullable Component original, @Cancellable CallbackInfo ci) {
        var event = new TitleEvent.Title(original);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
        return event.getText();
    }

    @ModifyExpressionValue(method = "setSubtitleText", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundSetSubtitleTextPacket;text()Lnet/minecraft/network/chat/Component;"))
    private @Nullable Component hookOnSubtitle(@Nullable Component original, @Cancellable CallbackInfo ci) {
        var event = new TitleEvent.Subtitle(original);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
        return event.getText();
    }

    @ModifyArgs(method = "setTitlesAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setTimes(III)V"))
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
    @Inject(method = "handleTitlesClear", at = @At(value = "HEAD"), cancellable = true)
    private void hookOnTitleClear(ClientboundClearTitlesPacket packet, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(packet, (ClientGamePacketListener) this, this.minecraft.packetProcessor());
        var event = new TitleEvent.Clear(packet.shouldResetTimes());
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        this.minecraft.gui.clearTitles();
        if (event.getReset()) {
            this.minecraft.gui.resetTitleTimes();
        }
        ci.cancel();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ModifyExpressionValue(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;playerKnockback()Ljava/util/Optional;"))
    private Optional<Vec3> onExplosionVelocity(Optional<Vec3> original) {
        var present = original.isPresent();
        if (present && ModuleAntiExploit.canLimit(Limit.EXPLOSION_STRENGTH)) {
            var vec = original.get();
            double fixedX = Mth.clamp(vec.x, -10.0, 10.0);
            double fixedY = Mth.clamp(vec.y, -10.0, 10.0);
            double fixedZ = Mth.clamp(vec.z, -10.0, 10.0);

            if (fixedX != vec.x || fixedY != vec.y || fixedZ != vec.z) {
                ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too strong explosion",
                        true);
                return Optional.of(new Vec3(fixedX, fixedY, fixedZ));
            }
        }

        return original;
    }

    @ModifyExpressionValue(method = "handleParticleEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;getCount()I", ordinal = 1))
    private int onParticleAmount(int original) {
        if (ModuleAntiExploit.canLimit(Limit.PARTICLES_AMOUNT) && 500 <= original) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too many particles", true);
            return 100;
        }
        return original;
    }

    @ModifyExpressionValue(method = "handleParticleEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundLevelParticlesPacket;getMaxSpeed()F"))
    private float onParticleSpeed(float original) {
        if (ModuleAntiExploit.canLimit(Limit.PARTICLES_SPEED) && 10.0f <= original) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Limited too fast particles speed", true);
            return 10.0f;
        }
        return original;
    }

    @ModifyExpressionValue(method = "handleGameEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket;getEvent()Lnet/minecraft/network/protocol/game/ClientboundGameEventPacket$Type;"))
    private ClientboundGameEventPacket.Type onGameStateChange(ClientboundGameEventPacket.Type original) {
        if (ModuleAntiExploit.INSTANCE.getRunning() && original == ClientboundGameEventPacket.DEMO_EVENT && ModuleAntiExploit.INSTANCE.getCancelDemo()) {
            ModuleAntiExploit.INSTANCE.notifyAboutExploit("Cancelled demo GUI (just annoying thing)", false);
            return null;
        }

        return original;
    }

    @Inject(method = "handleSetHealth", at = @At("HEAD"))
    private void injectHealthUpdate(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;

        if (player == null) {
            return;
        }

        EventManager.INSTANCE.callEvent(new HealthUpdateEvent(packet.getHealth(), packet.getFood(), packet.getSaturation(), player.getHealth()));

        if (packet.getHealth() == 0) {
            EventManager.INSTANCE.callEvent(DeathEvent.INSTANCE);
        }
    }

    @Unique
    private final ThreadLocal<Rotation> rotationThreadLocal = ThreadLocal.withInitial(() -> null);

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;setValuesFromPositionPacket(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;Lnet/minecraft/world/entity/Entity;Z)Z"))
    private void injectPlayerPositionLook(
        ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Local Player playerEntity) {
        rotationThreadLocal.set(new Rotation(playerEntity.getYRot(), playerEntity.getXRot(), true));
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void injectNoRotateSet(ClientboundPlayerPositionPacket packet, CallbackInfo ci, @Local Player playerEntity) {
        if (!ModuleNoRotateSet.INSTANCE.getRunning() || Minecraft.getInstance().screen instanceof LevelLoadingScreen) {
            return;
        }

        var prevRotation = this.rotationThreadLocal.get();
        if (prevRotation == null) {
            return;
        }
        this.rotationThreadLocal.remove();

        if (ModuleNoRotateSet.INSTANCE.getMode().getActiveMode() == ModuleNoRotateSet.ResetRotation.INSTANCE) {
            // Changes your server side rotation and then resets it with provided settings
            var rotationTarget = ModuleNoRotateSet.ResetRotation.INSTANCE.getRotations().toRotationTarget(
                    new Rotation(playerEntity.getYRot(), playerEntity.getXRot(), true),
                    null,
                    true,
                    null
            );
            RotationManager.INSTANCE.setRotationTarget(rotationTarget, Priority.NOT_IMPORTANT, ModuleNoRotateSet.INSTANCE);
        }

        // Increase yaw and pitch by a value so small that the difference cannot be seen,
        // just to update the rotation server-side.
        playerEntity.setYRot(prevRotation.yRot() + 0.000001f);
        playerEntity.setXRot(prevRotation.xRot() + 0.000001f);
    }

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private String handleSendMessage(String content) {
        var result = ModuleBetterChat.INSTANCE.modifyMessage(content);

        if (DisablerSpigotSpam.INSTANCE.getRunning()) {
            return DisablerSpigotSpam.INSTANCE.getMessage() + " " + result;
        }

        return result;
    }

}
