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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSuperKnockback;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleAntiHunger;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePortalMenu;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleEntityControl;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoSlow;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint;
import net.ccbluex.liquidbounce.features.module.modules.movement.step.ModuleStep;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoSwing;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.web.socket.protocol.rest.game.PlayerData;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends MixinPlayerEntity {

    @Shadow
    public Input input;

    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    @Shadow
    public abstract boolean isSubmergedInWater();

    @Shadow
    protected abstract boolean isWalking();

    @Unique
    private PlayerData lastKnownStatistics = null;

    /**
     * Hook entity tick event
     */
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
            shift = At.Shift.BEFORE,
            ordinal = 0),
            cancellable = true)
    private void hookTickEvent(CallbackInfo ci) {
        var tickEvent = new PlayerTickEvent();
        EventManager.INSTANCE.callEvent(tickEvent);

        if (tickEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V",
            shift = At.Shift.AFTER,
            ordinal = 0))
    private void hookPostTickEvent(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new PlayerPostTickEvent());

        // Call player statistics change event when statistics change
        var statistics = PlayerData.Companion.fromPlayer((ClientPlayerEntity) (Object) this);
        if (lastKnownStatistics == null || lastKnownStatistics != statistics) {
            EventManager.INSTANCE.callEvent(ClientPlayerDataEvent.Companion.fromPlayerStatistics(statistics));
        }
        this.lastKnownStatistics = statistics;
    }

    /**
     * Hook entity movement tick event
     */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hookMovementTickEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerMovementTickEvent());
    }

    /**
     * Hook entity movement tick event at HEAD and call out PRE tick movement event
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void hookMovementPre(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerNetworkMovementTickEvent(EventState.PRE));
    }

    /**
     * Hook entity movement tick event at RETURN and call out POST tick movement event
     */
    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void hookMovementPost(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerNetworkMovementTickEvent(EventState.POST));
    }

    /**
     * Hook push out function tick at HEAD and call out push out event, which is able to stop the cancel the execution.
     */
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void hookPushOut(double x, double z, CallbackInfo ci) {
        final PlayerPushOutEvent pushOutEvent = new PlayerPushOutEvent();
        EventManager.INSTANCE.callEvent(pushOutEvent);
        if (pushOutEvent.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * Hook move function at HEAD and call out move event, which is able to stop the cancel the execution.
     */
    @Inject(method = "move", at = @At("HEAD"))
    private void hookMove(MovementType type, Vec3d movement, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerMoveEvent(type, movement));
    }

    /**
     * Hook portal menu module to make opening menus in portals possible
     */
    @Redirect(method = "updateNausea", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;shouldPause()Z"))
    private boolean hookNetherClosingScreen(Screen screen) {
        if (ModulePortalMenu.INSTANCE.getEnabled()) {
            return true;
        }
        return screen.shouldPause();
    }

    /**
     * Hook custom multiplier
     */
    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0))
    private void hookCustomMultiplier(CallbackInfo callbackInfo) {
        final Input input = this.input;
        // reverse
        input.movementForward /= 0.2f;
        input.movementSideways /= 0.2f;

        // then
        final PlayerUseMultiplier playerUseMultiplier = new PlayerUseMultiplier(0.2f, 0.2f);
        EventManager.INSTANCE.callEvent(playerUseMultiplier);
        input.movementForward *= playerUseMultiplier.getForward();
        input.movementSideways *= playerUseMultiplier.getSideways();
    }

    /**
     * Hook sprint effect from NoSlow module
     */
    @Redirect(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean hookSprintAffectStart(ClientPlayerEntity playerEntity) {
        if (ModuleNoSlow.INSTANCE.getEnabled()) {
            return false;
        }

        return playerEntity.isUsingItem();
    }

    // Silent rotations (Rotation Manager)

    @Redirect(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float hookSilentRotationYaw(ClientPlayerEntity instance) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return instance.getYaw();
        }

        return rotation.getYaw();
    }

    @Redirect(method = {"sendMovementPackets", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float hookSilentRotationPitch(ClientPlayerEntity instance) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return instance.getPitch();
        }

        return rotation.getPitch();
    }

    @Inject(method = "isAutoJumpEnabled", cancellable = true, at = @At("HEAD"))
    private void injectLegitStep(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleStep.INSTANCE.getEnabled()) {
            cir.setReturnValue(ModuleStep.Legit.INSTANCE.isActive());
        }
    }

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void swingHand(Hand hand, CallbackInfo ci) {
        if (ModuleNoSwing.INSTANCE.getEnabled()) {
            if (!ModuleNoSwing.INSTANCE.shouldHideForServer()) {
                networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            }
            if (!ModuleNoSwing.INSTANCE.shouldHideForClient()) {
                swingHand(hand, false);
            }

            ci.cancel();
        }
    }

    @Inject(method = "getMountJumpStrength", at = @At("HEAD"), cancellable = true)
    private void hookMountJumpStrength(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (ModuleEntityControl.INSTANCE.getEnabled() && ModuleEntityControl.INSTANCE.getEnforceJumpStrength()) {
            callbackInfoReturnable.setReturnValue(1f);
        }
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z"))
    private boolean hookFreeCamPreventCreativeFly(boolean original) {
        return !ModuleFreeCam.INSTANCE.getEnabled() && original;
    }

    @ModifyVariable(method = "sendMovementPackets", at = @At("STORE"), ordinal = 2)
    private boolean hookFreeCamPreventRotations(boolean bl4) {
        return (!ModuleFreeCam.INSTANCE.shouldDisableRotations() ||  ModuleRotations.INSTANCE.shouldSendCustomRotation())  && bl4;
    }

    @ModifyConstant(method = "canSprint", constant = @Constant(floatValue = 6.0F), slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;getFoodLevel()I", ordinal = 0)))
    private float hookSprintIgnoreHunger(float constant) {
        return ModuleSprint.INSTANCE.shouldIgnoreHunger() ? -1F : constant;
    }

    @ModifyExpressionValue(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean hookAutoSprint(boolean original) {
        return !ModuleSuperKnockback.INSTANCE.shouldBlockSprinting() && !ModuleKillAura.INSTANCE.shouldBlockSprinting()
                && (ModuleSprint.INSTANCE.getEnabled() || original);
    }

    @Redirect(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isWalking()Z"))
    private boolean hookOmnidirectionalSprintB(ClientPlayerEntity instance) {
        return isOmniWalking(instance);
    }

    @ModifyExpressionValue(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Z"))
    private boolean hookSprintIgnoreBlindness(boolean original) {
        return !ModuleSprint.INSTANCE.shouldIgnoreBlindness() && original;
    }

    @Redirect(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isWalking()Z"))
    private boolean hookOmnidirectionalSprintC(ClientPlayerEntity instance) {
        return isOmniWalking(instance);
    }

    private boolean isOmniWalking(ClientPlayerEntity instance) {
        boolean hasMovement = Math.abs(instance.input.movementForward) > 1.0E-5F || Math.abs(instance.input.movementSideways) > 1.0E-5F;
        boolean isWalking = (double) Math.abs(instance.input.movementForward) >= 0.8 || (double) Math.abs(instance.input.movementSideways) >= 0.8;
        boolean modifiedIsWalking = this.isSubmergedInWater() ? hasMovement : isWalking;
        return ModuleSprint.INSTANCE.shouldSprintOmnidirectionally() ? modifiedIsWalking : this.isWalking();
    }

    @ModifyExpressionValue(method = "sendSprintingPacket", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSprinting()Z")
    )
    private boolean hookNoHungerSprint(boolean original) {
        return !(ModuleAntiHunger.INSTANCE.getEnabled() && ModuleAntiHunger.INSTANCE.getNoSprint()) && original;
    }

}
