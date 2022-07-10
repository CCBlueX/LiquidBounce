/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import com.mojang.authlib.GameProfile;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.Bobbing;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.At.Shift;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase
{
    @Shadow
    protected int flyToggleTimer;

    @Shadow
    public PlayerCapabilities capabilities;

    @Shadow
    public abstract ItemStack getHeldItem();

    @Shadow
    public abstract GameProfile getGameProfile();

    @Shadow
    protected abstract boolean canTriggerWalking();

    @Shadow
    protected abstract String getSwimSound();

    @Shadow
    public abstract FoodStats getFoodStats();

    @Shadow
    public abstract int getItemInUseDuration();

    @Shadow
    public abstract ItemStack getItemInUse();

    @Shadow
    public abstract boolean isUsingItem();

    @ModifyVariable(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;onGround:Z", ordinal = 0, shift = Shift.BEFORE), ordinal = 0)
    public float injectBobbingCameraIncrementMultiplierYaw(final float f)
    {
        final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);

        if (!bobbing.getState())
            return f;
        return f * bobbing.getCameraIncrementMultiplierYawValue().get();
    }

    @ModifyVariable(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;onGround:Z", ordinal = 1, shift = Shift.BEFORE), ordinal = 1)
    public float injectBobbingCameraIncrementMultiplierPitch(final float f1)
    {
        final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);

        if (!bobbing.getState())
            return f1;
        return f1 * bobbing.getCameraIncrementMultiplierPitchValue().get();
    }

    @Redirect(method = "onLivingUpdate", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/EntityPlayer;onGround:Z"))
    public boolean injectBobbingGroundCheck(final EntityPlayer instance)
    {
        final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);
        return onGround || bobbing.getState() && !bobbing.getCheckGroundValue().get();
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(floatValue = 0.4F, ordinal = 0))
    public float injectBobbingMultiplierYaw(final float _0_4)
    {
        final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);

        if (!bobbing.getState())
            return _0_4;
        return bobbing.getCameraMultiplierYawValue().get();
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(floatValue = 0.8F, ordinal = 0))
    public float injectBobbingMultiplierPitch(final float _0_8)
    {
        final Bobbing bobbing = (Bobbing) LiquidBounce.moduleManager.get(Bobbing.class);

        if (!bobbing.getState())
            return _0_8;
        return bobbing.getCameraMultiplierPitchValue().get();
    }
}
