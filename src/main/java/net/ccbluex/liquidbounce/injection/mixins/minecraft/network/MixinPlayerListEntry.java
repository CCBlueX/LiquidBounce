/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.cosmetic.Cosmetics;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntry {

    @Shadow @Final private GameProfile profile;
    private boolean cosmeticTexturesLoaded = false;

    private Identifier capeTexture = null;
    private Identifier elytraTexture = null;

    @Inject(method = "loadTextures", at = @At("RETURN"))
    private void injectTextureLoading(CallbackInfo callbackInfo) {
        synchronized(this) {
            if (!this.cosmeticTexturesLoaded) {
                this.cosmeticTexturesLoaded = true;

                Cosmetics.INSTANCE.loadCosmeticTexture(profile.getId(), (type, id, texture) -> {
                    switch (type) {
                        case SKIN -> {
                            // there is no skin cosmetic
                        }
                        case CAPE -> capeTexture = id;
                        case ELYTRA -> elytraTexture = id;
                    }
                });
            }

        }
    }

    @Inject(method = "getCapeTexture", at = @At("RETURN"), cancellable = true)
    private void injectCapeCosmetic(CallbackInfoReturnable<Identifier> callbackInfo) {
        if (capeTexture == null) {
            return;
        }

        callbackInfo.setReturnValue(capeTexture);
    }

    @Inject(method = "getElytraTexture", at = @At("RETURN"), cancellable = true)
    private void injectElytraCosmetic(CallbackInfoReturnable<Identifier> callbackInfo) {
        if (elytraTexture == null) {
            return;
        }

        callbackInfo.setReturnValue(elytraTexture);
    }

}
