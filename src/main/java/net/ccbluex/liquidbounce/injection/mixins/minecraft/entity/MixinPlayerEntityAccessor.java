package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerEntity.class)
public interface MixinPlayerEntityAccessor {
    /*
        * Used to get the TrackedData of the arm
     */
    @Accessor("MAIN_ARM")
    TrackedData<Byte> getTrackedMainArm();
}
