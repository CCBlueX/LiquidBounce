package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.exploit.UnSaddledRide;
import net.minecraft.entity.passive.EntityHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityHorse.class)
public class MixinEntityHorse {

  @Inject(method = "isHorseSaddled", cancellable = true, at = @At("HEAD"))
  private void injectIsHorseSaddled(CallbackInfoReturnable<Boolean> cir) {
    // noinspection ConstantConditions
    if (LiquidBounce.moduleManager.getModule(UnSaddledRide.class).getState()) {
      cir.setReturnValue(true);
      cir.cancel();
    }
  }
}
