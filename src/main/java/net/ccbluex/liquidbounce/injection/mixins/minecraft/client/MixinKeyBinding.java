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
import net.ccbluex.liquidbounce.event.events.KeyBindingCPSEvent;
import net.ccbluex.liquidbounce.event.events.KeybindChangeEvent;
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent;
import net.ccbluex.liquidbounce.interfaces.KeyBindingAdditions;
import net.ccbluex.liquidbounce.utils.client.VanillaTranslationRecognizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBinding.class)
public abstract class MixinKeyBinding implements KeyBindingAdditions {

    @Shadow
    public InputUtil.Key boundKey;

    /**
     * Records clicks in latest 20 ticks (1 sec)
     */
    @Unique
    private final int[] liquidbounce$countByTick = new int[20];

    /**
     * Flag to track if count was incremented in onKeyPressed
     */
    @Unique
    private boolean liquidbounce$countedInOnKeyPressed = false;

    @Override
    @Unique
    public void liquidbounce$incrementCurrentCount() {
        liquidbounce$currentCount++;
        liquidbounce$countedInOnKeyPressed = true;
    }

    /**
     * Sum of {@link #liquidbounce$countByTick}
     */
    @Unique
    private int liquidbounce$cps = 0;

    @Unique
    private int liquidbounce$tickIndex = 0;

    @Unique
    private int liquidbounce$currentCount = 0;

    @Unique
    private boolean liquidbounce$wasPressed = false;

    @Override
    public void liquidbounce$triggerTickEnd() {
        liquidbounce$cps -= liquidbounce$countByTick[liquidbounce$tickIndex];
        liquidbounce$countByTick[liquidbounce$tickIndex] = liquidbounce$currentCount;
        liquidbounce$cps += liquidbounce$currentCount;
        liquidbounce$currentCount = 0;
        liquidbounce$tickIndex = (liquidbounce$tickIndex + 1) % liquidbounce$countByTick.length;
        EventManager.INSTANCE.callEvent(new KeyBindingCPSEvent(this.boundKey, liquidbounce$cps));
    }

    @Override
    public int liquidbounce$getCps() {
        return liquidbounce$cps;
    }

    @Inject(method = "onKeyPressed", at = @At("HEAD"))
    private static void onKeyPressedCPSInject(InputUtil.Key key, CallbackInfo ci) {
        for (KeyBinding kb : MinecraftClient.getInstance().options.allKeys) {
            if (kb.boundKey.getCode() == key.getCode() && kb instanceof KeyBindingAdditions) {
                ((KeyBindingAdditions) kb).liquidbounce$incrementCurrentCount();
            }
        }
    }

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
        boolean pressed = EventManager.INSTANCE.callEvent(new KeybindIsPressedEvent((KeyBinding) (Object) this, original)).isPressed();
        if (!liquidbounce$wasPressed && pressed && !liquidbounce$countedInOnKeyPressed) {
            liquidbounce$currentCount++;
        }
        liquidbounce$wasPressed = pressed;
        liquidbounce$countedInOnKeyPressed = false; // Reset flag after each check
        return pressed;
    }
}
