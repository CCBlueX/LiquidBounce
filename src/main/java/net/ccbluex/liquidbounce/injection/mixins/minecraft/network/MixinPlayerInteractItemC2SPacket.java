package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerInteractItemC2SPacket.class)
public class MixinPlayerInteractItemC2SPacket {

    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/util/Hand;IFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/network/packet/c2s/play/PlayerInteractItemC2SPacket;yaw:F"))
    private float modifyYaw(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return original;
        }

        return rotation.getYaw();
    }

    @ModifyExpressionValue(method = "<init>(Lnet/minecraft/util/Hand;IFF)V", at = @At(value = "FIELD", target = "Lnet/minecraft/network/packet/c2s/play/PlayerInteractItemC2SPacket;pitch:F"))
    private float modifyPitch(float original) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        if (rotation == null) {
            return original;
        }

        return rotation.getPitch();
    }

}
