package net.ccbluex.liquidbounce.injection.forge.mixins.util;

import net.ccbluex.liquidbounce.injection.implementations.IMixinTimer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Timer.class)
public class MixinTimer implements IMixinTimer {
    @Shadow
    public float elapsedPartialTicks;
    @Shadow
    public float renderPartialTicks;
    @Shadow
    public int elapsedTicks;
    private float timerSpeed = 1.0F;
    @Shadow
    private long lastSyncSysClock;
    @Shadow
    private float tickLength;

    /**
     * @author superblaubeere27
     * @reason Impossible to emulate timerSpeed in a different way than this
     */
    @Overwrite
    public void updateTimer() {
        long i = Minecraft.getSystemTime();
        this.elapsedPartialTicks = (i - this.lastSyncSysClock) / this.tickLength * this.timerSpeed;
        this.lastSyncSysClock = i;
        this.renderPartialTicks += this.elapsedPartialTicks;
        this.elapsedTicks = (int) this.renderPartialTicks;
        this.renderPartialTicks -= (float) this.elapsedTicks;
    }

    @Override
    public float getTimerSpeed() {
        return this.timerSpeed;
    }

    @Override
    public void setTimerSpeed(float timerSpeed) {
        this.timerSpeed = timerSpeed;
    }
}
