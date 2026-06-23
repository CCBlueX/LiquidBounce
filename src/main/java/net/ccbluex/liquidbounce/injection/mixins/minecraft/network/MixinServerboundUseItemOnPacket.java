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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.features.module.modules.exploit.disabler.disablers.DisablerVerusScaffoldG;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerboundUseItemOnPacket.class)
public abstract class MixinServerboundUseItemOnPacket {
    @WrapOperation(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;writeBlockHitResult(Lnet/minecraft/world/phys/BlockHitResult;)V"))
    private void writeBlockHitResult(FriendlyByteBuf buf, BlockHitResult blockHit, Operation<Void> original) {
        if (DisablerVerusScaffoldG.INSTANCE.getRunning()) {
            buf.writeBlockPos(blockHit.getBlockPos());
            buf.writeVarInt(6 + blockHit.getDirection().ordinal() * 7);
            buf.writeFloat((float) blockHit.getLocation().x - blockHit.getBlockPos().getX());
            buf.writeFloat((float) blockHit.getLocation().y - blockHit.getBlockPos().getY());
            buf.writeFloat((float) blockHit.getLocation().z - blockHit.getBlockPos().getZ());
            buf.writeBoolean(blockHit.isInside());
        } else {
            original.call(buf, blockHit);
        }
    }
}
