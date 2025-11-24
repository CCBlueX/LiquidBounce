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

package net.ccbluex.liquidbounce.injection.mixins.blaze3d;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.ccbluex.liquidbounce.common.GlobalFramebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GlStateManager.class, remap = false)
public abstract class MixinGlStateManager {

    @Redirect(method = "_glBindFramebuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glBindFramebuffer(II)V", ordinal = 0))
    private static void hookBindRead(int target, int framebuffer) {
        GlobalFramebuffer.updateRead(framebuffer);
    }

    @Redirect(method = "_glBindFramebuffer", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glBindFramebuffer(II)V", ordinal = 1))
    private static void hookBindWrite(int target, int framebuffer) {
        GlobalFramebuffer.updateWrite(framebuffer);
    }

    @Inject(method = "getFrameBuffer", at = @At("HEAD"), cancellable = true)
    private static void hookBindWrite(int target, CallbackInfoReturnable<Integer> cir) {
        if (target == GlConst.GL_READ_FRAMEBUFFER) {
            cir.setReturnValue(GlobalFramebuffer.getRead());
        } else {
            if (target == GlConst.GL_DRAW_FRAMEBUFFER) {
                cir.setReturnValue(GlobalFramebuffer.getWrite());
            }

            cir.setReturnValue(0);
        }
    }

}
