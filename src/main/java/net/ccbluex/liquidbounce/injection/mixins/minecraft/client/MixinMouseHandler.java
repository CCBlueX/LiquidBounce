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
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.InputConstants;
import net.ccbluex.liquidbounce.additions.MouseHandlerAddition;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleZoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MixinMouseHandler implements MouseHandlerAddition {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Unique
    @Override
    public void liquidbounce$setPosition(double x, double y) {
        this.xpos = x;
        this.ypos = y;
        InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), InputConstants.CURSOR_NORMAL, this.xpos, this.ypos);
    }

    /**
     * Hook mouse button event
     */
    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        final var button = input.button();
        EventManager.INSTANCE.callEvent(new MouseButtonEvent(
                InputConstants.Type.MOUSE.getOrCreate(button),
                button,
                action,
                input.modifiers(),
                this.minecraft.screen
        ));
    }

    /**
     * Hook mouse scroll event
     */
    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookMouseScroll(long window, double horizontal, double vertical, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new MouseScrollEvent(horizontal, vertical));
    }

    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z", shift = At.Shift.BEFORE), cancellable = true)
    private void hookMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci, @Local(ordinal = 0) int i) {
        if (EventManager.INSTANCE.callEvent(new MouseScrollInHotbarEvent(i)).isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * Hook mouse cursor event
     */
    @Inject(method = "onMove", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isWindowActive()Z", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookCursorPos(long window, double x, double y, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new MouseCursorEvent(x, y));
    }

    @ModifyExpressionValue(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/CameraType;isFirstPerson()Z"))
    private boolean injectZoomCondition1(boolean original) {
        return original || ModuleZoom.INSTANCE.getRunning();
    }

    @ModifyExpressionValue(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isScoping()Z"))
    private boolean injectZoomCondition2(boolean original) {
        return original || ModuleZoom.INSTANCE.getRunning();
    }

    @WrapWithCondition(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"), require = 1, allow = 1)
    private boolean modifyMouseRotationInput(LocalPlayer instance, double cursorDeltaX, double cursorDeltaY) {
        final MouseRotationEvent event = new MouseRotationEvent(cursorDeltaX, cursorDeltaY);
        EventManager.INSTANCE.callEvent(event);

        if (!event.isCancelled()) {
            instance.turn(event.getCursorDeltaX(), event.getCursorDeltaY());
        }

        return false;
    }

}
