/*
 *
 *  * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *  *
 *  * Copyright (c) 2015 - 2025 CCBlueX
 *  *
 *  * LiquidBounce is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * LiquidBounce is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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
    private static Text TO_TITLE_TEXT;

    @Shadow
    @Final
    private Text buttonLabel;

    @Shadow
    @Final
    private Screen parent;

    @Unique
    private ButtonWidget disconnectButton;

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
        disconnectButton = (this.client.isMultiplayerEnabled() ?
                ButtonWidget.builder(this.buttonLabel, button -> this.client.setScreen(this.parent)) :
                ButtonWidget.builder(TO_TITLE_TEXT, button -> this.client.setScreen(new TitleScreen()))
        ).dimensions(x, y, 120, 20).build();
        addDrawableChild(disconnectButton);
    }

    @Inject(method = "refreshWidgetPositions", at = @At("HEAD"))
    private void moveButtons(final CallbackInfo callback) {
        if (disconnectButton != null) {
            // fixes button position
            int x = this.width - 140;
            int y = this.height - 30;
            disconnectButton.setPosition(x, y);
        }
    }

}
