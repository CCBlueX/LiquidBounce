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

package net.ccbluex.liquidbounce.injection.mixins.blaze3d;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.GLX;
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManagerKt;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GLX.class)
public abstract class MixinGLX {

    @ModifyExpressionValue(method = "_initGlfw", at = @At(value = "FIELD", target = "Lnet/minecraft/SharedConstants;DEBUG_PREFER_WAYLAND:Z", opcode = Opcodes.GETSTATIC))
    private static boolean isWaylandPreferred(boolean original) {
        return original || !BrowserBackendManagerKt.isBrowserDisabled()
            && !BrowserBackendManagerKt.getBrowserBackend().equalsIgnoreCase("none")
            && !BrowserBackendManagerKt.isBrowserAccelerationDisabled();
    }

}
