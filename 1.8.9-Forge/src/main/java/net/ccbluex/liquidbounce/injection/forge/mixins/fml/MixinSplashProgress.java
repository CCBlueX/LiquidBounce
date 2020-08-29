package net.ccbluex.liquidbounce.injection.forge.mixins.fml;

import net.minecraftforge.fml.client.SplashProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.Semaphore;

@Mixin(SplashProgress.class)
public interface MixinSplashProgress {

    @Accessor(value = "mutex")
    public static Semaphore getMutex() {
        throw new IllegalStateException("???");
    }

    @Accessor(value = "done")
    public static boolean isDone() {
        throw new IllegalStateException("???");
    }

    @Accessor(value = "pause")
    public static boolean isPaused() {
        throw new IllegalStateException("???");
    }

}
