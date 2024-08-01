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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.event.events.RotatedMovementInputEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSuperKnockback;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSprint;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.utils.aiming.AimPlan;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.client.KeybindExtensionsKt;
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

    /**
     * Hook inventory move module
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean hookInventoryMove(KeyBinding keyBinding) {
        return ModuleInventoryMove.INSTANCE.shouldHandleInputs(keyBinding)
                ? KeybindExtensionsKt.getPressedOnKeyboard(keyBinding) : keyBinding.isPressed();
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;pressingBack:Z", ordinal = 0))
    private void hookInventoryMoveSprint(boolean slowDown, float f, CallbackInfo ci) {
        if (ModuleInventoryMove.INSTANCE.shouldHandleInputs(this.settings.sprintKey)) {
            this.settings.sprintKey.setPressed(KeybindExtensionsKt.getPressedOnKeyboard(this.settings.sprintKey));
        }
    }

    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/input/KeyboardInput;sneaking:Z", shift = At.Shift.AFTER), allow = 1)
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

        this.fixStrafeMovement();

        if (ModuleSuperKnockback.INSTANCE.shouldStopMoving()) {
            this.movementForward = 0f;

            ModuleSprint sprint = ModuleSprint.INSTANCE;

            if (sprint.getEnabled() && sprint.getAllDirections()) {
                this.movementSideways = 0f;
            }
        }

        this.jumping = event.getJumping();
        this.sneaking = event.getSneaking();
    }

    private void fixStrafeMovement() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        RotationManager rotationManager = RotationManager.INSTANCE;
        Rotation rotation = rotationManager.getCurrentRotation();
        AimPlan configurable = rotationManager.getStoredAimPlan();

        float z = this.movementForward;
        float x = this.movementSideways;

        final RotatedMovementInputEvent MoveInputEvent;

        if (configurable == null || !configurable.getApplyVelocityFix() || rotation == null || player == null) {
            MoveInputEvent = new RotatedMovementInputEvent(z, x);
            EventManager.INSTANCE.callEvent(MoveInputEvent);
        } else {
            float deltaYaw = player.getYaw() - rotation.getYaw();

            float newX = x * MathHelper.cos(deltaYaw * 0.017453292f) - z * MathHelper.sin(deltaYaw * 0.017453292f);
            float newZ = z * MathHelper.cos(deltaYaw * 0.017453292f) + x * MathHelper.sin(deltaYaw * 0.017453292f);

            MoveInputEvent = new RotatedMovementInputEvent(Math.round(newZ), Math.round(newX));
            EventManager.INSTANCE.callEvent(MoveInputEvent);
        }

        this.movementSideways = MoveInputEvent.getSideways();
        this.movementForward = MoveInputEvent.getForward();
    }

}
