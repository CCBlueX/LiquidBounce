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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.texture;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PlayerSkinTextureDownloader.class)
public class MixinPlayerSkinTextureDownloader {

    /**
     * @author Chasteful
     * @reason Remove vanilla size check to allow non-64x64 skin textures (e.g. 128x128 from Netease servers).
     */
    @Overwrite
    private static NativeImage remapTexture(NativeImage image, String uri) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width == 64 && (height == 32 || height == 64)) {
            boolean old = height == 32;
            if (old) {
                NativeImage nativeImage = new NativeImage(64, 64, true);
                nativeImage.copyFrom(image);
                image.close();
                image = nativeImage;

            }
            return image;
        }

        return image;
    }
}
