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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.KeybindChangeEvent;
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class MixinKeyMapping {

    @Inject(method = "setKey", at = @At("RETURN"))
    private void hookSetBoundKey(InputConstants.Key boundKey, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(KeybindChangeEvent.INSTANCE);
    }

    @ModifyReturnValue(method = "isDown", at = @At("RETURN"))
    private boolean isPressed(boolean original) {
        return EventManager.INSTANCE.callEvent(new KeybindIsPressedEvent((KeyMapping) (Object) this, original)).isPressed();
    }

}
