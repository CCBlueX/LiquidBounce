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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.integration.theme.component.SubtitleOverlayCapture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Captures the values passed to the vanilla subtitle renderer without reproducing its state calculations.
 *
 * @see net.minecraft.client.gui.components.SubtitleOverlay#extractRenderState(GuiGraphicsExtractor)
 */
@Mixin(SubtitleOverlay.class)
public abstract class MixinSubtitleOverlay {

    @WrapMethod(method = "extractRenderState")
    private void captureFrame(GuiGraphicsExtractor graphics, Operation<Void> original) {
        SubtitleOverlayCapture.begin();
        try {
            original.call(graphics);
            SubtitleOverlayCapture.publish();
        } finally {
            SubtitleOverlayCapture.end();
        }
    }

    @WrapOperation(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
        )
    )
    private void captureBackground(
        GuiGraphicsExtractor graphics,
        int x0,
        int y0,
        int x1,
        int y1,
        int color,
        Operation<Void> original
    ) {
        SubtitleOverlayCapture.captureBackground(color);
        if (SubtitleOverlayCapture.shouldRenderVanilla()) {
            original.call(graphics, x0, y0, x1, y1, color);
        }
    }

    @WrapOperation(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"
        )
    )
    private void captureDirection(
        GuiGraphicsExtractor graphics,
        Font font,
        String text,
        int x,
        int y,
        int color,
        Operation<Void> original
    ) {
        SubtitleOverlayCapture.captureDirection(text);
        if (SubtitleOverlayCapture.shouldRenderVanilla()) {
            original.call(graphics, font, text, x, y, color);
        }
    }

    @WrapOperation(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
        )
    )
    private void captureSubtitle(
        GuiGraphicsExtractor graphics,
        Font font,
        Component text,
        int x,
        int y,
        int color,
        Operation<Void> original
    ) {
        SubtitleOverlayCapture.captureSubtitle(text, color);
        if (SubtitleOverlayCapture.shouldRenderVanilla()) {
            original.call(graphics, font, text, x, y, color);
        }
    }

}
