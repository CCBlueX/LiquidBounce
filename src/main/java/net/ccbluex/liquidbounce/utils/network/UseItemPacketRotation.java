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

package net.ccbluex.liquidbounce.utils.network;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import org.jspecify.annotations.Nullable;

/**
 * Constructs use-item packets whose rotations must not be replaced by the current managed rotation.
 */
public final class UseItemPacketRotation {

    private static final ScopedValue<Boolean> EXPLICIT_ROTATION = ScopedValue.newInstance();

    private UseItemPacketRotation() {
    }

    public static boolean shouldOverride() {
        return !EXPLICIT_ROTATION.isBound();
    }

    public static ServerboundUseItemPacket createExplicit(
        @Nullable InteractionHand hand,
        int sequence,
        float yRot,
        float xRot
    ) {
        return ScopedValue.where(EXPLICIT_ROTATION, true).call(
            () -> new ServerboundUseItemPacket(hand, sequence, yRot, xRot)
        );
    }

}
