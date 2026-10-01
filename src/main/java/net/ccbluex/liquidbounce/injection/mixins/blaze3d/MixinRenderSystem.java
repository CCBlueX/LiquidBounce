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
package net.ccbluex.liquidbounce.injection.mixins.blaze3d;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManagerKt;
import net.minecraft.util.TimeSource;
import net.minecraft.util.Util;
import org.lwjgl.sdl.SDLHints;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSystem.class)
public abstract class MixinRenderSystem {

    /**
     * Accelerated paint needs an EGL context, but SDL creates GLX contexts on X11.
     * An SDL_VIDEO_FORCE_EGL environment variable still takes priority over this.
     */
    @Inject(method = "initBackendSystem", at = @At("HEAD"))
    private static void hookForceEgl(CallbackInfoReturnable<TimeSource.NanoTimeSource> cir) {
        if (Util.getPlatform() == Util.OS.LINUX && !BrowserBackendManagerKt.isBrowserSkipped()) {
            SDLHints.SDL_SetHint(SDLHints.SDL_HINT_VIDEO_FORCE_EGL, "1");
        }
    }

}
