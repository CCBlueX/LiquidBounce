package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.disablers.DisablerVerusScaffoldG;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerInteractBlockC2SPacket.class)
public class MixinPlayerInteractBlockC2SPacket {
    @Redirect(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketByteBuf;writeBlockHitResult(Lnet/minecraft/util/hit/BlockHitResult;)V"))
    private void writeBlockHitResult(PacketByteBuf buf, BlockHitResult hitResult) {
        if (DisablerVerusScaffoldG.INSTANCE.getRunning()) {
            buf.writeBlockPos(hitResult.getBlockPos());
            buf.writeVarInt(6 + hitResult.getSide().ordinal() * 7);
            buf.writeFloat((float) hitResult.getPos().x - hitResult.getBlockPos().getX());
            buf.writeFloat((float) hitResult.getPos().y - hitResult.getBlockPos().getY());
            buf.writeFloat((float) hitResult.getPos().z - hitResult.getBlockPos().getZ());
            buf.writeBoolean(hitResult.isInsideBlock());
        } else buf.writeBlockHitResult(hitResult);
    }
}
