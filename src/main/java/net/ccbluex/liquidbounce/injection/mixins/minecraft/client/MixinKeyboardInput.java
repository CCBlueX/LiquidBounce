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
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.util.Mth.DEG_TO_RAD;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends MixinClientInput {

    @Shadow
    @Final
    private Options options;

    /**
     * Hook inventory move module
     */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;isDown()Z"))
    private boolean hookInventoryMove(KeyMapping instance, Operation<Boolean> original) {
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
        if (ModuleInventoryMove.INSTANCE.shouldHandleInputs(this.options.keySprint)) {
            this.options.keySprint.setDown(InputTracker.INSTANCE.isPressedOnAny(this.options.keySprint));
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input modifyInput(Input original) {
        this.initial = original;

        var event = new MovementInputEvent(new DirectionalInput(original), original.jump(), original.shift());
        EventManager.INSTANCE.callEvent(event);
        var untransformedDirectionalInput = event.getDirectionalInput();
        var directionalInput = transformDirection(untransformedDirectionalInput);

        var sprintEvent = new SprintEvent(directionalInput, original.sprint(), SprintEvent.Source.INPUT);
        EventManager.INSTANCE.callEvent(sprintEvent);

        // Store the untransformed input for later use
        this.untransformed = new Input(
                untransformedDirectionalInput.getForwards(),
                untransformedDirectionalInput.getBackwards(),
                untransformedDirectionalInput.getLeft(),
                untransformedDirectionalInput.getRight(),
                event.getJump(),
                event.getSneak(),
                sprintEvent.getSprint()
        );

        return new Input(
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
        var player = Minecraft.getInstance().player;
        var rotation = RotationManager.INSTANCE.getCurrentRotation();
        var configurable = RotationManager.INSTANCE.getActiveRotationTarget();

        float z = KeyboardInput.calculateImpulse(input.getForwards(), input.getBackwards());
        float x = KeyboardInput.calculateImpulse(input.getLeft(), input.getRight());

        if (configurable == null || configurable.getMovementCorrection() != MovementCorrection.SILENT
                || rotation == null || player == null) {
            return input;
        }

        float deltaYaw = player.getYRot() - rotation.yRot();

        float newX = x * Mth.cos(deltaYaw * DEG_TO_RAD) - z *
                Mth.sin(deltaYaw * DEG_TO_RAD);
        float newZ = z * Mth.cos(deltaYaw * DEG_TO_RAD) + x *
                Mth.sin(deltaYaw * DEG_TO_RAD);

        var movementSideways = Math.round(newX);
        var movementForward = Math.round(newZ);

        return new DirectionalInput(movementForward, movementSideways);
    }

}
