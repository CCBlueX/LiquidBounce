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
package net.ccbluex.liquidbounce.injection.mixins.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.model.light.LightMode;
import net.caffeinemc.mods.sodium.client.model.light.data.QuadLightData;
import net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.caffeinemc.mods.sodium.client.render.model.SodiumShadeMode;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(AbstractBlockRenderContext.class)
public abstract class MixinSodiumAbstractBlockRenderContext {

    @Shadow
    protected BlockState state;

    @Shadow
    protected BlockPos pos;

    @Shadow
    protected BlockAndTintGetter level;

    @Shadow
    @Final
    protected QuadLightData quadLightData;

    @Unique
    private static final int FULL_BRIGHT_LIGHTMAP = 0x00F000F0;

    @Inject(method = "shouldDrawSide", at = @At("HEAD"), cancellable = true)
    private void injectXRayForceWhitelistedFace(Direction facing, CallbackInfoReturnable<Boolean> cir) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning() || this.state == null || this.pos == null) {
            return;
        }

        if (module.shouldRender(this.state, this.pos)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyReturnValue(method = "shouldDrawSide", at = @At("RETURN"))
    private boolean injectXRayDrawSide(boolean original, Direction facing) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning() || this.state == null || this.pos == null || this.level == null) {
            return original;
        }

        return module.modifyDrawSide(this.state, this.level, this.pos, facing, original);
    }

    @Inject(method = "shadeQuad", at = @At("RETURN"))
    private void injectXRayFullBright(MutableQuadViewImpl quad, LightMode lightMode, boolean emissive,
            SodiumShadeMode shadeMode, CallbackInfo ci) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning() || !module.getFullBright() || this.state == null || this.pos == null) {
            return;
        }

        if (!module.shouldRender(this.state, this.pos)) {
            return;
        }

        float[] brightnesses = this.quadLightData.br;
        for (int i = 0; i < 4; i++) {
            quad.setLight(i, FULL_BRIGHT_LIGHTMAP);
            brightnesses[i] = 1.0F;
        }
    }

}
