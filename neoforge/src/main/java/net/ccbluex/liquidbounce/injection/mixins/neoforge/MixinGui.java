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

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Loader-specific companion of {@code minecraft.client.MixinGui}.
 * <p>
 * NeoForge patches the screen extraction in {@code extractRenderState} through
 * {@code ClientHooks#extractScreen}, so the screen render event is anchored
 * after that call. The {@code GuiGraphicsExtractor} is captured by type because
 * the recompiled NeoForge jar does not preserve local variable names.
 */
@Mixin(Gui.class)
public abstract class MixinGui {

    /**
     * Hook screen render event
     */
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/neoforged/neoforge/client/ClientHooks;extractScreen("
            + "Lnet/minecraft/client/gui/screens/Screen;Ljava/util/Stack;"
            + "Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
        remap = false,
        shift = At.Shift.AFTER))
    public void hookScreenRender(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded,
        CallbackInfo ci, @Local GuiGraphicsExtractor graphics) {
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
    }

}
