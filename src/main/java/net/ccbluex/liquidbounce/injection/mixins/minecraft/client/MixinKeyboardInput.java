/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {

    /**
     * Hook inventory move module
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;isPressed()Z"))
    private boolean hookInventoryMove(KeyBinding keyBinding) {
        return ModuleInventoryMove.INSTANCE.shouldHandleInputs(keyBinding) ?
                InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(),
                                       keyBinding.boundKey.getCode()) :
                keyBinding.isPressed();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void injectStrafing(boolean slowDown, CallbackInfo ci) {
        if (RotationManager.INSTANCE.getActiveConfigurable() == null || !RotationManager.INSTANCE.getActiveConfigurable().getFixVelocity())
            return;

        Rotation currentRotation = RotationManager.INSTANCE.getCurrentRotation();

        if (currentRotation == null)
            return;

        currentRotation = currentRotation.fixedSensitivity();

        float deltaYaw = MinecraftClient.getInstance().player.getYaw() - currentRotation.getYaw();

        float x = this.movementSideways;
        float z = this.movementForward;

        float newX = x * MathHelper.cos(deltaYaw * 0.017453292f) - z * MathHelper.sin(deltaYaw * 0.017453292f);
        float newZ = z * MathHelper.cos(deltaYaw * 0.017453292f) + x * MathHelper.sin(deltaYaw * 0.017453292f);

        this.movementSideways = Math.round(newX);
        this.movementForward = Math.round(newZ);

        if (slowDown) {
            this.movementSideways *= 0.3F;
            this.movementForward *= 0.3F;
        }
    }

}
