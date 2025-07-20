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
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.KeybindChangeEvent;
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent;
import net.ccbluex.liquidbounce.utils.client.VanillaTranslationRecognizer;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public class MixinKeyBinding {

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/util/InputUtil$Type;ILjava/lang/String;)V", at = @At("RETURN"), require = 1)
    private void injectVanillaKeybindRegistering(String translationKey, InputUtil.Type type, int code, String category, CallbackInfo ci) {
        VanillaTranslationRecognizer.INSTANCE.registerKey(translationKey);
    }

    @Inject(method = "setBoundKey", at = @At("RETURN"))
    private void hookSetBoundKey(InputUtil.Key boundKey, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(KeybindChangeEvent.INSTANCE);
    }

    @ModifyReturnValue(method = "isPressed", at = @At("RETURN"))
    private boolean isPressed(boolean original) {
        return EventManager.INSTANCE.callEvent(new KeybindIsPressedEvent((KeyBinding) (Object) this, original)).isPressed();
    }

}
