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
