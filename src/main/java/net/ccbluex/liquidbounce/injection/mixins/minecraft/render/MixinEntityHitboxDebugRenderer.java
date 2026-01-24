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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleHitbox;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class MixinEntityHitboxDebugRenderer {

    @ModifyExpressionValue(
        method = "showHitboxes",
        at = {
            @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
            ),
            @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
            ),
        }
    )
    private AABB getBoundingBox(AABB original, @Local(argsOnly = true) Entity entity) {
        var moduleHitBox = ModuleHitbox.INSTANCE;
        if (entity != null && moduleHitBox.getRunning()
            && moduleHitBox.getApplyToDebugHitbox() && CombatExtensionsKt.shouldBeAttacked(entity)) {
            var expansion = moduleHitBox.getSize();
            return original.inflate(expansion);
        }
        return original;
    }

}
