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

package net.ccbluex.liquidbounce.injection.mixins.cef;

import net.minecraft.client.input.InputQuirks;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@NullMarked
@Pseudo
@Mixin(targets = "org.cef.CefClient", remap = false)
public abstract class MixinCefClient {

    /**
     * CEF sends unhandled synthetic key events to the macOS menu bar, where plain F can match
     * the native fullscreen shortcut. Minecraft already handles window shortcuts from the
     * original input. Consume the browser fallback after the renderer has processed the event
     * so text input and page shortcuts still work.
     */
    @Inject(method = "onKeyEvent", at = @At("RETURN"), cancellable = true)
    private void preventNativeShortcuts(CallbackInfoReturnable<Boolean> cir) {
        if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            cir.setReturnValue(true);
        }
    }

}
