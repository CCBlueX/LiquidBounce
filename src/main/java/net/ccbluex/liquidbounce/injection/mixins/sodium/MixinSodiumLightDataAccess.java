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
package net.ccbluex.liquidbounce.injection.mixins.sodium;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.caffeinemc.mods.sodium.client.model.light.data.LightDataAccess;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(LightDataAccess.class)
public abstract class MixinSodiumLightDataAccess {

    @Shadow
    @Final
    private BlockPos.MutableBlockPos pos;

    @Shadow
    protected BlockAndTintGetter level;

    /**
     * Maximum light level for all color channels.
     * <p>
     * Minecraft's lighting system represents light in a range of 0-15,
     * where 15 corresponds to maximum brightness.
     */
    @Unique
    private static final int MAX_LIGHT_LEVEL = 15 | 15 << 4 | 15 << 8;

    @ModifyReturnValue(method = "compute", at = @At("RETURN"))
    private int modifyLightLevel(int original) {
        var xray = ModuleXRay.INSTANCE;
        if (xray.getRunning() && xray.getFullBright()) {
            var blockState = this.level.getBlockState(pos);

            if (xray.shouldRender(blockState, pos)) {
                // Ensures that the brightness is on max for all color channels
                return original | MAX_LIGHT_LEVEL;
            }
        }

        return original;
    }

}
