package net.ccbluex.liquidbounce.injection.mixins.graaljs;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "com/oracle/truffle/api/TruffleLanguage")
public class MixinTruffleLanguage {

    /**
     * @author Senk Ju
     * @reason Prevent GraalVM from blocking multi threaded access to resources
     */
    @Overwrite(remap = false)
    protected boolean isThreadAccessAllowed(Thread thread, boolean singleThreaded) {
        return true;
    }
}
