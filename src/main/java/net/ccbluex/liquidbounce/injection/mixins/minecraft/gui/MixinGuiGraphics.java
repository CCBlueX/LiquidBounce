/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2023 JustAlittleWolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.additions.GuiGraphicsAddition;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterInventory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class MixinGuiGraphics implements GuiGraphicsAddition {

    @Shadow
    protected abstract void renderItemBar(ItemStack stack, int x, int y);

    @Shadow
    protected abstract void renderItemCount(Font textRenderer, ItemStack stack, int x, int y,
        @Nullable String stackCountText);

    @Shadow
    protected abstract void renderItemCooldown(ItemStack stack, int x, int y);

    @Inject(method = "renderItemCooldown", at = @At("TAIL"))
    private void drawCooldownProgress(ItemStack stack, int x, int y, CallbackInfo ci) {
        ModuleBetterInventory.INSTANCE.drawTextCooldownProgress((GuiGraphics) (Object) this, stack, x, y);
    }

    @Override
    public void liquidbounce$drawItemBar(@NotNull ItemStack stack, int x, int y) {
        renderItemBar(stack, x, y);
    }

    @Override
    public void liquidbounce$drawStackCount(@NotNull Font textRenderer, @NotNull ItemStack stack, int x, int y,
            @Nullable String stackCountText) {
        renderItemCount(textRenderer, stack, x, y, stackCountText);
    }

    @Override
    public void liquidbounce$drawCooldownProgress(@NotNull ItemStack stack, int x, int y) {
        renderItemCooldown(stack, x, y);
    }
}
