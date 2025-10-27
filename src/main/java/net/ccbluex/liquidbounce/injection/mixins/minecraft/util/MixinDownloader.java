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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.util;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.spoofer.SpooferFingerprint;
import net.ccbluex.liquidbounce.utils.client.ClientUtilsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Downloader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;
import java.util.UUID;

@Mixin(Downloader.class)
public abstract class MixinDownloader {

    @Shadow
    @Final
    private Path directory;

    @ModifyExpressionValue(method = "method_55485", at = @At(value = "INVOKE", target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"))
    private Path hookResolve(Path original, @Local(argsOnly = true) UUID id) {
        if (SpooferFingerprint.INSTANCE.getRunning()) {
            var accountId = MinecraftClient.getInstance().getSession().getUuidOrNull();
            if (accountId == null) {
                ClientUtilsKt.getLogger().warn("Failed to change download directory, because account id is null.");
                return original;
            }

            return directory.resolve(accountId.toString()).resolve(id.toString());
        }

        return original;
    }


}
