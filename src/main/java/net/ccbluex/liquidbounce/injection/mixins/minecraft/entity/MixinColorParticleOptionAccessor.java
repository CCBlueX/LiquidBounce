package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.minecraft.core.particles.ColorParticleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ColorParticleOption.class)
public interface MixinColorParticleOptionAccessor {

    @Accessor("color")
    int getColor();

}
