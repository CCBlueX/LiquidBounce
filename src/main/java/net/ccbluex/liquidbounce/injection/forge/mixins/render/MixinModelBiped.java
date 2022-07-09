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
    public int heldItemRight;

    @Shadow
    public ModelRenderer bipedHead;

    /**
     * Rotations - Head only, Pitch
     * 
     * @see Rotations
     */
    @Inject(method = "setRotationAngles", at = @At(value = "FIELD", target = "Lnet/minecraft/client/model/ModelBiped;swingProgress:F"))
    private void injectRotations(final float limbSwing, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float headPitch, final float scaleFactor, final Entity entityIn, final CallbackInfo callbackInfo)
    {
        if (heldItemRight == 3)
            bipedRightArm.rotateAngleY = 0.0F;

        if (entityIn instanceof EntityPlayer)
        {
            final EntityPlayer player = (EntityPlayer) entityIn;
            final Rotations rotations = (Rotations) LiquidBounce.moduleManager.get(Rotations.class);

            if (rotations.getState() && !rotations.getBodyValue().get() && entityIn.equals(Minecraft.getMinecraft().thePlayer) && rotations.isRotating(EntityPlayerImplKt.wrap(player)))
            {
                bipedHead.rotateAngleY = Rotations.Companion.interpolateIf(rotations.getInterpolateRotationsValue().get(), RotationUtils.lastServerRotation.getYaw() - player.prevRenderYawOffset, RotationUtils.serverRotation.getYaw() - player.renderYawOffset, Minecraft.getMinecraft().timer.renderPartialTicks).toRadians;
                bipedHead.rotateAngleX = Rotations.Companion.interpolateIf(rotations.getInterpolateRotationsValue().get(), RotationUtils.lastServerRotation.getPitch(), RotationUtils.serverRotation.getPitch(), Minecraft.getMinecraft().timer.renderPartialTicks).toRadians;
            }
        }
    }
}
