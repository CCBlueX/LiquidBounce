package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoSlow;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public class MixinMovementInputFromOptions extends MovementInput
{
	@Inject(method = "updatePlayerMoveState", at = @At(value = "FIELD", target = "Lnet/minecraft/util/MovementInputFromOptions;sneak:Z", shift = Shift.AFTER, ordinal = 0))
	private void updatePlayerMoveState(final CallbackInfo callbackInfo)
	{
		if (sneak)
		{
			final NoSlow noSlow = (NoSlow) LiquidBounce.moduleManager.get(NoSlow.class);

			moveForward *= noSlow.getSneakForwardMultiplier().get();
			moveStrafe *= noSlow.getSneakStrafeMultiplier().get();
		}
	}
}
