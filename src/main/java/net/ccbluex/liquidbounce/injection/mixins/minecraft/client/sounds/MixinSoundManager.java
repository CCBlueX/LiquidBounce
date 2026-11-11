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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.sounds;

import net.ccbluex.liquidbounce.features.session.SessionContext;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Only the active session is heard.
 */
@Mixin(SoundManager.class)
public abstract class MixinSoundManager {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void muteBackgroundSession(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (SessionContext.isBackground()) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method = {
        "playDelayed",
        "queueTickingSound",
        "stop()V",
        "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V",
        "stop(Lnet/minecraft/resources/Identifier;Lnet/minecraft/sounds/SoundSource;)V"
    }, at = @At("HEAD"), cancellable = true)
    private void muteBackgroundSession(CallbackInfo ci) {
        if (SessionContext.isBackground()) {
            ci.cancel();
        }
    }

}
