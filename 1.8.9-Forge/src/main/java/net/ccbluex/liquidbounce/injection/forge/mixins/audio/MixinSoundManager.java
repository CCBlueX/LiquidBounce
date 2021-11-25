package net.ccbluex.liquidbounce.injection.forge.mixins.audio;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Multimap;

import net.minecraft.client.audio.*;
import net.minecraft.client.audio.SoundManager.SoundSystemStarterThread;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.ITickable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SoundManager.class)
public abstract class MixinSoundManager
{
	@Shadow
	@Final
	private static Marker LOG_MARKER;

	@Shadow
	@Final
	private static Logger logger;

	@Shadow
	@Final
	public SoundHandler sndHandler;

	@Shadow
	@Final
	private GameSettings options;
	@Shadow
	private SoundSystemStarterThread sndSystem;

	@Shadow
	private boolean loaded;

	@Shadow
	private int playTime;

	@Shadow
	@Final
	private Map<String, ISound> playingSounds;

	@Shadow
	@Final
	private Map<ISound, String> invPlayingSounds;

	@Shadow
	private Map<ISound, SoundPoolEntry> playingSoundPoolEntries;

	@Shadow
	@Final
	private Multimap<SoundCategory, String> categorySounds;

	@Shadow
	@Final
	private List<ITickableSound> tickableSounds;

	@Shadow
	@Final
	private Map<ISound, Integer> delayedSounds;

	@Shadow
	@Final
	private Map<String, Integer> playingSoundsStopTime;

	@Shadow
	public abstract void playSound(ISound p_sound);

	@Shadow
	public abstract void stopSound(ISound sound);

	@Shadow
	private float getNormalizedPitch(final ISound sound, final SoundPoolEntry entry)
	{
		return 0.0F;
	}

	@Shadow
	private float getNormalizedVolume(final ISound sound, final SoundPoolEntry entry, final SoundCategory category)
	{
		return 0.0F;
	}

	/**
	 * @author Mojang
	 * @author Marco
	 * @reason Fix ConcurrentModificationException and reduce memory usages
	 */
	@Overwrite
	public void updateAllSounds()
	{
		++playTime;

		String sourceName;
		ISound sound;
		try
		{
			final Iterator<ITickableSound> tickableSoundItr = tickableSounds.iterator();
			SoundPoolEntry poolEntry;
			ITickableSound tickableSound;
			while (tickableSoundItr.hasNext())
			{
				tickableSound = tickableSoundItr.next();

				tickableSound.update();

				if (tickableSound.isDonePlaying())
					stopSound(tickableSound);
				else
				{
					sourceName = invPlayingSounds.get(tickableSound);
					poolEntry = playingSoundPoolEntries.get(tickableSound);

					sndSystem.setVolume(sourceName, getNormalizedVolume(tickableSound, poolEntry, sndHandler.getSound(tickableSound.getSoundLocation()).getSoundCategory()));
					sndSystem.setPitch(sourceName, getNormalizedPitch(tickableSound, poolEntry));
					sndSystem.setPosition(sourceName, tickableSound.getXPosF(), tickableSound.getYPosF(), tickableSound.getZPosF());
				}
			}
		}
		catch(final ConcurrentModificationException ignored)
		{
			logger.warn(LOG_MARKER, "Suppressed ConcurrentModificationException in TickableSoundIteration");
		}

		try
		{
			Entry<String, ISound> entry;

			final Iterator<Entry<String, ISound>> playingSoundItr = playingSounds.entrySet().iterator();

			while (playingSoundItr.hasNext())
			{
				entry = playingSoundItr.next();

				sourceName = entry.getKey();
				sound = entry.getValue();

				if (!sndSystem.playing(sourceName))
				{
					final int i = playingSoundsStopTime.get(sourceName);
					if (i <= playTime)
					{
						final int repeatDelay = sound.getRepeatDelay();
						if (sound.canRepeat() && repeatDelay > 0)
							delayedSounds.put(sound, playTime + repeatDelay);

						playingSoundItr.remove();

						logger.debug(LOG_MARKER, "Removed channel {} because it's not playing anymore", sourceName);
						sndSystem.removeSource(sourceName);
						playingSoundsStopTime.remove(sourceName);
						playingSoundPoolEntries.remove(sound);

						try
						{
							categorySounds.remove(sndHandler.getSound(sound.getSoundLocation()).getSoundCategory(), sourceName);
						}
						catch (final RuntimeException ignored)
						{
						}

						if (sound instanceof ITickableSound)
							tickableSounds.remove(sound);
					}
				}
			}
		}
		catch (final ConcurrentModificationException ignored)
		{
			logger.warn(LOG_MARKER, "Suppressed ConcurrentModificationException in PlayingSoundIteration");
		}

		try
		{
			final Iterator<Entry<ISound, Integer>> delayedSoundItr = delayedSounds.entrySet().iterator();
			Entry<ISound, Integer> soundEntry;
			while (delayedSoundItr.hasNext())
			{
				soundEntry = delayedSoundItr.next();
				if (playTime >= soundEntry.getValue())
				{
					sound = soundEntry.getKey();
					if (sound instanceof ITickableSound)
						((ITickable) sound).update();

					playSound(sound);
					delayedSoundItr.remove();
				}
			}
		}
		catch(final ConcurrentModificationException ignored)
		{
			logger.warn(LOG_MARKER, "Suppressed ConcurrentModificationException in DelayedSoundIteration");
		}
	}
}
