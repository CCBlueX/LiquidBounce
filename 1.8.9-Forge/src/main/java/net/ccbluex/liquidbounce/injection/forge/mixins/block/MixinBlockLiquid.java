/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.block;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoSlow;
import net.ccbluex.liquidbounce.features.module.modules.world.Liquids;
import net.minecraft.block.BlockLiquid;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(BlockLiquid.class)
public class MixinBlockLiquid
{

	@Inject(method = "canCollideCheck", at = @At("HEAD"), cancellable = true)
	private void onCollideCheck(final CallbackInfoReturnable<Boolean> callbackInfoReturnable)
	{
		// Liquids
		if (LiquidBounce.moduleManager.get(Liquids.class).getState())
			callbackInfoReturnable.setReturnValue(true);
	}

	@Inject(method = "modifyAcceleration", at = @At("HEAD"), cancellable = true)
	private void onModifyAcceleration(final CallbackInfoReturnable<Vec3> callbackInfoReturnable)
	{
		// NoSlow LiquidPush
		final NoSlow noSlow = (NoSlow) LiquidBounce.moduleManager.get(NoSlow.class);

		if (noSlow.getState() && noSlow.getLiquidPushValue().get())
			callbackInfoReturnable.setReturnValue(new Vec3(0.0D, 0.0D, 0.0D));
	}
}
