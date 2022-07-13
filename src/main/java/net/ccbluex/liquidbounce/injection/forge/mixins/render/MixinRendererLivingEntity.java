/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.Locale;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.Rotation;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.extensions.EntityExtensionKt;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.RGBAColorValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import co.uk.hexeption.utils.OutlineUtils;

@Mixin(RendererLivingEntity.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRendererLivingEntity extends MixinRender
{
    @Shadow
    @Final
    private static DynamicTexture textureBrightness;

    @Shadow
    protected final boolean renderOutlines = false;

    @Shadow
    protected ModelBase mainModel;

    @Shadow
    protected FloatBuffer brightnessBuffer;

    @Shadow
    protected abstract float interpolateRotation(float prevRotation, float rotation, float partialTicks);

    @Shadow
    protected abstract float getSwingProgress(EntityLivingBase livingBase, float partialTicks);

    @Shadow
    protected abstract void renderLayers(EntityLivingBase entitylivingbaseIn, float p_177093_2_, float p_177093_3_, float partialTicks, float p_177093_5_, float p_177093_6_, float p_177093_7_, float p_177093_8_);

    @Shadow
    protected abstract void renderLivingAt(EntityLivingBase entityLivingBaseIn, double x, double y, double z);

    @Shadow
    protected abstract void rotateCorpse(EntityLivingBase bat, float p_77043_2_, float p_77043_3_, float partialTicks);

    @Shadow
    protected abstract boolean setScoreTeamColor(EntityLivingBase entityLivingBaseIn);

    @Shadow
    protected abstract void unsetScoreTeamColor();

    @Shadow
    protected abstract boolean setDoRenderBrightness(EntityLivingBase entityLivingBaseIn, float partialTicks);

    @Shadow
    protected abstract void unsetBrightness();

    @Shadow
    protected abstract float handleRotationFloat(EntityLivingBase livingBase, float partialTicks);

    @Shadow
    protected abstract void preRenderCallback(EntityLivingBase entitylivingbaseIn, float partialTickTime);

    @Shadow
    protected abstract int getColorMultiplier(EntityLivingBase entitylivingbaseIn, float lightBrightness, float partialTickTime);

    @Shadow
    protected abstract <T extends EntityLivingBase> void renderModel(T entitylivingbaseIn, float p_77036_2_, float p_77036_3_, float p_77036_4_, float p_77036_5_, float p_77036_6_, float scaleFactor);

    private int getRotationsFlags()
    {
        final EntityPlayerSP thePlayer = Minecraft.getMinecraft().thePlayer;
        int ret = 0;
        if (doRender_entity instanceof EntityPlayer && doRender_entity == thePlayer && doRender_entityYaw != 0 && rotations.getState() && rotations.getBodyValue().get() && rotations.isRotating(thePlayer))
            ret |= 0b1;
        if (rotations.getInterpolateRotationsValue().get())
            ret |= 0b10;
        return ret;
    }


    /* doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V */
    private EntityLivingBase doRender_entity;
    private float doRender_entityYaw;
    private float doRender_partialTicks;
    private Rotations rotations;
    private ESP esp;
    private Chams chams;
    private Module nameTags;

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"))
    private <T extends EntityLivingBase> void captureVariables(final T entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks, final CallbackInfo ci)
    {
        // WORKAROUND: Capture variables
        doRender_entity = entity;
        doRender_entityYaw = entityYaw;
        doRender_partialTicks = partialTicks;

        rotations = (Rotations) LiquidBounce.moduleManager.get(Rotations.class);
        esp = (ESP) LiquidBounce.moduleManager.get(ESP.class);
        chams = (Chams) LiquidBounce.moduleManager.get(Chams.class);
        nameTags = LiquidBounce.moduleManager.get(NameTags.class);
    }

    @SuppressWarnings("InvalidInjectorMethodSignature")
    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderModel(Lnet/minecraft/entity/EntityLivingBase;FFFFFF)V", shift = Shift.AFTER, ordinal = 1), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void drawESP(final EntityLivingBase entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks, final CallbackInfo ci, final float f, final float f1, final float f2, final float f7, final float f8, final float f4, final float f5, final float f6, final boolean flag)
    {
        final String mode = esp.getModeValue().get();
        if (esp.getState() && ("Fill".equalsIgnoreCase(mode) || "CSGO".equalsIgnoreCase(mode)) && EntityExtensionKt.isSelected(entity, false))
        {
            final EntityRenderer entityRenderer = Minecraft.getMinecraft().entityRenderer;
            entityRenderer.disableLightmap();
            RenderUtils.glColor(esp.getColor(entity));
            GL11.glPushMatrix();
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_TEXTURE_2D);

            final boolean disableLighting = !"CSGO".equalsIgnoreCase(mode);
            if (disableLighting)
                RenderHelper.disableStandardItemLighting();

            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1.0f, -3900000.0f);
            renderModel(entity, f6, f5, f8, f2, f7, 0.0625F);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            if (disableLighting)
            {
                GlStateManager.enableLighting();
                GlStateManager.enableLight(0);
                GlStateManager.enableLight(1);
                GlStateManager.enableColorMaterial();
            }
            GL11.glPopMatrix();
            entityRenderer.disableLightmap();
            RenderUtils.glColor(-1);
        }
    }

    @ModifyVariable(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderLivingAt(Lnet/minecraft/entity/EntityLivingBase;DDD)V"), ordinal = 2)
    private float modifyInterpolatedYaw(final float yawIn)
    {
        final int flags = getRotationsFlags();
        if ((flags & 0b1) != 0)
        {
            final Rotation lastServerRotation = RotationUtils.lastServerRotation;
            final Rotation serverRotation = RotationUtils.serverRotation;
            final float lastYaw = lastServerRotation.getYaw();
            final float yaw = serverRotation.getYaw();

            return (flags & 0b10) != 0 ? interpolateRotation(lastYaw, yaw, doRender_partialTicks) : yaw; // Body Rotation
        }
        return yawIn;
    }

    @ModifyVariable(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderLivingAt(Lnet/minecraft/entity/EntityLivingBase;DDD)V"), ordinal = 5)
    private float modifyInterpolatedPitch(final float pitchIn)
    {
        final int flags = getRotationsFlags();
        if ((flags & 0b1) != 0)
        {
            final Rotation lastServerRotation = RotationUtils.lastServerRotation;
            final Rotation serverRotation = RotationUtils.serverRotation;
            final float lastPitch = lastServerRotation.getPitch();
            final float pitch = serverRotation.getPitch();

            return (flags & 0b10) != 0 ? interpolateRotation(lastPitch, pitch, doRender_partialTicks) : pitch; // Pitch
        }
        return pitchIn;
    }

    @ModifyVariable(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderLivingAt(Lnet/minecraft/entity/EntityLivingBase;DDD)V"), ordinal = 4)
    private float modifyYawDelta(final float yawDelta)
    {
        if ((getRotationsFlags() & 0b1) != 0)
            return 0;
        return yawDelta;
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;pushMatrix()V", shift = Shift.BEFORE, ordinal = 0))
    private <T extends EntityLivingBase> void chamsPre(final T entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks, final CallbackInfo ci)
    {
        if (chams.getState() && chams.getTargetsValue().get() && EntityExtensionKt.isSelected(entity, false))
        {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1.0F, -1000000.0F);
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "NEW", target = "Lnet/minecraftforge/client/event/RenderLivingEvent$Post;<init>(Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/client/renderer/entity/RendererLivingEntity;DDD)V", shift = Shift.BEFORE, ordinal = 0))
    private <T extends EntityLivingBase> void chamsPost(final T entity, final double x, final double y, final double z, final float entityYaw, final float partialTicks, final CallbackInfo ci)
    {
        if (chams.getState() && chams.getTargetsValue().get() && EntityExtensionKt.isSelected(entity, false))
        {
            GL11.glPolygonOffset(1.0F, 1000000.0F);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }

    /* canRenderName */

    @Inject(method = "canRenderName(Lnet/minecraft/entity/EntityLivingBase;)Z", at = @At("HEAD"), cancellable = true)
    private <T extends EntityLivingBase> void canRenderName(final T entity, final CallbackInfoReturnable<? super Boolean> callbackInfoReturnable)
    {
        if (!ESP.Companion.getRenderNameTags() || nameTags.getState() && EntityExtensionKt.isSelected(entity, false))
            callbackInfoReturnable.setReturnValue(false);
    }

    /* renderModel */

    private EntityLivingBase renderModel_entitylivingbaseIn;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private <T extends EntityLivingBase> void captureVariables(final T entitylivingbaseIn, final float p_77036_2_, final float p_77036_3_, final float p_77036_4_, final float p_77036_5_, final float p_77036_6_, final float scaleFactor, final CallbackInfo ci)
    {
        // WORKAROUND: Capture variables
        renderModel_entitylivingbaseIn = entitylivingbaseIn;
    }

    @Redirect(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInvisibleToPlayer(Lnet/minecraft/entity/player/EntityPlayer;)Z"))
    private boolean applyTrueSight(final EntityLivingBase instance, final EntityPlayer entityPlayer)
    {
        final TrueSight trueSight = (TrueSight) LiquidBounce.moduleManager.get(TrueSight.class);
        return !(trueSight.getState() && trueSight.getEntitiesValue().get()) && instance.isInvisibleToPlayer(entityPlayer);
    }

    @ModifyArg(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V", ordinal = 0), index = 3)
    private float applyTrueSightAlpha(final float colorAlpha)
    {
        final TrueSight trueSight = (TrueSight) LiquidBounce.moduleManager.get(TrueSight.class);
        if (trueSight.getState() && trueSight.getEntitiesValue().get())
            return trueSight.getEntitiesAlphaValue().get();
        return colorAlpha;
    }

    @ModifyVariable(method = "renderModel", at = @At(value = "LOAD", ordinal = 0), ordinal = 1, require = 1)
    private boolean applyTrueSightFakePlayers(final boolean value)
    {
        return value || renderModel_entitylivingbaseIn != null && renderModel_entitylivingbaseIn.getEntityId() < 0;
    }

    @Inject(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V", shift = Shift.BEFORE))
    protected <T extends EntityLivingBase> void renderESP(final T entitylivingbaseIn, final float p_77036_2_, final float p_77036_3_, final float p_77036_4_, final float p_77036_5_, final float p_77036_6_, final float scaleFactor, final CallbackInfo ci)
    {
        final ESP esp = (ESP) LiquidBounce.moduleManager.get(ESP.class);
        if (esp.getState() && EntityExtensionKt.isSelected(entitylivingbaseIn, false))
        {
            final Minecraft mc = Minecraft.getMinecraft();
            final boolean fancyGraphics = mc.gameSettings.fancyGraphics;
            mc.gameSettings.fancyGraphics = false;

            final float gamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 100000.0F;

            switch (esp.getModeValue().get().toLowerCase(Locale.ENGLISH))
            {
                case "wireframe":
                    GL11.glPushMatrix();
                    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                    GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glDisable(GL11.GL_DEPTH_TEST);
                    GL11.glEnable(GL11.GL_LINE_SMOOTH);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    RenderUtils.glColor(esp.getColor(entitylivingbaseIn));
                    GL11.glLineWidth(esp.getModeWireFrameWidth().get());
                    mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
                    GL11.glPopAttrib();
                    GL11.glPopMatrix();
                    break;

                case "outline":
                    ClientUtils.disableFastRender();
                    GlStateManager.resetColor();

                    final int color = esp.getColor(entitylivingbaseIn);
                    RenderUtils.glColor(color);
                    OutlineUtils.renderOne(esp.getModeOutlineWidth().get());
                    mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
                    RenderUtils.glColor(color);
                    OutlineUtils.renderTwo();
                    mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
                    RenderUtils.glColor(color);
                    OutlineUtils.renderThree();
                    mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
                    RenderUtils.glColor(color);
                    OutlineUtils.renderFour(color);
                    mainModel.render(entitylivingbaseIn, p_77036_2_, p_77036_3_, p_77036_4_, p_77036_5_, p_77036_6_, scaleFactor);
                    RenderUtils.glColor(color);
                    OutlineUtils.renderFive();
                    RenderUtils.glColor(Color.WHITE);
            }
            mc.gameSettings.fancyGraphics = fancyGraphics;
            mc.gameSettings.gammaSetting = gamma;
        }
    }

    /**
     * @author eric0210
     * @reason HurtCam: Custom Hurt Effect Color
     */
    @Inject(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3, shift = Shift.AFTER), remap = false)
    private <T extends EntityLivingBase> void setBrightness(final T entitylivingbaseIn, final float partialTicks, final boolean combineTextures, final CallbackInfoReturnable<Boolean> cir)
    {
        final HurtCam hurtCam = (HurtCam) LiquidBounce.moduleManager.get(HurtCam.class);
        if (hurtCam.getState() && hurtCam.getCustomHurtEffectEnabledValue().get())
        {
            final RGBAColorValue color = hurtCam.getCustomHurtEffectColorValue();
            brightnessBuffer.clear(); // Clear the buffer before re-fill

            brightnessBuffer.put(color.getRed() / 255.0F);
            brightnessBuffer.put(color.getGreen() / 255.0F);
            brightnessBuffer.put(color.getBlue() / 255.0F);
            brightnessBuffer.put(color.getAlpha() / 255.0F);
        }
    }
}
