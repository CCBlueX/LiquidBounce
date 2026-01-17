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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.gl;

import com.mojang.blaze3d.opengl.GlDebug;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GlDebug.class)
public abstract class MixinGlDebug {

    /**
     * Adds source information to GL errors.
     */
    @Redirect(method = "printDebugLog", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"), remap = false)
    private void injectAdvancedDebugInfo(Logger logger, String format, Object arg) {
        var exception = new Exception();

        var currState = 0;

        StackTraceElement finalElement = null;

        for (var stackTraceElement : exception.getStackTrace()) {
            if (currState == 0 && stackTraceElement.getClassName().startsWith("org.lwjgl.")) {
                currState = 1;
            } else if (currState == 1 && !stackTraceElement.getClassName().startsWith("org.lwjgl.")) {
                finalElement = stackTraceElement;
                break;
            }
        }

        String locationText;

        if (finalElement != null) {
            locationText = finalElement.getClassName() + '.' + finalElement.getMethodName() + ':' + finalElement.getLineNumber();
        } else {
            locationText = "?";
        }

        logger.info("OpenGL debug message: {} (at {})", arg, locationText);
    }
}
