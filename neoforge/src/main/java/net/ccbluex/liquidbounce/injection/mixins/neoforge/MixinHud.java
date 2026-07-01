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
package net.ccbluex.liquidbounce.injection.mixins.neoforge;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager;
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak;
import net.ccbluex.liquidbounce.interfaces.GuiAddition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loader-specific companion of {@code minecraft.gui.MixinHud}.
 * <p>
 * NeoForge splits {@code extractHotbarAndDecorations} into individual HUD
 * layer methods, so each loader anchors these hooks in its own shape of the
 * hotbar and experience extraction.
 */
@Mixin(Hud.class)
public abstract class MixinHud {

    /**
     * Hook render hud event at the top layer.
     * <p>
     * {@code extractHotbar} handles the spectator branch internally, making its
     * head equivalent to the head of the vanilla {@code extractHotbarAndDecorations}.
     */
    @Inject(method = "extractHotbar", at = @At("HEAD"))
    private void hookRenderEventStart(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        ((GuiAddition) this).liquid_bounce$extractOverlay(context, tickCounter);
    }

    @WrapOperation(
        method = "extractExperienceLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;hasExperience()Z")
    )
    private boolean tweakExpLevelText(MultiPlayerGameMode instance, Operation<Boolean> original) {
        if (HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_EXP_BAR)) {
            return false;
        }
        return original.call(instance);
    }

}
