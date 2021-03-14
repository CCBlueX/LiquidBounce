/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Queue;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEmitter;
import net.minecraft.client.particle.ParticleManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ParticleManager.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEffectRenderer
{

	@Shadow
	@Final
	private final Queue<ParticleEmitter> particleEmitters = Queues.newArrayDeque();
	@Shadow
	@Final
	private Queue<Particle> queue;
	@Shadow
	@Final
	private ArrayDeque<Particle>[][] fxLayers;

	@Shadow
	protected abstract void updateEffectLayer(int layer);

	/**
	 * @author CCBlueX (superblaubeere27)
	 */
	@Overwrite
	public void updateEffects()
	{
		try
		{
			for (int i = 0; i < 4; ++i)
			{
				updateEffectLayer(i);
			}

			if (!particleEmitters.isEmpty())
			{
				final List<ParticleEmitter> list = Lists.newArrayList();

				for (final ParticleEmitter particleemitter : particleEmitters)
				{
					particleemitter.onUpdate();

					if (!particleemitter.isAlive())
					{
						list.add(particleemitter);
					}
				}

				particleEmitters.removeAll(list);
			}

			if (!queue.isEmpty())
			{
				for (Particle particle = queue.poll(); particle != null; particle = queue.poll())
				{
					final int j = particle.getFXLayer();
					final int k = particle.shouldDisableDepth() ? 0 : 1;

					if (fxLayers[j][k].size() >= 16384)
					{
						fxLayers[j][k].removeFirst();
					}

					fxLayers[j][k].add(particle);
				}
			}
		}
		catch (final ConcurrentModificationException ignored)
		{
		}
	}
}
