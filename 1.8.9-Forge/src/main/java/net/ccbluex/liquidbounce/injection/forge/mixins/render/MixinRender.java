/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.RenderEntityEvent;
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.EntityImplKt;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Render.class)
public abstract class MixinRender
{
	@Shadow
	protected abstract <T extends Entity> boolean bindEntityTexture(T entity);

	@Inject(method = "doRender", at = @At("HEAD"))
	protected void doRender(final Entity entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks, final CallbackInfo callbackInfo)
	{
		LiquidBounce.eventManager.callEvent(new RenderEntityEvent(EntityImplKt.wrap(entity), x, y, z, entityYaw, partialTicks));
	}
}
