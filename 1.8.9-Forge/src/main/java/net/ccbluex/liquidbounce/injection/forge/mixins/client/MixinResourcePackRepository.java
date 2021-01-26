/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import java.io.File;
import java.util.Collections;
import java.util.List;

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

@Mixin(ResourcePackRepository.class)
public class MixinResourcePackRepository
{

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
	private void deleteOldServerResourcesPacks()
	{
		try
		{
			final List<File> resourcePacks = Lists.newArrayList(FileUtils.listFiles(dirServerResourcepacks, TrueFileFilter.TRUE, null));
			resourcePacks.sort(LastModifiedFileComparator.LASTMODIFIED_REVERSE);
			int index = 0;

			for (final File resourcePackFile : resourcePacks)
				if (index++ >= 10)
				{
					logger.info("Deleting old server resource pack {}", resourcePackFile.getName());
					FileUtils.deleteQuietly(resourcePackFile);
				}
		}
		catch (final Throwable e)
		{
			e.printStackTrace();
		}
	}
}
