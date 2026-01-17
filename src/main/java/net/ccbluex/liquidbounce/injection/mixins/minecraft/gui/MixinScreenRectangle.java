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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScreenRectangle.class)
public abstract class MixinScreenRectangle {

    @WrapOperation(
        method = "transformAxisAligned",
        at = @At(value = "NEW", target = "()Lorg/joml/Vector2f;", remap = false)
    )
    private Vector2f reuseVec_transform(Operation<Vector2f> original) {
        return Pools.Vec2f.borrow();
    }

    @WrapOperation(
        method = "transformMaxBounds",
        at = @At(value = "NEW", target = "()Lorg/joml/Vector2f;", remap = false)
    )
    private Vector2f reuseVec_transformEachVertex(Operation<Vector2f> original) {
        return Pools.Vec2f.borrow();
    }

    @Inject(
        method = "transformAxisAligned",
        at = @At("RETURN")
    )
    private void recycleVec_transform(
        Matrix3x2fc matrix, CallbackInfoReturnable<ScreenRectangle> cir, @Local(ordinal = 0) Vector2f v0, @Local(ordinal = 1) Vector2f v1
    ) {
        Pools.Vec2f.recycle(v0);
        Pools.Vec2f.recycle(v1);
    }

    @Inject(
        method = "transformMaxBounds",
        at = @At("RETURN")
    )
    private void recycleVec_transformEachVertex(
        Matrix3x2fc matrix, CallbackInfoReturnable<ScreenRectangle> cir, @Local(ordinal = 0) Vector2f v0,
        @Local(ordinal = 1) Vector2f v1, @Local(ordinal = 2) Vector2f v2, @Local(ordinal = 3) Vector2f v3
    ) {
        Pools.Vec2f.recycle(v0);
        Pools.Vec2f.recycle(v1);
        Pools.Vec2f.recycle(v2);
        Pools.Vec2f.recycle(v3);
    }

}
