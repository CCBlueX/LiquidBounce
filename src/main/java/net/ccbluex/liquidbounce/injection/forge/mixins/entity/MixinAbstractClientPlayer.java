/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import java.util.Objects;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.cape.CapeAPI;
import net.ccbluex.liquidbounce.cape.CapeInfo;
import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect;
import net.ccbluex.liquidbounce.features.module.modules.render.NoFOV;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinAbstractClientPlayer extends MixinEntityPlayer
{
    private CapeInfo capeInfo;

    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void injectLiquidBounceCape(final CallbackInfoReturnable<? super ResourceLocation> callbackInfoReturnable)
    {
        // Custom Cape
        if (!CapeAPI.INSTANCE.hasCapeService())
            return;

        if (capeInfo == null)
            capeInfo = CapeAPI.INSTANCE.loadCape(getUniqueID());

        if (capeInfo != null && capeInfo.isCapeAvailable())
            callbackInfoReturnable.setReturnValue(capeInfo.getResourceLocation());
    }

    @Inject(method = "getFovModifier", at = @At("HEAD"), cancellable = true)
    private void injectNoFov(final CallbackInfoReturnable<? super Float> callbackInfoReturnable)
    {
        // NoFOV
        final NoFOV fovModule = (NoFOV) LiquidBounce.moduleManager.get(NoFOV.class);

        if (fovModule.getState())
        {
            float newFOV = fovModule.getFovValue().get();

            if (!isUsingItem())
            {
                callbackInfoReturnable.setReturnValue(newFOV);
                return;
            }

            if (getItemInUse().getItem() != Items.bow)
            {
                callbackInfoReturnable.setReturnValue(newFOV);
                return;
            }

            final int i = getItemInUseDuration();
            float f1 = i * 0.05f;
            f1 = f1 > 1.0f ? 1.0f : f1 * f1;
            newFOV *= 1.0f - f1 * 0.15f;
            callbackInfoReturnable.setReturnValue(newFOV);
        }
    }

    @Inject(method = "getLocationSkin()Lnet/minecraft/util/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void injectSkinProtect(final CallbackInfoReturnable<? super ResourceLocation> callbackInfoReturnable)
    {
        // NameProtect SkinProtect
        final NameProtect nameProtect = (NameProtect) LiquidBounce.moduleManager.get(NameProtect.class);

        if (nameProtect.getState() && nameProtect.getSkinProtectValue().get())
        {
            if (!nameProtect.getAllPlayerEnabledValue().get() && !Objects.equals(getGameProfile().getName(), Minecraft.getMinecraft().thePlayer.getGameProfile().getName()))
                return;

            callbackInfoReturnable.setReturnValue(DefaultPlayerSkin.getDefaultSkin(getUniqueID()));
        }
    }
}
