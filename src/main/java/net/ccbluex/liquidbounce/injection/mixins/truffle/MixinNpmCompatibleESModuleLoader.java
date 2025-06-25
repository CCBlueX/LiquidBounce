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
 *
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.truffle;

import java.net.URI;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.js.builtins.commonjs.NpmCompatibleESModuleLoader;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader;
import net.ccbluex.liquidbounce.script.api.ScriptJvmTypeSupport;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NpmCompatibleESModuleLoader.class)
public abstract class MixinNpmCompatibleESModuleLoader extends DefaultESModuleLoader {

    protected MixinNpmCompatibleESModuleLoader(JSRealm realm) {
        super(realm);
    }

    @Final
    @Shadow(remap = false)
    private static URI TryCommonJS;

    // Inject into esmResolve method to intercept jvm-types imports
    @Inject(method = "esmResolve", at = @At("HEAD"), cancellable = true, remap = false)
    private void interceptJvmTypesResolve(String specifier, URI parentURL, TruffleLanguage.Env env, CallbackInfoReturnable<URI> cir) {
        // Check if this is a jvm-types import
        if (specifier.startsWith(ScriptJvmTypeSupport.JVM_TYPES_PREFIX)) {
            // Return special token to trigger CommonJS fallback
            // This matches the TryCommonJS constant in NpmCompatibleESModuleLoader
            cir.setReturnValue(TryCommonJS);
        }
    }
}
