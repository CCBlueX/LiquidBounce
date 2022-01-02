/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.OverlayRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {

    @Final
    @Shadow
    private static Identifier PUMPKIN_BLUR;

    /**
     * Hook render hud event at the top layer
     */
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderStatusEffectOverlay(Lnet/minecraft/client/util/math/MatrixStack;)V", shift = At.Shift.AFTER))
    private void hookRenderEvent(MatrixStack matrices, float tickDelta, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new OverlayRenderEvent(matrices, tickDelta));
    }

    @Inject(method = "renderOverlay", at = @At("HEAD"), cancellable = true)
    private void injectPumpkinBlur(Identifier texture, float opacity, CallbackInfo callback) {
        ModuleAntiBlind module = ModuleAntiBlind.INSTANCE;
        if (module.getEnabled() && module.getPumpkinBlur() && PUMPKIN_BLUR.equals(texture)) {
            callback.cancel();
        }
    }
}
