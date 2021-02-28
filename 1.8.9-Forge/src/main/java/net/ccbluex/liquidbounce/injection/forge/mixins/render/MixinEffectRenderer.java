/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityParticleEmitter;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EffectRenderer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEffectRenderer
{

	@Shadow
	protected abstract void updateEffectLayer(int layer);

	@Shadow
	private List<EntityParticleEmitter> particleEmitters;

	/**
	 * @author Mojang
	 * @author Marco
	 * @reason
	 */
	@Overwrite
	public void updateEffects()
	{
		try
		{
			IntStream.range(0, 4).forEach(this::updateEffectLayer);

			final Iterator<EntityParticleEmitter> it = particleEmitters.iterator();

			while (it.hasNext())
			{
				final EntityParticleEmitter entityParticleEmitter = it.next();

				entityParticleEmitter.onUpdate();

				if (entityParticleEmitter.isDead)
					it.remove();
			}
		}
		catch (final ConcurrentModificationException ignored)
		{
		}
	}
}
