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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.cosmetic.CapeCosmeticsManager;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntry {

    @Shadow
    @Final
    private GameProfile profile;

    @Unique
    private boolean capeTextureLoading = false;
    @Unique
    private Identifier capeTexture = null;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void liquid_bounce$init(GameProfile profile, boolean secureChatEnforced, CallbackInfo ci) {
        if (ModuleSkinChanger.INSTANCE.getRunning() && MinecraftClient.getInstance().getGameProfile() == this.profile) {
            ModuleSkinChanger.INSTANCE.getSkinTextures().get();
        }

        liquid_bounce$fetchCapeTexture();
    }

    @ModifyReturnValue(method = "getSkinTextures", at = @At("RETURN"))
    private SkinTextures liquid_bounce$skin(SkinTextures original) {
        if (ModuleSkinChanger.INSTANCE.getRunning() && MinecraftClient.getInstance().getGameProfile().equals(this.profile)) {
            original = ModuleSkinChanger.INSTANCE.getSkinTextures().get();
        }

        if (capeTexture != null) {
            return new SkinTextures(original.texture(), original.textureUrl(), capeTexture,
                    original.elytraTexture(), original.model(), original.secure());
        }

        liquid_bounce$fetchCapeTexture();
        return original;
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
