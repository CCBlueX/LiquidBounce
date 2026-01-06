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
