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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class MixinDisconnectedScreen extends MixinScreen {

    @Shadow
    @Final
    private static Component TO_TITLE;

    @Shadow
    @Final
    private Component buttonText;

    @Shadow
    @Final
    private Screen parent;

    @Unique
    private Button disconnectButton;

    @Inject(method = "init", at = @At("HEAD"))
    private void injectButtons(final CallbackInfo callback) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        /*
         * Add second quit button in-case the first one is being covered by the multiplayer message
         * This technique is used by many servers or anti-cheats to prevent players from quitting
         * out of the game when they are banned
         */
        int x = this.width - 140;
        int y = this.height - 30;
        disconnectButton = (this.minecraft.allowsMultiplayer() ?
                Button.builder(this.buttonText, button -> this.minecraft.setScreen(this.parent)) :
                Button.builder(TO_TITLE, button -> this.minecraft.setScreen(new TitleScreen()))
        ).bounds(x, y, 120, 20).build();
        addRenderableWidget(disconnectButton);
    }

    @Inject(method = "repositionElements", at = @At("HEAD"))
    private void moveButtons(final CallbackInfo callback) {
        if (disconnectButton != null) {
            // fixes button position
            int x = this.width - 140;
            int y = this.height - 30;
            disconnectButton.setPosition(x, y);
        }
    }

}
