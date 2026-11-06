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

import com.mojang.blaze3d.platform.SDLEventHandler;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.utils.input.WindowsShiftCompatibility;
import org.lwjgl.sdl.SDL_Event;
import org.lwjgl.system.Platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TODO(26.4): remove this after 26.4-snapshot-2
 */
// Recover the raw scan code before Minecraft copies the native event into a KeyEvent.
@Mixin(SDLEventHandler.class)
public abstract class MixinSDLEventHandler {
    @Inject(method = "handleKeyEvent", at = @At("HEAD"))
    private void repairRightShift(SDL_Event event, CallbackInfo ci) {
        if (Platform.get() == Platform.WINDOWS) {
            WindowsShiftCompatibility.repair(event.key());
        }
    }

    @Inject(method = "pollEvents", at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/platform/Window;handleEvent(Lorg/lwjgl/sdl/SDL_Event;)V"))
    private void clearShiftOnFocusLoss(CallbackInfo ci, @Local SDL_Event event) {
        WindowsShiftCompatibility.windowEvent(event);
    }
}
