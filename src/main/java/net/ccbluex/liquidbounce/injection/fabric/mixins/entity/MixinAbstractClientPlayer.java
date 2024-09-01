/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.entity;

import net.ccbluex.liquidbounce.cape.CapeAPI;
import net.ccbluex.liquidbounce.cape.CapeInfo;
import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect;
import net.ccbluex.liquidbounce.features.module.modules.render.NoFOV;
import net.minecraft.client.network.AbstractClientPlayerEntityEntity;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;

@Mixin(AbstractClientPlayerEntity.class)
@SideOnly(Side.CLIENT)
public abstract class MixinAbstractClientPlayerEntity extends MixinPlayerEntity {

    private CapeInfo capeInfo;

    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void getCape(CallbackInfoReturnable<Identifier> callbackInfoReturnable) {
        if (capeInfo == null) {
            CapeAPI.INSTANCE.loadCape(getUniqueID(), newCapeInfo -> {
                capeInfo = newCapeInfo;
                return null;
            });
        }

        if (capeInfo != null && capeInfo.isCapeAvailable()) {
            callbackInfoReturnable.setReturnValue(capeInfo.getResourceLocation());
        }
    }

    @Inject(method = "getFovModifier", at = @At("HEAD"), cancellable = true)
    private void getFovModifier(CallbackInfoReturnable<Float> callbackInfoReturnable) {
        final NoFOV fovModule = NoFOV.INSTANCE;

        if (fovModule.handleEvents()) {
            float newFOV = fovModule.getFov();

            if (!isUsingItem()) {
                callbackInfoReturnable.setReturnValue(newFOV);
                return;
            }

            if (getItemInUse().getItem() != Items.bow) {
                callbackInfoReturnable.setReturnValue(newFOV);
                return;
            }

            int i = getItemInUseDuration();
            float f1 = (float) i / 20f;
            f1 = f1 > 1f ? 1f : f1 * f1;
            newFOV *= 1f - f1 * 0.15f;
            callbackInfoReturnable.setReturnValue(newFOV);
        }
    }

    @Inject(method = "getLocationSkin()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
    private void getSkin(CallbackInfoReturnable<Identifier> callbackInfoReturnable) {
        final NameProtect nameProtect = NameProtect.INSTANCE;

        if (nameProtect.handleEvents() && nameProtect.getSkinProtect()) {
            if (!nameProtect.getAllPlayers() && !Objects.equals(getGameProfile().getName(), mc.player.getGameProfile().getName()))
                return;

            callbackInfoReturnable.setReturnValue(DefaultPlayerSkin.getDefaultSkin(getUniqueID()));
        }
    }
}
