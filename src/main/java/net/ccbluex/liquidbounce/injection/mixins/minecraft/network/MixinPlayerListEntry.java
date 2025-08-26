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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.cosmetic.CapeCosmeticsManager;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSkinChanger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntry {

    @Shadow
    @Final
    private GameProfile profile;

    @Unique
    private boolean capeTextureLoading = false;
    @Unique
    private Identifier capeTexture = null;

    @ModifyReturnValue(method = "getSkinTextures", at = @At("RETURN"))
    @SuppressWarnings({"ConstantConditions", "EqualsBetweenInconvertibleTypes", "RedundantCast"})
    private SkinTextures liquid_bounce$skin(SkinTextures original) {
        if (HideAppearance.INSTANCE.isDestructed()) {
            return original;
        }

        if (ModuleSkinChanger.INSTANCE.getRunning()) {
            var player = MinecraftClient.getInstance().player;
            if (player != null) {
                var playerListEntry = player.getPlayerListEntry();
                if (playerListEntry != null && playerListEntry.equals((PlayerListEntry) (Object) this)) {
                    var customSkinTextures = ModuleSkinChanger.INSTANCE.getSkinTextures();
                    if (customSkinTextures != null) {
                        original = customSkinTextures.get();
                    }
                }
            }
        }

        if (capeTexture != null) {
            return new SkinTextures(original.texture(), original.textureUrl(), capeTexture,
                    original.elytraTexture(), original.model(), original.secure());
        }

        liquid_bounce$fetchCapeTexture();
        return original;
    }

    @ModifyExpressionValue(method = "texturesSupplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;uuidEquals(Ljava/util/UUID;)Z"))
    private static boolean liquid_bounce$allow_custom_skin(boolean b) {
        return b || ModuleSkinChanger.INSTANCE.getRunning();
    }

    @Unique
    private void liquid_bounce$fetchCapeTexture() {
        if (capeTextureLoading) {
            return;
        }

        capeTextureLoading = true;
        CapeCosmeticsManager.INSTANCE.loadPlayerCape(this.profile, id -> capeTexture = id);
    }

}
