/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.tweaks;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.DataInputStream;
import java.io.IOException;

@Mixin(AnvilChunkLoader.class)
public class MixinAnvilChunkLoader {

    /**
     * Due to limitations with local variable capture in Mixins,
     * a Redirect is used as an alternative.
     */
    @Redirect(
            method = "loadChunk__Async",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/CompressedStreamTools;read(Ljava/io/DataInputStream;)Lnet/minecraft/nbt/NBTTagCompound;"
            )
    )

    private NBTTagCompound redirectReadChunkData(DataInputStream inputStream) throws IOException {
        try (DataInputStream stream = inputStream) {
            return CompressedStreamTools.read(stream);
        }
    }
}
