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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.gl;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gl.RenderPassImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static net.ccbluex.liquidbounce.utils.client.GenericPools.HASH_MAP;
import static net.ccbluex.liquidbounce.utils.client.GenericPools.HASH_SET;

/**
 * Purpose: reusing objects for less GC.
 * In Minecraft's design, we will have only one existing RenderPass instance.
 * So the collections in it can be safely reused.
 *
 * @author MukjepScarlet
 */
@SuppressWarnings("rawtypes")
@Mixin(RenderPassImpl.class)
public abstract class MixinRenderPassImpl {

    @Shadow
    @Final
    protected HashMap<String, Object> simpleUniforms;

    @Shadow
    @Final
    protected HashMap<String, GpuTexture> samplerUniforms;

    @Shadow
    @Final
    protected Set<String> setSimpleUniforms;

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "()Ljava/util/HashMap;"))
    private HashMap reuseHashMap(Operation<HashMap> original) {
        return HASH_MAP.borrow();
    }

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "()Ljava/util/HashSet;"))
    private HashSet reuseSet(Operation<HashSet> original) {
        return HASH_SET.borrow();
    }

    @Inject(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/GlCommandEncoder;closePass()V"))
    private void recycleStuffs(CallbackInfo ci) {
        HASH_MAP.recycle(this.simpleUniforms);
        HASH_MAP.recycle(this.samplerUniforms);
        HASH_SET.recycle((HashSet) this.setSimpleUniforms);
    }


}
