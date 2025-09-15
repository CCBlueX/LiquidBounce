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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.texture;

import net.minecraft.client.texture.PlayerSkinProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

@Mixin(PlayerSkinProvider.class)
public class MixinPlayerSkinProvider {

//    @Redirect(
//            method = "<clinit>",
//            at = @At(
//                    value = "INVOKE",
//                    target = "Lcom/mojang/logging/LogUtils;getLogger()Lorg/slf4j/Logger;"
//            ),
//            remap = false
//    )
//    private static Logger replaceLogger() {
//        Logger original = LoggerFactory.getLogger("net.minecraft.client.texture.PlayerSkinProvider");
//
//        InvocationHandler handler = new InvocationHandler() {
//            @Override
//            public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                if ("warn".equals(method.getName()) && args != null) {
//                    for (Object a : args) {
//                        if (a instanceof Throwable cause) {
//                            while (cause != null) {
//                                if (cause instanceof java.io.IOException && cause.getMessage() != null
//                                        && cause.getMessage().contains("Bad PNG Signature")) {
//
//                                    return null;
//                                }
//                                cause = cause.getCause();
//                            }
//                        }
//                    }
//                }
//                return method.invoke(original, args);
//            }
//        };
//
//        return (Logger) Proxy.newProxyInstance(
//                Logger.class.getClassLoader(),
//                new Class<?>[]{Logger.class},
//                handler
//        );
//    }
}
