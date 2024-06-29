package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PacketByteBuf.class)
public abstract class MixinPacketByteBuf {

    @Shadow
    public abstract PacketByteBuf writeVarInt(int value);

    @Inject(
            method = "writeEnumConstant",
            at = @At("HEAD"),
            cancellable = true
    )
    public void writeEnumConstant(Enum<?> instance, CallbackInfoReturnable<PacketByteBuf> cir) {
        if (instance == null) {
            cir.setReturnValue(this.writeVarInt(-1));
        }
    }

}
