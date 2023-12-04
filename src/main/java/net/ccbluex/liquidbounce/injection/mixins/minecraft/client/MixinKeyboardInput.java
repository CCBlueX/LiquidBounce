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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSuperKnockback;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.client.KeybindExtensionsKt;
import net.ccbluex.liquidbounce.utils.client.TickStateManager;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
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
        Boolean enforced = KeybindExtensionsKt.getEnforced(keyBinding);
        return ModuleInventoryMove.INSTANCE.shouldHandleInputs(keyBinding) ? (enforced != null ? enforced : KeybindExtensionsKt.getPressedOnKeyboard(keyBinding)) : (enforced != null ? enforced : keyBinding.isPressed());
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;pressingBack:Z", ordinal = 0))
    private void hookInventoryMoveSprint(boolean slowDown, float f, CallbackInfo ci) {
        if (ModuleInventoryMove.INSTANCE.shouldHandleInputs(this.settings.sprintKey)) {
            this.settings.sprintKey.setPressed(KeybindExtensionsKt.getPressedOnKeyboard(this.settings.sprintKey));
        }
    }

    @Inject(method = "tick", at = @At("RETURN"), allow = 1)
    private void injectMovementInputEvent(boolean slowDown, float f, CallbackInfo ci) {
        var event = new MovementInputEvent(new DirectionalInput(this.pressingForward, this.pressingBack, this.pressingLeft, this.pressingRight), this.jumping, this.sneaking);

        EventManager.INSTANCE.callEvent(event);

        var directionalInput = event.getDirectionalInput();

        this.pressingForward = directionalInput.getForwards();
        this.pressingBack = directionalInput.getBackwards();
        this.pressingLeft = directionalInput.getLeft();
        this.pressingRight = directionalInput.getRight();
        this.movementForward = KeyboardInput.getMovementMultiplier(directionalInput.getForwards(), directionalInput.getBackwards());
        this.movementSideways = KeyboardInput.getMovementMultiplier(directionalInput.getLeft(), directionalInput.getRight());
        this.jumping = event.getJumping();
        this.sneaking = event.getSneaking();
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z", shift = At.Shift.BEFORE))
    private void injectStrafing(boolean slowDown, float f, CallbackInfo ci) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        if (rotationManager.getAimPlan() == null || !rotationManager.getAimPlan().getApplyVelocityFix() || rotation == null || player == null) {
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
