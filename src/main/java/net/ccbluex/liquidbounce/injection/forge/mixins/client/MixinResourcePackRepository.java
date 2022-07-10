/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import java.io.File;

import net.minecraft.client.resources.ResourcePackRepository;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ResourcePackRepository.class)
public class MixinResourcePackRepository
{
    @Shadow
    @Final
    private File dirServerResourcepacks;

    @Shadow
    @Final
    private static Logger logger;

    @Shadow
    private void deleteOldServerResourcesPacks()
    {
    }

    @Redirect(method = "downloadResourcePack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/ResourcePackRepository;deleteOldServerResourcesPacks()V"))
    public void injectExceptionHandler(final ResourcePackRepository instance)
    {
        try
        {
            deleteOldServerResourcesPacks();
        }
        catch (final Throwable t)
        {
            logger.warn("Failed to delete old server resource packs", t);
        }
    }
}
