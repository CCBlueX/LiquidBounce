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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.MovementInputEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSuperKnockback;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.client.TickStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {

    @Shadow
    @Final
    private GameOptions settings;

    @Inject(method = "getMovementMultiplier", at = @At("RETURN"), cancellable = true)
    private static void hookFreeCamCanceledMovementInput(boolean positive, boolean negative, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ModuleFreeCam.INSTANCE.cancelMovementInput(cir.getReturnValue()));
    }

    /**
     * Hook inventory move module
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean hookInventoryMove(KeyBinding keyBinding) {
        return ModuleInventoryMove.INSTANCE.shouldHandleInputs(keyBinding) ? InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), keyBinding.boundKey.getCode()) : keyBinding.isPressed();
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;pressingBack:Z", ordinal = 0))
    private void hookInventoryMoveSprint(boolean slowDown, float f, CallbackInfo ci) {
        if (ModuleInventoryMove.INSTANCE.shouldHandleInputs(this.settings.sprintKey)) {
            this.settings.sprintKey.setPressed(InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), this.settings.sprintKey.boundKey.getCode()));
        }
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;pressingRight:Z", shift = At.Shift.AFTER))
    private void injectMovementInputEvent(boolean slowDown, float f, CallbackInfo ci) {
        var event = new MovementInputEvent(this.pressingForward, this.pressingBack, this.pressingLeft, this.pressingRight);

        EventManager.INSTANCE.callEvent(event);

        this.pressingForward = event.getForwards();
        this.pressingBack = event.getBackwards();
        this.pressingLeft = event.getLeft();
        this.pressingRight = event.getRight();
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z", shift = At.Shift.BEFORE))
    private void injectStrafing(boolean slowDown, float f, CallbackInfo ci) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        if (rotationManager.getActiveConfigurable() == null || !rotationManager.getActiveConfigurable().getFixVelocity() || rotation == null || player == null) {
            return;
        }

        float deltaYaw = player.getYaw() - rotation.getYaw();

        float x = this.movementSideways;
        float z = this.movementForward;

        float newX = x * MathHelper.cos(deltaYaw * 0.017453292f) - z * MathHelper.sin(deltaYaw * 0.017453292f);
        float newZ = z * MathHelper.cos(deltaYaw * 0.017453292f) + x * MathHelper.sin(deltaYaw * 0.017453292f);

        this.movementSideways = Math.round(newX);
        this.movementForward = Math.round(newZ);
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;sneakKey:Lnet/minecraft/client/option/KeyBinding;", shift = At.Shift.AFTER))
    private void hookSuperKnockbackStopMoving(boolean slowDown, float f, CallbackInfo ci) {
        if (ModuleSuperKnockback.INSTANCE.shouldStopMoving()) {
            this.movementForward = 0.0f;
        }
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z"))
    private void injectForcedState(KeyboardInput instance, boolean value) {
        Boolean enforceEagle = TickStateManager.INSTANCE.getEnforcedState().getEnforceEagle();
        instance.sneaking = enforceEagle != null ? enforceEagle : value;
    }

}
