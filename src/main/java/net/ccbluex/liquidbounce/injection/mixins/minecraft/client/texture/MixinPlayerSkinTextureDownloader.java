
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.texture;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.PlayerSkinTextureDownloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PlayerSkinTextureDownloader.class)
public class MixinPlayerSkinTextureDownloader {

    /**
     * @author ccbluex
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
