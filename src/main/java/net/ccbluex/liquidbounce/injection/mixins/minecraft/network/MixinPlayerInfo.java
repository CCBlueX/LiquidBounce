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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.cosmetic.CapeCosmeticsManager;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSkinChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInfo.class)
public abstract class MixinPlayerInfo {

    @Shadow
    @Final
    private GameProfile profile;

    @Unique
    private boolean capeTextureLoading = false;
    @Unique
    private Identifier capeTexture = null;

    @ModifyReturnValue(method = "getSkin", at = @At("RETURN"))
    @SuppressWarnings({"ConstantConditions", "EqualsBetweenInconvertibleTypes", "RedundantCast"})
    private PlayerSkin liquid_bounce$skin(PlayerSkin original) {
        if (HideAppearance.INSTANCE.isDestructed()) {
            return original;
        }

        if (ModuleSkinChanger.INSTANCE.getRunning()) {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                var playerListEntry = player.getPlayerInfo();
                if (playerListEntry != null && playerListEntry.equals((PlayerInfo) (Object) this)) {
                    var customSkinTextures = ModuleSkinChanger.INSTANCE.getSkinTextures();
                    if (customSkinTextures != null) {
                        original = customSkinTextures.get();
                    }
                }
            }
        }

        if (capeTexture != null) {
            return new PlayerSkin(original.body(), new ClientAsset.ResourceTexture(capeTexture, capeTexture),
                    original.elytra(), original.model(), original.secure());
        }

        liquid_bounce$fetchCapeTexture();
        return original;
    }

    @ModifyExpressionValue(method = "createSkinLookup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalPlayer(Ljava/util/UUID;)Z"))
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
