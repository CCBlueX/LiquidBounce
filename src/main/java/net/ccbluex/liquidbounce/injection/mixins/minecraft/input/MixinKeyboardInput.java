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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.input;

import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleInventoryMove;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.options.GameOptions;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends Input {
    @Shadow
    @Final
    private GameOptions settings;

    @Override
    @Overwrite
    public void tick(boolean slowDown) {
        if (ModuleInventoryMove.INSTANCE.getEnabled() && (ModuleInventoryMove.INSTANCE.getChat() || !(MinecraftClient.getInstance().currentScreen instanceof ChatScreen))){
            this.pressingForward = (isKeyDown(this.settings.keyForward.getDefaultKey().getCode()) || this.settings.keyForward.isPressed());
            this.pressingBack = (isKeyDown(this.settings.keyBack.getDefaultKey().getCode()) || this.settings.keyBack.isPressed());
            this.pressingLeft = (isKeyDown(this.settings.keyLeft.getDefaultKey().getCode()) || this.settings.keyLeft.isPressed());
            this.pressingRight = (isKeyDown(this.settings.keyRight.getDefaultKey().getCode()) || this.settings.keyRight.isPressed());
            this.movementForward = this.pressingForward == this.pressingBack ? 0.0F : (this.pressingForward ? 1.0F : -1.0F);
            this.movementSideways = this.pressingLeft == this.pressingRight ? 0.0F : (this.pressingLeft ? 1.0F : -1.0F);
            this.jumping = (ModuleInventoryMove.INSTANCE.getJump() ? (isKeyDown(this.settings.keyJump.getDefaultKey().getCode()) || this.settings.keyJump.isPressed()) : this.settings.keyJump.isPressed());
            this.sneaking = (ModuleInventoryMove.INSTANCE.getSneak() ? (isKeyDown(this.settings.keySneak.getDefaultKey().getCode()) || this.settings.keySneak.isPressed()) : this.settings.keySneak.isPressed());
            if (slowDown) {
                this.movementSideways = (float)((double)this.movementSideways * 0.3);
                this.movementForward = (float)((double)this.movementForward * 0.3);
            }
        } else {
            this.pressingForward = this.settings.keyForward.isPressed();
            this.pressingBack = this.settings.keyBack.isPressed();
            this.pressingLeft = this.settings.keyLeft.isPressed();
            this.pressingRight = this.settings.keyRight.isPressed();
            this.movementForward = this.pressingForward == this.pressingBack ? 0.0F : (this.pressingForward ? 1.0F : -1.0F);
            this.movementSideways = this.pressingLeft == this.pressingRight ? 0.0F : (this.pressingLeft ? 1.0F : -1.0F);
            this.jumping = this.settings.keyJump.isPressed();
            this.sneaking = this.settings.keySneak.isPressed();
            if (slowDown) {
                this.movementSideways = (float)((double)this.movementSideways * 0.3);
                this.movementForward = (float)((double)this.movementForward * 0.3);
            }
        }
    }

    public boolean isKeyDown(int key){
        return GLFW.glfwGetKey(MinecraftClient.getInstance().getWindow().getHandle(), key) == 1;
    }
}
