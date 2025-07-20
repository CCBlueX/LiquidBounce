/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.minecraft.client.util.NarratorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The minecraft narrator is so annoying and always gets enabled it accidentally by pressing the keys.
 */
@Mixin(NarratorManager.class)
public class MixinNarratorManager {

    /**
     * This turns off the narrator
     */
    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void notActive(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    /**
     * This removes the narrator notifications
     */
    @Inject(method = {"narrate(Lnet/minecraft/text/Text;)V", "narrate(Lnet/minecraft/text/Text;)V", "narrate(Lnet/minecraft/text/Text;)V", "narrateChatMessage"}, at = @At("HEAD"), cancellable = true)
    private void cancelToast(CallbackInfo callback) {
        callback.cancel();
    }

}
