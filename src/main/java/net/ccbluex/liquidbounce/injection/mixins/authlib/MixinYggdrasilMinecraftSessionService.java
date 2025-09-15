package net.ccbluex.liquidbounce.injection.mixins.authlib;


import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(YggdrasilMinecraftSessionService.class)
public class MixinYggdrasilMinecraftSessionService {

    @Inject(
            method = "getPropertySignatureState",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void bypassSignature(Property property, CallbackInfoReturnable<SignatureState> cir) {
        cir.setReturnValue(SignatureState.SIGNED);
    }
    @Redirect(
            method = "unpackTextures",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/authlib/yggdrasil/TextureUrlChecker;isAllowedTextureDomain(Ljava/lang/String;)Z"
            ),
            remap = false
    )
    private boolean bypassUrlCheck(String url) {
        return true;
    }
}
