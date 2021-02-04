package net.ccbluex.liquidbounce.injection.forge.mixins.resources;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.misc.NameProtect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkinManager.class)
public class MixinSkinManager
{

	@Inject(method = "loadSkinFromCache", cancellable = true, at = @At("HEAD"))
	private void injectSkinProtect(final GameProfile gameProfile, final CallbackInfoReturnable<Map<Type, MinecraftProfileTexture>> cir)
	{
		if (gameProfile == null)
			return;

		final NameProtect nameProtect = (NameProtect) LiquidBounce.moduleManager.get(NameProtect.class);

		if (nameProtect.getState() && nameProtect.getSkinProtectValue().get())
			if (nameProtect.getAllPlayersValue().get() || Objects.equals(gameProfile.getId(), Minecraft.getMinecraft().getSession().getProfile().getId())) {
				cir.setReturnValue(new EnumMap<>(Type.class));
				cir.cancel();
			}
	}

}
