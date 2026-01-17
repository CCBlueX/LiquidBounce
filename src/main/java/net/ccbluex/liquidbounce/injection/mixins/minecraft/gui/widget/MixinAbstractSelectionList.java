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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.widget;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractSelectionList.class)
public abstract class MixinAbstractSelectionList {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @WrapWithCondition(method = "renderWidget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/AbstractSelectionList;renderListSeparators(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private boolean renderBackground(AbstractSelectionList instance, GuiGraphics context) {
        return this.minecraft.level != null || HideAppearance.INSTANCE.isHidingNow();
    }

}
