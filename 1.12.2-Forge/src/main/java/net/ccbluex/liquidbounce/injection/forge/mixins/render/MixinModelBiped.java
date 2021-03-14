/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper;
import net.ccbluex.liquidbounce.features.module.modules.render.Rotations;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBiped.ArmPose;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBiped.class)
@SideOnly(Side.CLIENT)
public class MixinModelBiped
{

	@Shadow
	public ModelRenderer bipedRightArm;

	@Shadow
	public ModelRenderer bipedHead;

	@Shadow
	public ArmPose rightArmPose;

	@Inject(method = "setRotationAngles", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;swingProgress:F"))
	private void revertSwordAnimation(final float p_setRotationAngles_1_, final float p_setRotationAngles_2_, final float p_setRotationAngles_3_, final float p_setRotationAngles_4_, final float p_setRotationAngles_5_, final float p_setRotationAngles_6_, final Entity p_setRotationAngles_7_, final CallbackInfo callbackInfo)
	{
		if (rightArmPose == ArmPose.BOW_AND_ARROW)
			bipedRightArm.rotateAngleY = 0F;

		if (LiquidBounce.moduleManager.get(Rotations.class).getState() && RotationUtils.serverRotation != null && p_setRotationAngles_7_ instanceof EntityPlayer && p_setRotationAngles_7_.equals(Minecraft.getMinecraft().player))
		{
			bipedHead.rotateAngleX = RotationUtils.serverRotation.getPitch() / (180F / WMathHelper.PI);
		}
	}
}
