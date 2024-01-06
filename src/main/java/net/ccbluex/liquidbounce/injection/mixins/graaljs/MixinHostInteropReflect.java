/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

package net.ccbluex.liquidbounce.injection.mixins.graaljs;

import net.ccbluex.liquidbounce.utils.mappings.McMappings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * @author lit
 */
@Mixin(targets = "com/oracle/truffle/host/HostInteropReflect")
public class MixinHostInteropReflect {

    private static Class<?> interopCurrentClass;

    @ModifyVariable(method = "findField", at = @At("HEAD"), argsOnly = true, index = 1, remap = false)
    private static Class<?> getFieldClass(Class<?> clazz) {
        interopCurrentClass = clazz;
        return clazz;
    }

    @ModifyVariable(method = "findField", at = @At("HEAD"), argsOnly = true, index = 2, remap = false)
    private static String remapFieldName(String value) {
        return McMappings.INSTANCE.remapField(interopCurrentClass, value, true);
    }

    @ModifyVariable(method = "findMethod", at = @At("HEAD"), argsOnly = true, index = 1, remap = false)
    private static Class<?> getMethodClass(Class<?> clazz) {
        interopCurrentClass = clazz;
        return clazz;
    }

    @ModifyVariable(method = "findMethod", at = @At("HEAD"), argsOnly = true, index = 2, remap = false)
    private static String remapMethodName(String value) {
        return McMappings.INSTANCE.remapMethod(interopCurrentClass, value, true);
    }

}
