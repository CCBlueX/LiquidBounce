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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.integration.screen.ScreenManager;
import net.ccbluex.liquidbounce.integration.screen.ScreenTweak;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen {

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/LogoRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IF)V"))
    private boolean hookLogo(LogoRenderer logoRenderer, GuiGraphicsExtractor graphics, int width, float alpha) {
        return !LiquidBounce.INSTANCE.isInitialized() || !ScreenManager.isTweakEnabled(ScreenTweak.DISABLE_LOGO);
    }

    @WrapWithCondition(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SplashRenderer;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;ILnet/minecraft/client/gui/Font;F)V"))
    private boolean hookSplash(SplashRenderer splashRenderer, GuiGraphicsExtractor graphics, int width, Font font, float alpha) {
        return !LiquidBounce.INSTANCE.isInitialized() || !ScreenManager.isTweakEnabled(ScreenTweak.DISABLE_SPLASH);
    }

}
