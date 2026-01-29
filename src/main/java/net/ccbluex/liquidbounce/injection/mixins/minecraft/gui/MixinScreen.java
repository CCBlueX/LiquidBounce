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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.additions.ScreenAddition;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.integration.theme.ThemeManager;
import net.ccbluex.liquidbounce.utils.text.RunnableClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class MixinScreen implements ScreenAddition {

    @Final
    @Shadow
    protected Font font;
    @Shadow
    public int height;
    @Shadow
    public int width;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable> T addRenderableWidget(T drawableElement);

    @Final
    @Shadow
    @Nullable
    protected Minecraft minecraft;

    @Shadow
    private boolean initialized;

    @Inject(method = "init(II)V", at = @At("TAIL"))
    private void objInit(CallbackInfo ci) {
        if (!LiquidBounce.INSTANCE.isInitialized()) {
            return;
        }

        ThemeManager.INSTANCE.loadBackgroundAsync();
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    protected void init(CallbackInfo ci) {
        if (!LiquidBounce.INSTANCE.isInitialized()) {
            return;
        }

        ThemeManager.INSTANCE.loadBackgroundAsync();
    }

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void hookRenderInGameBackground(GuiGraphics context, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.GUI_BACKGROUND)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"), cancellable = true)
    private void cancelRenderByChestStealer(CallbackInfo ci) {
        if (LiquidBounce.INSTANCE.isInitialized() && FeatureSilentScreen.INSTANCE.getShouldHide()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderBackgroundTexture(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.minecraft != null && this.minecraft.level == null && !HideAppearance.INSTANCE.isHidingNow()) {
            if (!LiquidBounce.INSTANCE.isInitialized()) {
                return;
            }

            if (ThemeManager.INSTANCE.drawBackground(context, width, height, mouseX, mouseY, delta)) {
                ci.cancel();
            }
        }
    }

    /**
     * Allows the execution of {@link RunnableClickEvent}.
     * (default branch in switch pattern matching)
     */
    @Inject(method = "defaultHandleClickEvent", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", ordinal = 0, shift = At.Shift.BEFORE, remap = false), cancellable = true)
    private static void hookExecuteClickEvents(ClickEvent clickEvent, Minecraft client, Screen screenAfterRun, CallbackInfo ci) {
        if (clickEvent instanceof RunnableClickEvent runnableClickEvent) {
            runnableClickEvent.run();
            ci.cancel();
        }
    }

    @Unique
    @Override
    public boolean liquidbounce$screenInitialized() {
        return initialized;
    }
}
