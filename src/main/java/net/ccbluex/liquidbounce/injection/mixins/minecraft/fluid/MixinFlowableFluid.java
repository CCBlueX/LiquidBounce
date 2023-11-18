package net.ccbluex.liquidbounce.injection.mixins.minecraft.fluid;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.FluidPushEvent;
import net.minecraft.fluid.FlowableFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;

@Mixin(FlowableFluid.class)
public class MixinFlowableFluid {

    @Redirect(method = "getVelocity", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", ordinal = 0))
    private boolean hookLiquidPush(Iterator instance) {
        final FluidPushEvent fluidPushEvent = new FluidPushEvent();
        EventManager.INSTANCE.callEvent(fluidPushEvent);
        return !fluidPushEvent.isCancelled() && instance.hasNext();
    }
}
