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
	private void revertSwordAnimation(final float limbSwing, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float headPitch, final float scaleFactor, final Entity entityIn, final CallbackInfo callbackInfo)
	{
		if (rightArmPose == ArmPose.BOW_AND_ARROW)
			bipedRightArm.rotateAngleY = 0.0F;

		final Rotations rotations = (Rotations) LiquidBounce.moduleManager.get(Rotations.class);

		if (rotations.getState() && !rotations.getBodyValue().get() && entityIn instanceof EntityPlayer && entityIn.equals(Minecraft.getMinecraft().player))
			bipedHead.rotateAngleX = WMathHelper.toRadians(Rotations.Companion.interpolateIf(rotations.getInterpolateRotationsValue().get(), RotationUtils.lastServerRotation.getPitch(), RotationUtils.serverRotation.getPitch(), Minecraft.getMinecraft().timer.renderPartialTicks));
	}
}
