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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.ccbluex.liquidbounce.utils.render.LiquidBounceGuiElementRenderState;
import net.minecraft.client.gui.render.state.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.ccbluex.liquidbounce.utils.client.GenericPools.ARRAY_LIST;

@SuppressWarnings("rawtypes")
@Mixin(GuiRenderState.class)
public abstract class MixinGuiRenderState {

    @Shadow
    @Final
    private List<GuiRenderState.Layer> rootLayers;

    @Inject(method = "clear", at = @At("HEAD"))
    private void clear(CallbackInfo ci) {
        for (GuiRenderState.Layer layer : rootLayers) {
            if (layer.simpleElementRenderStates != null) {
                layer.simpleElementRenderStates.forEach(liquid_bounce$tryRecycleMatrix3x2f);
                ARRAY_LIST.recycle((ArrayList) layer.simpleElementRenderStates);
            }

            if (layer.preparedTextElementRenderStates != null) {
                layer.preparedTextElementRenderStates.forEach(liquid_bounce$tryRecycleMatrix3x2f);
                ARRAY_LIST.recycle((ArrayList) layer.preparedTextElementRenderStates);
            }

            if (layer.itemElementRenderStates != null) {
                ARRAY_LIST.recycle((ArrayList) layer.itemElementRenderStates);
            }

            if (layer.textElementRenderStates != null) {
                ARRAY_LIST.recycle((ArrayList) layer.textElementRenderStates);
            }

            if (layer.specialElementRenderStates != null) {
                ARRAY_LIST.recycle((ArrayList) layer.specialElementRenderStates);
            }
        }
    }

    @Unique
    private static final Consumer<GuiElementRenderState> liquid_bounce$tryRecycleMatrix3x2f = element -> {
        if (element instanceof LiquidBounceGuiElementRenderState t) {
            Pools.Mat3x2f.recycle(t.pose());
        }
    };

}
