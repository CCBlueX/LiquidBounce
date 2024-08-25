package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;

@Mixin(NetworkPlayerInfo.class)
public class MixinNetworkPlayerInfo {
    @Shadow
    @Final
    private GameProfile gameProfile;

    @Inject(method = "getLocationSkin", cancellable = true, at = @At("HEAD"))
    private void injectSkinProtect(CallbackInfoReturnable<Identifier> cir) {
        final NameProtect nameProtect = NameProtect.INSTANCE;

        if (nameProtect.handleEvents() && nameProtect.getSkinProtect()) {
            if (nameProtect.getAllPlayers() || Objects.equals(gameProfile.getId(), mc.getSession().getProfile().getId())) {
                cir.setReturnValue(DefaultPlayerSkin.getDefaultSkin(gameProfile.getId()));
                cir.cancel();
            }
        }

    }
}