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

import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.builtins.commonjs.CommonJSRequireBuiltin;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;
import net.ccbluex.liquidbounce.script.api.ScriptJvmTypeSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommonJSRequireBuiltin.class)
public abstract class MixinCommonJSRequireBuiltin extends GlobalBuiltins.JSFileLoadingOperation {

    MixinCommonJSRequireBuiltin(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Inject(method = "require", at = @At("HEAD"), cancellable = true, remap = false)
    private void interceptRequire(JSDynamicObject currentRequire, TruffleString moduleIdentifier, CallbackInfoReturnable<Object> cir) {
        String moduleId = moduleIdentifier.toJavaStringUncached();

        // Check if this is a jvm-types require
        if (moduleId.startsWith(ScriptJvmTypeSupport.JVM_TYPES_PREFIX)) {
            String javaTypePath = moduleId.substring(ScriptJvmTypeSupport.JVM_TYPES_PREFIX.length()).replace("/", ".");
            try {
                // Get the class name for the export
                String className = moduleId.substring(moduleId.lastIndexOf("/") + 1);

                // Create the export object
                JSRealm realm = getRealm();
                JSObject exports = JSOrdinary.create(getContext(), realm);

                // Load the Java type and add it to exports
                Object javaType = realm.getEnv().lookupHostSymbol(javaTypePath);
                JSObject.set(exports, Strings.fromJavaString(className), javaType);

                cir.setReturnValue(exports);
            } catch (Exception e) {
                // Let it fall through to normal error handling
            }
        }
    }

}
