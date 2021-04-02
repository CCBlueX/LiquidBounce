package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiLevitation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    /**
     * Hook anti levitation module
     */
    @Redirect(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/effect/StatusEffect;)Z"))
    public boolean hookTravelStatusEffect(LivingEntity livingEntity, StatusEffect effect) {
        if ((effect == StatusEffects.LEVITATION || effect == StatusEffects.SLOW_FALLING) &&
                ModuleAntiLevitation.INSTANCE.getEnabled()) {
            livingEntity.fallDistance = 0f;
            return false;
        }

        return livingEntity.hasStatusEffect(effect);
    }

}
