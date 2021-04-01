package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModulePortalMenu;
import net.ccbluex.liquidbounce.utils.extensions.Rotation;
import net.ccbluex.liquidbounce.utils.extensions.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Shadow private float lastYaw;

    @Shadow private float lastPitch;

    /**
     * Hook entity tick event
     */
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.AFTER))
    private void hookTickEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerTickEvent());
    }

    /**
     * Hook entity movement tick event
     */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void hookMovementTickEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerMovementTickEvent());
    }

    /**
     * Hook entity movement tick event at HEAD and call out PRE tick movement event
     */
    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    private void hookMovementPre(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerNetworkMovementTickEvent(EventState.PRE));
    }

    /**
     * Hook entity movement tick event at RETURN and call out POST tick movement event
     */
    @Inject(method = "sendMovementPackets", at = @At("RETURN"))
    private void hookMovementPost(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerNetworkMovementTickEvent(EventState.POST));
    }

    /**
     * Hook push out function tick at HEAD and call out push out event, which is able to stop the cancel the execution.
     */
    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void hookPushOut(CallbackInfo callbackInfo) {
        final PlayerPushOutEvent pushOutEvent = new PlayerPushOutEvent();
        EventManager.INSTANCE.callEvent(pushOutEvent);
        if (pushOutEvent.isCancelled())
            callbackInfo.cancel();
    }

    /**
     * Hook push out function tick at HEAD and call out push out event, which is able to stop the cancel the execution.
     */
    @Inject(method = "move", at = @At("HEAD"))
    private void hookMove(MovementType type, Vec3d movement, CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new PlayerMoveEvent(type, movement));
    }

    @Inject(method = "updateNausea", at = @At(value = "FIELD", target =
            "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;"), cancellable = true)
    private void hookNetherClosingScreen(CallbackInfo callbackInfo) {
        if (ModulePortalMenu.INSTANCE.getEnabled()) {
            callbackInfo.cancel();
        }
    }

    // Silent rotations (Rotation Manager)

    private boolean updatedSilent;

    /**
     * Hook silent rotations
     */
    @ModifyVariable(method = "sendMovementPackets", ordinal = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z", shift = At.Shift.BEFORE))
    private boolean hookSilentRotations(boolean bl4) {
        updatedSilent = RotationManager.INSTANCE.needsUpdate(lastYaw, lastPitch);
        return (bl4 && RotationManager.INSTANCE.getCurrRotation() == null) || updatedSilent;
    }

    /**
     * Hook silent rotations
     */
    @Inject(method = "sendMovementPackets", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;lastPitch:F", shift = At.Shift.AFTER))
    private void hookSilentRotationsUpdate(CallbackInfo callbackInfo) {
        if (updatedSilent) {
            updatedSilent = false;

            final Rotation currRotation = RotationManager.INSTANCE.getCurrRotation();
            if (currRotation == null)
                return;

            this.lastYaw = currRotation.getYaw();
            this.lastPitch = currRotation.getPitch();
        }
    }

}
