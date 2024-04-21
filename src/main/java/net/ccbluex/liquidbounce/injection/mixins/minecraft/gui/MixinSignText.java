/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.utils.client.SignTranslationFixKt;
import net.minecraft.block.entity.SignText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignText.class)
public class MixinSignText {


    @Shadow
    @Final
    private Text[] messages;

    /**
     * Fixes a vulnerability where the server can find out about the installed mods via translated text.
     * <p>
     * This is not the same fix as by https://modrinth.com/mod/moddetectionpreventer/ even if it fixes the same issue.
     * In order to prevent further issues, we completely disallow creating a sign text with unknown translatable.
     */
    @Inject(method = "<init>([Lnet/minecraft/text/Text;[Lnet/minecraft/text/Text;Lnet/minecraft/util/DyeColor;Z)V", at = @At("RETURN"))
    private void injectSignVulnerabilityFix(CallbackInfo ci) {
        for (int i = 0; i < this.messages.length; i++) {
            var msg = this.messages[i];

            if (msg == null) {
                continue;
            }

            this.messages[i] = SignTranslationFixKt.filterNonVanillaText(msg);
        }
    }

}
