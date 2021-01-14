package net.ccbluex.liquidbounce.injection.mixins;

import net.ccbluex.liquidbounce.event.EntityTickEvent;
import net.ccbluex.liquidbounce.event.EventManager;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    /**
     * Hook entity tick event at HEAD
     *
     * Not useful to check for loaded chunk, PRE tick, next step is movement packet
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void hookTickEvent(final CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new EntityTickEvent());
    }

}
