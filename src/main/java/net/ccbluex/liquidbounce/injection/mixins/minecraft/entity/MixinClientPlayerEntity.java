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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePortalMenu;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoSlow;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModulePerfectHorseJump;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleStep;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoSwing;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.client.TickStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    /**
     * Hook entity tick event
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.AFTER))
    private void hookTickEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerTickEvent());
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
    private void hookPushOut(CallbackInfo callbackInfo) {
        final PlayerPushOutEvent pushOutEvent = new PlayerPushOutEvent();
        EventManager.INSTANCE.callEvent(pushOutEvent);
        if (pushOutEvent.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    /**
     * Hook push out function tick at HEAD and call push out event, which is able to stop the cancel the execution.
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
     * Hook sprint affect from NoSlow module
     */
    @Redirect(method = "tickMovement", slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;getFoodLevel()I"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isFallFlying()Z")), at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"), require = 2, allow = 2)
    private boolean hookSprintAffect(ClientPlayerEntity playerEntity) {
        if (ModuleNoSlow.INSTANCE.getEnabled()) {
            return false;
        }

        return playerEntity.isUsingItem();
    }

    // Silent rotations (Rotation Manager)

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float hookSilentRotationYaw(ClientPlayerEntity instance) {
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        if (rotation == null || !rotationManager.shouldUpdate()) {
            return instance.getYaw();
        }

        return rotation.getYaw();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float hookSilentRotationPitch(ClientPlayerEntity instance) {
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        if (rotation == null || !rotationManager.shouldUpdate()) {
            return instance.getPitch();
        }

        return rotation.getPitch();
    }

    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    private void injectForcedState(CallbackInfoReturnable<Boolean> cir) {
        Boolean enforceEagle = TickStateManager.INSTANCE.getEnforcedState().getEnforceEagle();

        if (enforceEagle != null) {
            cir.setReturnValue(enforceEagle);
            cir.cancel();
        }
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
            if (ModuleNoSwing.INSTANCE.getServerSide()) {
                networkHandler.sendPacket(new HandSwingC2SPacket(hand));
            }
            ci.cancel();
        }
    }

    @Inject(method = "getMountJumpStrength", at = @At("HEAD"), cancellable = true)
    private void hookMountJumpStrength(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        if (ModulePerfectHorseJump.INSTANCE.getEnabled()) {
            callbackInfoReturnable.setReturnValue(1f);
        }
    }

    @Redirect(method = "tickMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z", ordinal = 1))
    private boolean hookFreeCamPreventCreativeFly(PlayerAbilities instance) {
        return !ModuleFreeCam.INSTANCE.getEnabled() && instance.allowFlying;
    }

    @ModifyVariable(method = "sendMovementPackets", at = @At("STORE"), ordinal = 3)
    private boolean hookFreeCamPreventRotations(boolean bl4) {
        return !ModuleFreeCam.INSTANCE.shouldDisableRotations() && bl4;
    }
}
