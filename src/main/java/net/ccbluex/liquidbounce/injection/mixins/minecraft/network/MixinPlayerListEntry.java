// MixinPlayerListEntry.java
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.mojang.authlib.GameProfile;
import net.ccbluex.liquidbounce.features.module.modules.client.ModuleCapes;
import net.ccbluex.liquidbounce.features.cosmetic.CapeCosmeticsManager;
import net.minecraft.client.MinecraftClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSkinChanger;

@Mixin(PlayerListEntry.class)
public abstract class MixinPlayerListEntry {

    @Shadow
    @Final
    private GameProfile profile;

    @Unique
    private boolean capeTextureLoading = false;
    @Unique
    private Identifier capeTexture = null;

    @ModifyExpressionValue(method = "texturesSupplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;uuidEquals(Ljava/util/UUID;)Z"))
    private static boolean liquid_bounce$allow_custom_skin(boolean original) {
        return original || ModuleSkinChanger.INSTANCE.getRunning();
    }

    @ModifyReturnValue(method = "getSkinTextures", at = @At("RETURN"))
    private SkinTextures liquid_bounce$modifySkinTextures(SkinTextures original) {

        if (HideAppearance.INSTANCE.isDestructed()) {
            return original;
        }

        if (ModuleSkinChanger.INSTANCE.getRunning() &&
                MinecraftClient.getInstance().getGameProfile().equals(this.profile)) {
            var customSkinTextures = ModuleSkinChanger.INSTANCE.getSkinTextures();
            if (customSkinTextures != null) {
                original = customSkinTextures.get();
            }
        }
        if (!ModuleCapes.INSTANCE.getRunning()) {
            return original;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && this.profile.getId().equals(client.player.getGameProfile().getId())) {
            Identifier currentModeCape = ModuleCapes.INSTANCE.getCapeTextureId();
            return createTexturesWithCape(original, currentModeCape);
        }

        if (capeTexture != null) {
            return createTexturesWithCape(original, capeTexture);
        }

        liquid_bounce$fetchCapeTexture();
        return original;
    }

    @Unique
    private void liquid_bounce$fetchCapeTexture() {
        if (capeTextureLoading) return;
        capeTextureLoading = true;
        CapeCosmeticsManager.INSTANCE.loadPlayerCape(this.profile, id -> capeTexture = id);
    }

    @Unique
    private SkinTextures createTexturesWithCape(SkinTextures original, Identifier cape) {
        return new SkinTextures(
                original.texture(),
                original.textureUrl(),
                cape,
                original.elytraTexture(),
                original.model(),
                original.secure()
        );
    }
}
