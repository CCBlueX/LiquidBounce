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
package net.ccbluex.liquidbounce.injection.mixins.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loader-specific companion of {@code minecraft.render.MixinGameRenderer}.
 * <p>
 * NeoForge patches the screen extraction in {@code extractGui} into a
 * {@code ClientHooks.drawScreen} call, so each loader anchors the screen
 * render event after its own shape of the call.
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    /**
     * Hook screen render event
     */
    @Inject(method = "extractGui", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            shift = At.Shift.AFTER))
    public void hookScreenRender(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded,
        CallbackInfo ci, @Local(name = "graphics") GuiGraphicsExtractor graphics) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
    }

}
