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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.integration.theme.ThemeManager;
import net.ccbluex.liquidbounce.utils.client.RunnableClickEvent;
import net.ccbluex.liquidbounce.utils.math.Vec2i;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Screen.class)
public abstract class MixinScreen {
    @Shadow
    protected abstract void remove(Element child);

    @Shadow
    protected TextRenderer textRenderer;
    @Shadow
    public int height;
    @Shadow
    public int width;

    @Shadow
    protected abstract <T extends Element & Drawable> T addDrawableChild(T drawableElement);

    @Shadow
    @Nullable
    protected MinecraftClient client;

    @Inject(method = "init(Lnet/minecraft/client/MinecraftClient;II)V", at = @At("TAIL"))
    private void objInit(CallbackInfo ci) {
        ThemeManager.INSTANCE.initializeBackground();
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    protected void init(CallbackInfo ci) {
        ThemeManager.INSTANCE.initializeBackground();
    }

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
    private void hookRenderInGameBackground(DrawContext context, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.GUI_BACKGROUND)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderBackgroundTexture(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.client != null && this.client.world == null && !HideAppearance.INSTANCE.isHidingNow()) {
            if (ThemeManager.INSTANCE.drawBackground(context, width, height, new Vec2i(mouseX, mouseY), delta)) {
                ci.cancel();
            }
        }
    }

    /**
     * Allows the execution of {@link RunnableClickEvent}.
     */
    @Inject(method = "handleTextClick", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", ordinal = 2, shift = At.Shift.BEFORE, remap = false), cancellable = true)
    private void hookExecuteClickEvents(Style style, CallbackInfoReturnable<Boolean> cir, @Local ClickEvent clickEvent) {
        if (clickEvent instanceof RunnableClickEvent runnableClickEvent) {
            runnableClickEvent.run();
            cir.setReturnValue(true);
        }
    }

}
