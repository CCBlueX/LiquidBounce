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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.ModuleDisabler;
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.disablers.DisablerVulcanScaffold;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoFov;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSkinChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends Player {

    public MixinAbstractClientPlayer(Level world, GameProfile gameProfile) {
        super(world, gameProfile);
    }

    @ModifyReturnValue(method = "getFieldOfViewModifier", at = @At("RETURN"))
    private float injectFovMultiplier(float original) {
        if (ModuleNoFov.INSTANCE.getRunning()) {
            return ModuleNoFov.INSTANCE.getFovMultiplier(original);
        }
        return original;
    }

    @ModifyExpressionValue(method = "getFieldOfViewModifier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"))
    private double injectAlwaysSprint(double original) {
        if (ModuleDisabler.INSTANCE.getRunning() && DisablerVulcanScaffold.INSTANCE.getRunning()) {
            return 0.10000000149011612D;
        }
        return original;
    }

    @Inject(method = "getSkin", at = @At("TAIL"), cancellable = true)
    private void injectCustomSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        if (this.getUUID().equals(client.player.getUUID()) && ModuleSkinChanger.shouldApplyChanges()) {
            var customSupplier = ModuleSkinChanger.INSTANCE.getSkinTextures();
            if (customSupplier != null) {
                PlayerSkin original = cir.getReturnValue();
                PlayerSkin customTextures = customSupplier.get();
                cir.setReturnValue(new PlayerSkin(
                        customTextures.body(),
                        original.cape(),
                        customTextures.elytra(),
                        customTextures.model(),
                        customTextures.secure()
                ));
            }
        }
    }
}
