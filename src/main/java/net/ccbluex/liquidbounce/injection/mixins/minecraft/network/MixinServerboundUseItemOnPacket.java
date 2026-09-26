/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import io.netty.buffer.ByteBuf;
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.disablers.DisablerVerusScaffoldG;
import net.minecraft.core.BlockPos;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@NullMarked
@Mixin(ServerboundUseItemOnPacket.class)
public abstract class MixinServerboundUseItemOnPacket {
    /**
     * @param original {@link BlockHitResult#STREAM_CODEC}
     */
    @ModifyArg(
        method = "<clinit>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/codec/StreamCodec;composite(Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lnet/minecraft/network/codec/StreamCodec;Ljava/util/function/Function;Lcom/mojang/datafixers/util/Function3;)Lnet/minecraft/network/codec/StreamCodec;"
        ),
        index = 2
    )
    private static StreamCodec<ByteBuf, BlockHitResult> writeBlockHitResult(StreamCodec<ByteBuf, BlockHitResult> original) {
        return new StreamCodec<>() {
            @Override
            public BlockHitResult decode(ByteBuf input) {
                return original.decode(input);
            }

            @Override
            public void encode(ByteBuf output, BlockHitResult blockHit) {
                if (DisablerVerusScaffoldG.INSTANCE.getRunning()) {
                    BlockPos.STREAM_CODEC.encode(output, blockHit.getBlockPos());
                    VarInt.write(output, 6 + blockHit.getDirection().ordinal() * 7);
                    output.writeFloat((float) blockHit.getLocation().x - blockHit.getBlockPos().getX());
                    output.writeFloat((float) blockHit.getLocation().y - blockHit.getBlockPos().getY());
                    output.writeFloat((float) blockHit.getLocation().z - blockHit.getBlockPos().getZ());
                    output.writeBoolean(blockHit.isInside());
                    output.writeBoolean(blockHit.isWorldBorderHit());
                } else {
                    original.encode(output, blockHit);
                }
            }
        };
    }
}
