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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.event.events.SprintEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection;
import net.ccbluex.liquidbounce.utils.input.InputTracker;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends MixinInput {

    @Shadow
    @Final
    private GameOptions settings;

    /**
     * Hook inventory move module
     */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean hookInventoryMove(KeyBinding instance, Operation<Boolean> original) {
        return original.call(instance) ||
                ModuleInventoryMove.INSTANCE.shouldHandleInputs(instance)
                        && InputTracker.INSTANCE.isPressedOnAny(instance);
    }

    /**
     * Later in the code, the sprint key is checked for being pressed. We need to update the state of the key
     * as well.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void hookInventoryMoveSprint(CallbackInfo ci) {
        if (ModuleInventoryMove.INSTANCE.shouldHandleInputs(this.settings.sprintKey)) {
            this.settings.sprintKey.setPressed(InputTracker.INSTANCE.isPressedOnAny(this.settings.sprintKey));
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput modifyInput(PlayerInput original) {
        this.initial = original;

        var event = new MovementInputEvent(new DirectionalInput(original), original.jump(), original.sneak());
        EventManager.INSTANCE.callEvent(event);
        var untransformedDirectionalInput = event.getDirectionalInput();
        var directionalInput = transformDirection(untransformedDirectionalInput);

        var sprintEvent = new SprintEvent(directionalInput, original.sprint(), SprintEvent.Source.INPUT);
        EventManager.INSTANCE.callEvent(sprintEvent);

        // Store the untransformed input for later use
        this.untransformed = new PlayerInput(
                untransformedDirectionalInput.getForwards(),
                untransformedDirectionalInput.getBackwards(),
                untransformedDirectionalInput.getLeft(),
                untransformedDirectionalInput.getRight(),
                event.getJump(),
                event.getSneak(),
                sprintEvent.getSprint()
        );

        return new PlayerInput(
                directionalInput.getForwards(),
                directionalInput.getBackwards(),
                directionalInput.getLeft(),
                directionalInput.getRight(),
                event.getJump(),
                event.getSneak(),
                sprintEvent.getSprint()
        );
    }

    @Unique
    private DirectionalInput transformDirection(DirectionalInput input) {
        var player = MinecraftClient.getInstance().player;
        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var configurable = RotationManager.INSTANCE.getActiveRotationTarget();

        float z = KeyboardInput.getMovementMultiplier(input.getForwards(), input.getBackwards());
        float x = KeyboardInput.getMovementMultiplier(input.getLeft(), input.getRight());

        if (configurable == null || configurable.getMovementCorrection() != MovementCorrection.SILENT
                || rotation == null || player == null) {
            return input;
        }

        float deltaYaw = player.getYaw() - rotation.getYaw();

        float newX = x * MathHelper.cos(deltaYaw * 0.017453292f) - z *
                MathHelper.sin(deltaYaw * 0.017453292f);
        float newZ = z * MathHelper.cos(deltaYaw * 0.017453292f) + x *
                MathHelper.sin(deltaYaw * 0.017453292f);

        var movementSideways = Math.round(newX);
        var movementForward = Math.round(newZ);

        return new DirectionalInput(movementForward, movementSideways);
    }

}
