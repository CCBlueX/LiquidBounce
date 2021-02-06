package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.minecraft.client.renderer.tileentity.TileEntityMobSpawnerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.tileentity.MobSpawnerBaseLogic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityMobSpawnerRenderer.class)
public class MixinTileEntityMobSpawnerRenderer
{
	@Inject(method = "renderMob", cancellable = true, at = @At("HEAD"))
	private static void crashSpawnerExploitFix(final MobSpawnerBaseLogic mobSpawnerLogic, final double posX, final double posY, final double posZ, final float partialTicks, final CallbackInfo ci)
	{
		final Entity entity = mobSpawnerLogic.func_180612_a(mobSpawnerLogic.getSpawnerWorld());

		if (entity == null || entity instanceof EntityPainting)
			ci.cancel();
	}
}
