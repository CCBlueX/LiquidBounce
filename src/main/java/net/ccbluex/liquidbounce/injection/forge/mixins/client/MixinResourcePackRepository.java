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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Mixin(ResourcePackRepository.class)
public class MixinResourcePackRepository {

    @Shadow
    @Final
    private File dirServerResourcepacks;

    @Shadow
    @Final
    private static Logger logger;

    /**
     * @author Mojang
     * @reason Fix a bug
     */
    @Overwrite
    private void deleteOldServerResourcesPacks() {
        try {
            List<File> lvt_1_1_ = Lists.newArrayList(FileUtils.listFiles(this.dirServerResourcepacks, TrueFileFilter.TRUE, null));
            Collections.sort(lvt_1_1_, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            int lvt_2_1_ = 0;
            Iterator lvt_3_1_ = lvt_1_1_.iterator();

            while(lvt_3_1_.hasNext()) {
                File lvt_4_1_ = (File) lvt_3_1_.next();
                if(lvt_2_1_++ >= 10) {
                    logger.info("Deleting old server resource pack " + lvt_4_1_.getName());
                    FileUtils.deleteQuietly(lvt_4_1_);
                }
            }
        }catch(final Throwable e) {
            e.printStackTrace();
        }
    }
}