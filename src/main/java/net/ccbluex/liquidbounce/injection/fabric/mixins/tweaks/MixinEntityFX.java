package net.ccbluex.liquidbounce.injection.fabric.mixins.tweaks;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Particle.class)
public class MixinParticle {
    // Cache the brightness value
    @Unique
    private static final int BRIGHTNESS_VALUE = 0xF000F0;

    @Redirect(method={"renderParticle"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/particle/Particle;getBrightnessForRender(F)I"))
    private int renderParticle(Particle entityFX, float f) {
        return BRIGHTNESS_VALUE;
    }
}
