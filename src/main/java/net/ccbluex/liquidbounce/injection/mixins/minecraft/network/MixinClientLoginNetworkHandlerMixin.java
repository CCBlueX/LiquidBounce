package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleSpoofer;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientLoginNetworkHandler.class)
public class MixinClientLoginNetworkHandlerMixin {

    @Redirect(method = "onSuccess", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ClientBrandRetriever;getClientModName()Ljava/lang/String;"))
    private String getClientModName() {
        var moduleSpoofer = ModuleSpoofer.INSTANCE;
        return moduleSpoofer.clientBrand(ClientBrandRetriever.getClientModName());
    }

}
