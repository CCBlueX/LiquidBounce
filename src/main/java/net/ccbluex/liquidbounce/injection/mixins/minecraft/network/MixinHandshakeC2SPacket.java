package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.spoofer.SpooferBungeeCord;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HandshakeC2SPacket.class)
public class MixinHandshakeC2SPacket {

    @ModifyExpressionValue(method = "write", at = @At(value = "FIELD", target = "Lnet/minecraft/network/packet/c2s/handshake/HandshakeC2SPacket;address:Ljava/lang/String;"))
    private String modifyAddress(String original) {
        if (SpooferBungeeCord.INSTANCE.getRunning()) {
            return SpooferBungeeCord.INSTANCE.modifyHandshakeAddress(original);
        }

        return original;
    }

}
