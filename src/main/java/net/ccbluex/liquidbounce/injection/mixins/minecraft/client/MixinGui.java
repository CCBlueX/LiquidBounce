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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenEvent;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    /**
     * Fixes recursive screen opening,
     * this is usually caused by another mod such as Lunar Client.
     * Can also happen when opening a screen during [ScreenEvent].
     */
    @Unique
    private static final ScopedValue<Void> RECURSIVE_SCREEN_OPENING = ScopedValue.newInstance();

    /**
     * Handle opening screens
     *
     * @param screen       to be opened (null = no screen at all)
     * @param callbackInfo callback
     */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void hookScreen(@Nullable Screen screen, CallbackInfo callbackInfo) {
        if (RECURSIVE_SCREEN_OPENING.isBound()) {
            return;
        }

        var event = ScopedValue.where(RECURSIVE_SCREEN_OPENING, null)
            .call(() -> EventManager.INSTANCE.callEvent(new ScreenEvent(screen)));

        if (event.isCancelled()) {
            callbackInfo.cancel();
        }

        // Who need this GUI?
        if (screen instanceof AccessibilityOnboardingScreen) {
            callbackInfo.cancel();
            this.setScreen(new TitleScreen(true));
        }
    }

    @WrapWithCondition(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;releaseMouse()V"))
    private boolean cancelScreenMouseForChestStealer(MouseHandler instance) {
        // Allows rotation.
        return !LiquidBounce.INSTANCE.isInitialized() ||
            !FeatureSilentScreen.INSTANCE.getShouldHide() || FeatureSilentScreen.INSTANCE.getUnlockCursor();
    }

    /**
     * Hook screen render event
     */
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
        shift = At.Shift.AFTER))
    public void hookScreenRender(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded,
        CallbackInfo ci, @Local(name = "graphics") GuiGraphicsExtractor graphics) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
    }


}
