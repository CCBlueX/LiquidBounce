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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.client.render.MapRenderState;
import net.minecraft.client.render.MapRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapDecoration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(MapRenderer.class)
public class MixinMapRenderer {
    @ModifyExpressionValue(
            method = "draw",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/MapRenderState;decorations:Ljava/util/List;")
    ) private List<MapDecoration> hookMapMarkers(List<MapDecoration> original) {
        return ModuleAntiBlind.canRender(DoRender.MAP_MARKERS) ? original : List.of();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void hookMapContents(MapRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, boolean bl, int light, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.MAP_CONTENTS)) {
            ci.cancel();
        }
    }
}
