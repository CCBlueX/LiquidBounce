package net.ccbluex.liquidbounce.injection.mixins.truffle;

import java.net.URI;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.js.builtins.commonjs.NpmCompatibleESModuleLoader;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.objects.DefaultESModuleLoader;
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
        if (specifier.startsWith("jvm-types/")) {
            // Return special token to trigger CommonJS fallback
            // This matches the TryCommonJS constant in NpmCompatibleESModuleLoader
            cir.setReturnValue(TryCommonJS);
        }
    }
}
