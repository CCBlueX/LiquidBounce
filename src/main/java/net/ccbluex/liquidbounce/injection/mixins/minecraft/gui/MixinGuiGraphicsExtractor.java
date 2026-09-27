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

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.additions.GuiGraphicsExtractorAddition;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleBetterInventory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractor implements GuiGraphicsExtractorAddition {

    @Shadow
    protected abstract void itemBar(ItemStack stack, int x, int y);

    @Shadow
    protected abstract void itemCount(Font textRenderer, ItemStack stack, int x, int y,
        @Nullable String stackCountText);

    @Shadow
    protected abstract void itemCooldown(ItemStack stack, int x, int y);

    @Inject(method = "itemCooldown", at = @At("TAIL"))
    private void drawCooldownProgress(ItemStack stack, int x, int y, CallbackInfo ci) {
        ModuleBetterInventory.INSTANCE.drawTextCooldownProgress((GuiGraphicsExtractor) (Object) this, stack, x, y);
    }

    @Override
    public void liquidbounce$drawItemBar(ItemStack stack, int x, int y) {
        itemBar(stack, x, y);
    }

    @Override
    public void liquidbounce$drawStackCount(Font textRenderer, ItemStack stack, int x, int y,
            @Nullable String stackCountText) {
        itemCount(textRenderer, stack, x, y, stackCountText);
    }

    @Override
    public void liquidbounce$drawCooldownProgress(ItemStack stack, int x, int y) {
        itemCooldown(stack, x, y);
    }

    /**
     * Early reject of ZERO-size Scissor area.
     *
     * @see com.mojang.renderpearl.frontend.FrontendRenderPass#enableScissor(int, int, int, int)
     */
    @Inject(
        method = "enableScissor",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor$ScissorStack;push(Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V")
    )
    private void earlyCrash(int x0, int y0, int x1, int y1, CallbackInfo ci, @Local(name = "rectangle") ScreenRectangle rectangle) {
        if (rectangle.width() == 0 || rectangle.height() == 0) {
            throw new IllegalArgumentException("scissor area should have positive width and height, actual: " + rectangle);
        }
    }

}
