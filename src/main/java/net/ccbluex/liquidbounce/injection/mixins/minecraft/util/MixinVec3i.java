/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.util;

import net.minecraft.util.math.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Vec3i.class)
public abstract class MixinVec3i {

    @Shadow
    public abstract int getX();

    @Shadow
    public abstract int getY();

    @Shadow
    public abstract int getZ();

    /**
     * @author MukjepScarlet
     * @reason avoid useless int -> float -> int
     */
    @Overwrite
    public int getManhattanDistance(Vec3i vec) {
        int f = Math.abs(vec.getX() - this.getX());
        int g = Math.abs(vec.getY() - this.getY());
        int h = Math.abs(vec.getZ() - this.getZ());
        return f + g + h;
    }

}
