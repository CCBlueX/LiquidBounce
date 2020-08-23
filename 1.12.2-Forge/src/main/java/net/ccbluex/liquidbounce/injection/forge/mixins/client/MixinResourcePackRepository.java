/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import com.google.common.collect.Lists;
import net.minecraft.client.resources.ResourcePackRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.util.List;

@Mixin(ResourcePackRepository.class)
public class MixinResourcePackRepository {

    @Shadow
    @Final
    private static Logger LOGGER;
    @Shadow
    @Final
    private File dirServerResourcepacks;

    /**
     * @author Mojang
     * @reason Fix a bug
     */
    @Overwrite
    private void deleteOldServerResourcesPacks() {
        try {
            List<File> resourcePacksInFolder = Lists.newArrayList(FileUtils.listFiles(this.dirServerResourcepacks, TrueFileFilter.TRUE, null));
            resourcePacksInFolder.sort(LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            int count = 0;

            for (File resourcePackFile : resourcePacksInFolder) {
                if (count++ >= 10) {
                    LOGGER.info("Deleting old server resource pack " + resourcePackFile.getName());
                    try {
                        resourcePackFile.delete();
                    } catch (Throwable ignored) {
                    }
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }
}