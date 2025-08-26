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
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleZoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MixinMouse {

    @Shadow
    @Final
    private MinecraftClient client;

    /**
     * Hook mouse button event
     */
    @Inject(method = "onMouseButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookMouseButton(long window, int button, int action, int mods, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new MouseButtonEvent(
                InputUtil.Type.MOUSE.createFromCode(button),
                button,
                action,
                mods,
                this.client.currentScreen
        ));
    }

    /**
     * Hook mouse scroll event
     */
    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getOverlay()Lnet/minecraft/client/gui/screen/Overlay;", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookMouseScroll(long window, double horizontal, double vertical, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new MouseScrollEvent(horizontal, vertical));
    }

    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isSpectator()Z", shift = At.Shift.BEFORE), cancellable = true)
    private void hookMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci, @Local(ordinal = 0) int i) {
        if (EventManager.INSTANCE.callEvent(new MouseScrollInHotbarEvent(i)).isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * Hook mouse cursor event
     */
    @Inject(method = "onCursorPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isWindowFocused()Z", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookCursorPos(long window, double x, double y, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new MouseCursorEvent(x, y));
    }

    @ModifyExpressionValue(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/Perspective;isFirstPerson()Z"))
    private boolean injectZoomCondition1(boolean original) {
        return original || ModuleZoom.INSTANCE.getRunning();
    }

    @ModifyExpressionValue(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingSpyglass()Z"))
    private boolean injectZoomCondition2(boolean original) {
        return original || ModuleZoom.INSTANCE.getRunning();
    }

    @WrapWithCondition(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 1, allow = 1)
    private boolean modifyMouseRotationInput(ClientPlayerEntity instance, double cursorDeltaX, double cursorDeltaY) {
        final MouseRotationEvent event = new MouseRotationEvent(cursorDeltaX, cursorDeltaY);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        instance.changeLookDirection(event.getCursorDeltaX(), event.getCursorDeltaY());
        return false;
    }

}
