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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.util;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.spoofer.SpooferFingerprint;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.DownloadQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;
import java.util.UUID;

@Mixin(DownloadQueue.class)
public abstract class MixinDownloadQueue {

    @Shadow
    @Final
    private Path cacheDir;

    @ModifyExpressionValue(method = "method_55485", at = @At(value = "INVOKE", target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"))
    private Path hookResolve(Path original, @Local(argsOnly = true) UUID id) {
        // Check if our fingerprint spoofer is enabled or,
        // the folder has been altered with by another mod.
        if (!SpooferFingerprint.INSTANCE.getRunning() || !original.getParent().equals(cacheDir)) {
            return original;
        }

        var accountId = Minecraft.getInstance().getUser().getProfileId();
        var accountFolder = cacheDir.resolve(accountId.toString());
        return accountFolder.resolve(id.toString());
    }

}
