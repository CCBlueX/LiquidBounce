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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoFov;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSkinChanger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity extends PlayerEntity {

    public MixinAbstractClientPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @ModifyReturnValue(method = "getFovMultiplier", at = @At("RETURN"))
    private float injectFovMultiplier(float original) {
        if (ModuleNoFov.INSTANCE.getRunning()) {
            return ModuleNoFov.INSTANCE.getFovMultiplier(original);
        }
        return original;
    }

    @Inject(method = "getSkinTextures", at = @At("TAIL"), cancellable = true)
    private void injectCustomSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        if (this.getUuid().equals(client.player.getUuid()) && ModuleSkinChanger.shouldApplyChanges()) {
            var customSupplier = ModuleSkinChanger.INSTANCE.getSkinTextures();
            if (customSupplier != null) {
                SkinTextures original = cir.getReturnValue();
                SkinTextures customTextures = customSupplier.get();
                cir.setReturnValue(new SkinTextures(
                        customTextures.texture(),
                        customTextures.textureUrl(),
                        original.capeTexture(),
                        customTextures.elytraTexture(),
                        customTextures.model(),
                        customTextures.secure()
                ));
            }
        }
    }
}
