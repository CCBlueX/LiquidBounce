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
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.CommonComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class MixinLevelLoadingScreen extends MixinScreen {

    @Override
    protected void init(CallbackInfo ci) {
        super.init(ci);

        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        Button quitButton = Button.builder(CommonComponents.GUI_PROCEED, button -> this.minecraft.setScreen(null))
            .pos(this.width / 2 - 100, this.height / 2 + 50)
            .size(200, 20)
            .build();

        addRenderableWidget(quitButton);
    }

}
