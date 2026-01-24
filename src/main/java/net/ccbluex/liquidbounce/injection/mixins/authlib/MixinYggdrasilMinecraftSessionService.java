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

package net.ccbluex.liquidbounce.injection.mixins.authlib;

import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.TextureUrlChecker;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleYggdrasilSignatureFix;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(YggdrasilMinecraftSessionService.class)

public abstract class MixinYggdrasilMinecraftSessionService {
    @Inject(
            method = "getPropertySignatureState",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void bypassSignature(Property property, CallbackInfoReturnable<SignatureState> cir) {
        if (ModuleYggdrasilSignatureFix.INSTANCE.getRunning()) {
            cir.setReturnValue(SignatureState.SIGNED);
        }
    }

    @Redirect(
            method = "unpackTextures",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/authlib/yggdrasil/TextureUrlChecker;isAllowedTextureDomain(Ljava/lang/String;)Z"
            ),
            remap = false
    )
    private boolean bypassUrlCheck(String url) {
        if (ModuleYggdrasilSignatureFix.INSTANCE.getRunning()) {
            return true;
        }
        return TextureUrlChecker.isAllowedTextureDomain(url);
    }


    @Inject(method = "unpackTextures", at = @At("RETURN"), cancellable = true, remap = false)
    private void onUnpackTexturesReturn(Property packedTextures, CallbackInfoReturnable<MinecraftProfileTextures> cir) {
        if (!ModuleYggdrasilSignatureFix.INSTANCE.getRunning() || !ModuleYggdrasilSignatureFix.INSTANCE.getForceSlimModel())
            return;

        MinecraftProfileTextures original = cir.getReturnValue();
        if (original == null || original == MinecraftProfileTextures.EMPTY || original.skin() == null) return;

        String url = original.skin().getUrl();
        if (!url.contains("127.0.0.1")) return;

        MinecraftProfileTextures forced = new MinecraftProfileTextures(
                new MinecraftProfileTexture(url, Map.of("model", PlayerModelType.SLIM.getSerializedName())),
                original.cape(),
                original.elytra(),
                original.signatureState()
        );

        cir.setReturnValue(forced);
    }

}
