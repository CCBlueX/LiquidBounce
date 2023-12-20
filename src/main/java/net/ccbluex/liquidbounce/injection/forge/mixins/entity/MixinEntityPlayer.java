/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.module.modules.combat.KeepSprint;
import net.ccbluex.liquidbounce.utils.CooldownHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;

@Mixin(EntityPlayer.class)
public abstract class MixinEntityPlayer extends MixinEntityLivingBase {

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
    protected int flyToggleTimer;

    @Shadow
    public PlayerCapabilities capabilities;

    @Shadow
    public abstract int getItemInUseDuration();

    @Shadow
    public abstract ItemStack getItemInUse();

    @Shadow
    public abstract boolean isUsingItem();

    @Shadow
    public InventoryPlayer inventory;
    private ItemStack cooldownStack;
    private int cooldownStackSlot;

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void injectCooldown(final CallbackInfo callbackInfo) {
        if (getGameProfile() == mc.thePlayer.getGameProfile()) {
            CooldownHelper.INSTANCE.incrementLastAttackedTicks();
            CooldownHelper.INSTANCE.updateGenericAttackSpeed(getHeldItem());

            if (cooldownStackSlot != inventory.currentItem || !ItemStack.areItemStacksEqual(cooldownStack, getHeldItem())) {
                CooldownHelper.INSTANCE.resetLastAttackedTicks();
            }

            cooldownStack = getHeldItem();
            cooldownStackSlot = inventory.currentItem;
        }
    }

    @ModifyConstant(method = "attackTargetEntityWithCurrentItem", constant = @Constant(doubleValue = 0.6))
    private double injectKeepSprintA(double constant) {
        return KeepSprint.INSTANCE.getState() ? KeepSprint.INSTANCE.getMotionAfterAttack() : constant;
    }

    @Redirect(method = "attackTargetEntityWithCurrentItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setSprinting(Z)V"))
    private void injectKeepSprintB(EntityPlayer instance, boolean sprint) {
        if (!KeepSprint.INSTANCE.getState()) {
            instance.setSprinting(sprint);
        }
    }

}
