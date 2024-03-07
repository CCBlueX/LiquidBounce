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
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent;
import net.ccbluex.liquidbounce.event.events.MouseCursorEvent;
import net.ccbluex.liquidbounce.event.events.MouseRotationEvent;
import net.ccbluex.liquidbounce.event.events.MouseScrollEvent;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    /**
     * Hook mouse button event
     */
    @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookMouseButton(long window, int button, int action, int mods, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new MouseButtonEvent(button, action, mods));
    }

    /**
     * Hook mouse scroll event
     */
    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookMouseScroll(long window, double horizontal, double vertical, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new MouseScrollEvent(horizontal, vertical));
    }

    /**
     * Hook mouse cursor event
     */
    @Inject(method = "onCursorPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isWindowFocused()Z", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookCursorPos(long window, double x, double y, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new MouseCursorEvent(x, y));
    }

    /**
     * Hook mouse cursor event
     */
    @Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 1, allow = 1)
    private void hookUpdateMouse(ClientPlayerEntity entity, double cursorDeltaX, double cursorDeltaY) {
        final MouseRotationEvent event = new MouseRotationEvent(cursorDeltaX, cursorDeltaY);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled())
            return;

        entity.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
    }

}
