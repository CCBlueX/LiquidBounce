/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.ccbluex.liquidbounce.features.cosmetic.Cosmetics;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntry {

    @Shadow @Final private GameProfile profile;

    @Shadow @Final private Map<MinecraftProfileTexture.Type, Identifier> textures;
    private boolean loadedCapeTexture = false;

    @Inject(method = "getCapeTexture", at = @At("HEAD"))
    private void injectCapeCosmetic(CallbackInfoReturnable<Identifier> callbackInfo) {
        fetchCapeTexture();
    }

    @Inject(method = "getElytraTexture", at = @At("HEAD"))
    private void injectElytraCosmetic(CallbackInfoReturnable<Identifier> callbackInfo) {
        fetchCapeTexture();
    }

    private void fetchCapeTexture() {
        if (loadedCapeTexture)
            return;

        loadedCapeTexture = true;

        final Map<MinecraftProfileTexture.Type, Identifier> textures = this.textures;
        Cosmetics.INSTANCE.loadPlayerCape(this.profile, id -> {
            textures.put(MinecraftProfileTexture.Type.CAPE, id);
        });
    }

}
