/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.item;

import java.util.Locale;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.combat.TpAura;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.EatAnimation;
import net.ccbluex.liquidbounce.features.module.modules.render.SwingAnimation;
import net.ccbluex.liquidbounce.utils.extensions.MathExtensionKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings(
{
        "WeakerAccess", "MethodMayBeStatic", "DesignForExtension"
})
@Mixin(ItemRenderer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinItemRenderer
{
    @Shadow
    private float prevEquippedProgress;
    @Shadow
    private float equippedProgress;
    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    private ItemStack itemToRender;

    @Shadow
    private int equippedItemSlot;

    @Shadow
    protected abstract void rotateArroundXAndY(float angle, float angleY);

    @Shadow
    protected abstract void setLightMapFromPlayer(AbstractClientPlayer clientPlayer);

    @Shadow
    protected abstract void rotateWithPlayerRotations(EntityPlayerSP entityplayerspIn, float partialTicks);

    @Shadow
    protected abstract void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress);

    @Shadow
    protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer);

    @Shadow
    private void doBlockTransformations()
    {
    }

    @Shadow
    protected abstract void doItemUsedTransformations(float swingProgress);

    @Shadow
    public abstract void renderItem(EntityLivingBase entityIn, ItemStack heldStack, TransformType transform);

    @Shadow
    protected abstract void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress);

    /**
     * @author CCBlueX
     * @reason EatAnimation
     * @see    EatAnimation
     */
    @Overwrite
    private void performDrinking(final AbstractClientPlayer clientPlayer, final float partialTicks)
    {
        final EatAnimation ea = (EatAnimation) LiquidBounce.moduleManager.get(EatAnimation.class);
        final boolean eaState = ea.getState();

        final float interpolatedItemInUse = clientPlayer.getItemInUseCount() - partialTicks + 1.0F;
        final float itemInUseUnfinishedPercentage = interpolatedItemInUse / itemToRender.getMaxItemUseDuration();
        float xTranslation = 0.0f;
        float yTranslation = MathHelper.abs(MathHelper.cos(interpolatedItemInUse / (eaState ? ea.getVerticalSpeedValue().get() : 4.0F) * MathExtensionKt.PI) * (eaState ? ea.getVerticalIntensityValue().get() : 0.1F));

        if (eaState && ea.getHorizontalEnabledValue().get())
            xTranslation = MathHelper.abs(MathHelper.cos(interpolatedItemInUse / ea.getHorizontalSpeedValue().get() * MathExtensionKt.PI) * ea.getHorizontalIntensityValue().get());

        // Don't start shaking animation way too fast
        if (itemInUseUnfinishedPercentage >= (eaState ? ea.getShakeStartTime().get() : 0.8F))
        {
            xTranslation = 0.0F;
            yTranslation = 0.0F;
        }

        GlStateManager.translate(xTranslation, yTranslation, 0.0F);

        final float moveFoodToMouth = 1.0F - (float) StrictMath.pow(itemInUseUnfinishedPercentage, 27.0);
        GlStateManager.translate(moveFoodToMouth * 0.6F, moveFoodToMouth * -0.5F, 0.0F);
        GlStateManager.rotate(moveFoodToMouth * 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(moveFoodToMouth * 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(moveFoodToMouth * 30.0F, 0.0F, 0.0F, 1.0F);
    }

    /**
     * @author eric0210
     * @reason SwingAnimation.BlockAngle
     */
    @ModifyConstant(method = "doBlockTransformations", constant = @Constant(floatValue = -80.0F, ordinal = 0), require = 1)
    private float injectCustomBlockAngle(final float prevBlockAngle)
    {
        final SwingAnimation sa = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);

        return sa.getState() ? -sa.getBlockAngle().get() : -80.0f;
    }

    /**
     * Performs transformations prior to the rendering of a held item in first person.
     *
     * @author Mojang
     * @reason SwingAnimation
     * @see    SwingAnimation
     */
    @Overwrite
    private void transformFirstPersonItem(final float equipProgress, final float swingProgress)
    {
        final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
        final boolean swingAnimationState = swingAnimation.getState();

        final float fixedSwingProgress = swingAnimationState && swingAnimation.getSwingStaticSwingProgressEnabled().get() ? swingAnimation.getSwingStaticSwingProgressProgress().get() : swingProgress;

        final float sq = getAnimationProgress(fixedSwingProgress, true, true);
        final float sqrt = getAnimationProgress(fixedSwingProgress, false, true);

        translate(swingAnimation, fixedSwingProgress);

        GlStateManager.translate(0.0F, equipProgress * -0.6f, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(sq * -20.0f, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(sqrt * -80.0f, 1.0F, 0.0F, 0.0F);

        final float scale = swingAnimationState ? swingAnimation.getSwingScale().get() : 0.4f;
        GlStateManager.scale(scale, scale, scale);
    }

    private float getAnimationProgress(final float swingProgress, final boolean sq, final boolean isSwing)
    {
        final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);
        final float expBoost = swingAnimation.exponentBoost;

        if (sq)
        {
            if (swingAnimation.getState())
            {
                final float newSwingProgress;
                final float exp = (isSwing ? swingAnimation.getSwingSmoothingSqExp() : swingAnimation.getBlockSmoothingSqExp()).get() + expBoost;
                switch ((isSwing ? swingAnimation.getSwingSmoothingSqMode() : swingAnimation.getBlockSmoothingSqMode()).get().toLowerCase(Locale.ENGLISH))
                {
                    case "root":
                        newSwingProgress = (float) StrictMath.pow(swingProgress, 1.0 / exp);
                        break;
                    case "power":
                        newSwingProgress = (float) StrictMath.pow(swingProgress, exp);
                        break;
                    default:
                        newSwingProgress = swingProgress;
                }

                return MathHelper.sin(newSwingProgress * MathExtensionKt.PI);
            }

            return MathHelper.sin(swingProgress * swingProgress * MathExtensionKt.PI);
        }

        if (swingAnimation.getState())
        {
            final float newSwingProgress;
            final float exp = (isSwing ? swingAnimation.getSwingSmoothingSqrtExp() : swingAnimation.getBlockSmoothingSqrtExp()).get() + expBoost;
            switch ((isSwing ? swingAnimation.getSwingSmoothingSqrtMode() : swingAnimation.getBlockSmoothingSqrtMode()).get().toLowerCase(Locale.ENGLISH))
            {
                case "root":
                    newSwingProgress = (float) StrictMath.pow(swingProgress, 1.0 / exp);
                    break;
                case "power":
                    newSwingProgress = (float) StrictMath.pow(swingProgress, exp);
                    break;
                default:
                    newSwingProgress = swingProgress;
            }

            return MathHelper.sin(newSwingProgress * MathExtensionKt.PI);
        }

        return MathHelper.sin(MathHelper.sqrt_float(swingProgress) * MathExtensionKt.PI);
    }

    /**
     * Performs transformations prior to the rendering of a held item in first person.
     *
     * @author Mojang
     * @see    SwingAnimation
     */
    protected void transformFirstPersonItemBlock(final float equipProgress, final float swingProgress, final float equipProgressAffect)
    {
        final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);

        final float sq = getAnimationProgress(swingProgress, true, false);
        final float sqrt = getAnimationProgress(swingProgress, false, false);

        translateBlock(swingAnimation, swingProgress);

        final float xTranslate = equipProgress * (swingAnimation.getEquipProgressInfluenceTranslationEnabled().get() ? -swingAnimation.getEquipProgressInfluenceTranslationAffectnessX().get() : 0.0f);
        final float yTranslate = equipProgress * (swingAnimation.getEquipProgressInfluenceTranslationEnabled().get() ? -swingAnimation.getEquipProgressInfluenceTranslationAffectnessY().get() : -0.6f);
        final float zTranslate = equipProgress * (swingAnimation.getEquipProgressInfluenceTranslationEnabled().get() ? -swingAnimation.getEquipProgressInfluenceTranslationAffectnessZ().get() : 0.0f);

        GlStateManager.translate(xTranslate, yTranslate, zTranslate);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(equipProgressAffect * sq * -20.0f, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(equipProgressAffect * sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(equipProgressAffect * sqrt * -80.0f, 1.0F, 0.0F, 0.0F);

        final float scale = swingAnimation.getState() ? swingAnimation.getBlockScale().get() : 0.4f;

        GlStateManager.scale(scale, scale, scale);
    }

    /**
     * @author CCBlueX
     * @reason SwingAnimation
     * @see    KillAura
     * @see    SwingAnimation
     */
    @Overwrite
    public void renderItemInFirstPerson(final float partialTicks)
    {
        final float interpolatedEquipProgress = prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks;
        final EntityPlayerSP abstractclientplayer = mc.thePlayer;

        final float swingProgress = abstractclientplayer.getSwingProgress(partialTicks);
        final float smoothPitchRotation = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
        final float smoothYawRotation = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;

        rotateArroundXAndY(smoothPitchRotation, smoothYawRotation);
        setLightMapFromPlayer(abstractclientplayer);
        rotateWithPlayerRotations(abstractclientplayer, partialTicks);
        GlStateManager.enableRescaleNormal();
        GlStateManager.pushMatrix();

        float equipProgress = 1.0F - interpolatedEquipProgress;

        if (itemToRender != null)
        {
            final KillAura killAura = (KillAura) LiquidBounce.moduleManager.get(KillAura.class);
            final TpAura tpaura = (TpAura) LiquidBounce.moduleManager.get(TpAura.class);
            final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);

            if (swingAnimation.getState())
                equipProgress *= swingAnimation.getEquipProgressMultiplier().get();

            if (itemToRender.getItem() instanceof ItemMap)
                renderItemMap(abstractclientplayer, smoothPitchRotation, equipProgress, swingProgress);
            else if (abstractclientplayer.getItemInUseCount() > 0 || itemToRender.getItem() instanceof ItemSword && (killAura.getClientSideBlockingStatus() || tpaura.getClientSideBlockingStatus())) // Using Item (eating, drinking, blocking, etc.)
            {
                final EnumAction enumaction = killAura.getClientSideBlockingStatus()/* || tpaura.blockingRenderStatus */ ? EnumAction.BLOCK : itemToRender.getItemUseAction();

                switch (enumaction)
                {
                    case NONE:
                        transformFirstPersonItem(equipProgress, 0.0F);
                        break;
                    case EAT: // Eating food
                    case DRINK: // Drinking potion
                        performDrinking(abstractclientplayer, partialTicks);
                        transformFirstPersonItem(equipProgress, swingProgress);
                        break;
                    case BLOCK:
                        // Sword Blocking

                        if (swingAnimation.getState())
                            applyCustomBlockAnimation(swingAnimation, swingProgress, equipProgress, interpolatedEquipProgress);
                        else
                        {
                            // Default block animation
                            transformFirstPersonItem(equipProgress, swingProgress);
                            doBlockTransformations();
                        }
                        break;
                    case BOW:
                        // Charging bow
                        transformFirstPersonItem(equipProgress, swingProgress);
                        doBowTransformations(partialTicks, abstractclientplayer);
                }
            }
            else if (swingAnimation.getState() && swingAnimation.getSmoothSwing().get()) // Smooth swing
                transformFirstPersonItem(equipProgress, swingProgress);
            else
            {
                doItemUsedTransformations(swingProgress);
                transformFirstPersonItem(equipProgress, swingProgress);
            }

            renderItem(abstractclientplayer, itemToRender, TransformType.FIRST_PERSON);
        }
        else if (!abstractclientplayer.isInvisible()) // Render player arm if player is hold nothing and not invisible
            renderPlayerArm(abstractclientplayer, equipProgress, swingProgress);

        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }

    private void applyCustomBlockAnimation(final SwingAnimation swingAnimation, float swingProgress, final float equipProgress, final float interpolatedEquipProgress)
    {
        if (swingAnimation.getBlockStaticSwingProgressEnabled().get())
            swingProgress = swingAnimation.getBlockStaticSwingProgressProgress().get();

        final float sq = getAnimationProgress(swingProgress, true, false);
        final float sqrt = getAnimationProgress(swingProgress, false, false);
        final float equipProgressAffectness = swingAnimation.getEquipProgressInfluenceSwingProgressAffectAffectness().get() * 0.01f;
        final float equipProgressAffect = swingAnimation.getEquipProgressInfluenceSwingProgressAffectEnabled().get() ? 1 - equipProgressAffectness + interpolatedEquipProgress * equipProgressAffectness : 1;
        final float blockScale = swingAnimation.getBlockScale().get();

        final Boolean equipProgressAffectTranslation = swingAnimation.getEquipProgressInfluenceTranslationEnabled().get();

        final float xEquipProgressTranslationAffectness = -swingAnimation.getEquipProgressInfluenceTranslationAffectnessX().get();
        final float yEquipProgressTranslationAffectness = -swingAnimation.getEquipProgressInfluenceTranslationAffectnessY().get();
        final float zEquipProgressTranslationAffectness = -swingAnimation.getEquipProgressInfluenceTranslationAffectnessZ().get();

        switch (swingAnimation.getBlockAnimationMode().get().toLowerCase(Locale.ENGLISH))
        {
            case "liquidbounce":
            {
                transformFirstPersonItemBlock(equipProgress, swingProgress, equipProgressAffect);
                doBlockTransformations();
                GlStateManager.translate(-0.5f, 0.2F, 0.0F);
                break;
            }
            case "1.8":
            {
                transformFirstPersonItemBlock(equipProgress, 0, 1);
                doBlockTransformations();
                break;
            }
            case "1.7":
            {
                transformFirstPersonItemBlock(equipProgress, swingProgress, equipProgressAffect);
                doBlockTransformations();
                break;
            }
            case "avatar":
            {
                translateBlock(swingAnimation, swingProgress);

                final float xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f);
                final float yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.6f);
                final float zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f);

                GlStateManager.translate(xTranslate, yTranslate, zTranslate);

                GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(equipProgressAffect * sq * -20.0f, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(equipProgressAffect * sqrt * -20.0f, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(equipProgressAffect * sqrt * -40.0f, 1.0F, 0.0F, 0.0F);
                GlStateManager.scale(blockScale, blockScale, blockScale);
                doBlockTransformations();
                break;
            }
            case "sigma":
            {
                transformFirstPersonItemBlock(equipProgress, 0, 1);
                GlStateManager.rotate(equipProgressAffect * -sqrt * 27.5f, -8.0f, -0.0f, 9.0F);
                GlStateManager.rotate(equipProgressAffect * -sqrt * 45, 1.0F, sqrt * 0.5f, -0.0f);
                doBlockTransformations();
                GL11.glTranslated(1.2, 0.3, 0.5);
                GL11.glTranslatef(-1, mc.thePlayer.isSneaking() ? -0.1f : -0.2f, 0.2F);
                GlStateManager.scale(blockScale + 0.6f, blockScale + 0.6f, blockScale + 0.6f);
                break;
            }
            case "push":
            {
                transformFirstPersonItemBlock(equipProgress, 0.0F, 1);
                doBlockTransformations();
                GlStateManager.translate(-0.3f, 0.1f, 0.0f);
                GlStateManager.rotate(equipProgressAffect * sqrt * -25.0f, -8.0f, 0.0F, 9.0f);
                GlStateManager.scale(blockScale + 0.6f, blockScale + 0.6f, blockScale + 0.6f);
                break;
            }
            case "tap":
            {
                translateBlock(swingAnimation, swingProgress);

                final float xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f);
                final float yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.6f);
                final float zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f);

                GlStateManager.translate(xTranslate, yTranslate, zTranslate);

                GlStateManager.rotate(30, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(equipProgressAffect * sqrt * -30.0f, 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(blockScale, blockScale, blockScale);
                doBlockTransformations();
                break;
            }
            case "tap2":
            {
                final float smooth = swingProgress * 0.8f - swingProgress * swingProgress * 0.8f;

                translateBlock(swingAnimation, swingProgress);

                final float xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f);
                final float yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.6f);
                final float zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f);

                GlStateManager.translate(xTranslate, yTranslate, zTranslate);

                GlStateManager.rotate(45.0f, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(equipProgressAffect * smooth * -90.0f, 0.0F, 1.0F, 0.0F);
                GlStateManager.scale(blockScale, blockScale, blockScale);
                doBlockTransformations();
                break;
            }
            case "slide":
            {
                translateBlock(swingAnimation, swingProgress);

                final float xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f) + equipProgressAffect * sqrt * swingAnimation.getBlockAnimationSlidePushX().get() * 0.001f;
                final float yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.4f) + equipProgressAffect * sqrt * swingAnimation.getBlockAnimationSlidePushY().get() * 0.01f;
                final float zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f) + equipProgressAffect * sqrt * swingAnimation.getBlockAnimationSlidePushZ().get() * 0.01f;

                GlStateManager.translate(xTranslate, yTranslate, zTranslate);

                GlStateManager.rotate(45.0f, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(equipProgressAffect * sqrt * swingAnimation.getBlockAnimationSlideAngleY().get(), 0.0f, swingAnimation.getBlockAnimationSlidePosY().get(), 0.0f);
                GlStateManager.rotate(equipProgressAffect * sqrt * swingAnimation.getBlockAnimationSlideAngleZ().get(), 0.0f, 0.0f, swingAnimation.getBlockAnimationSlidePosZ().get());
                GlStateManager.rotate(equipProgressAffect * -sqrt * swingAnimation.getBlockAnimationSlideAngleX().get(), swingAnimation.getBlockAnimationSlidePosX().get(), 0.0f, 0.0f);
                GlStateManager.scale(blockScale, blockScale, blockScale);
                doBlockTransformations();
                break;
            }
            case "exhibobo":
            {
                translateBlock(swingAnimation, swingProgress);

                final float xTranslate = equipProgress * (equipProgressAffectTranslation ? xEquipProgressTranslationAffectness : 0.0f) + equipProgressAffect * sqrt * -swingAnimation.getBlockAnimationExhiPushXPos().get() * 0.001f + equipProgressAffect * -sq * swingAnimation.getBlockAnimationExhiSmoothX().get() * 0.005f;
                final float yTranslate = equipProgress * (equipProgressAffectTranslation ? yEquipProgressTranslationAffectness : -0.2f) + equipProgressAffect * sqrt * swingAnimation.getBlockAnimationExhiPushYPos().get() * 0.001f + equipProgressAffect * -sq * swingAnimation.getBlockAnimationExhiSmoothY().get() * 0.005f;
                final float zTranslate = equipProgress * (equipProgressAffectTranslation ? zEquipProgressTranslationAffectness : 0.0f) + equipProgressAffect * sqrt * swingAnimation.getBlockAnimationExhiPushZPos().get() * 0.001f + equipProgressAffect * -sq * swingAnimation.getBlockAnimationExhiSmoothZ().get() * 0.005f;

                GlStateManager.translate(xTranslate, yTranslate, zTranslate);

                GlStateManager.rotate(45.0f, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(-equipProgressAffect * sqrt * (10 + swingAnimation.getBlockAnimationExhiAngleY().get()), 0.0f, swingAnimation.getBlockAnimationExhiPosY().get(), 0.0f);
                GlStateManager.rotate(-equipProgressAffect * sqrt * (10 + swingAnimation.getBlockAnimationExhiAngleZ().get()), 0.0f, 0.0f, swingAnimation.getBlockAnimationExhiPosZ().get());
                GlStateManager.rotate(-equipProgressAffect * sqrt * (20 + swingAnimation.getBlockAnimationExhiAngleX().get()), swingAnimation.getBlockAnimationExhiPosX().get(), 0.0f, 0.0f);

                GlStateManager.scale(blockScale, blockScale, blockScale);
                doBlockTransformations();
                break;
            }
            case "lucid":
            {
                transformFirstPersonItemBlock(equipProgress, swingProgress, equipProgressAffect);
                doBlockTransformations();

                GL11.glTranslatef(-0.5F, 0.2F, 0.0F);

                // Reverse previous rotations

                GL11.glRotatef(60.0F, 0.0F, 0.0F, 1.0F);
                GL11.glRotatef(10.0F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(50.0F, 0.0F, 1.0F, 0.0F);
                GL11.glTranslatef(-0.05F, mc.thePlayer.isSneaking() ? -0.2F : 0.0F, 0.1F);
                break;
            }
            case "luna":
            {
                transformFirstPersonItemBlock(equipProgress, 0.0F, equipProgressAffect);
                doBlockTransformations();

                GlStateManager.translate(0.0F, 0.4F, 0.3F);
                GlStateManager.rotate(-equipProgressAffect * sqrt * 35.0f, -8.0F, -0.0F, 9.0F);
                GlStateManager.rotate(-equipProgressAffect * sqrt * 10.0F, 1.0F, -0.4F, -0.5F);
                break;
            }
            case "hooded":
            {
                transformFirstPersonItemBlock(equipProgress, swingProgress, equipProgressAffect);
                doBlockTransformations();

                GlStateManager.translate(-0.4F, 0.3F, 0.0F);
                GlStateManager.rotate(-equipProgressAffect * sqrt * 50.0F, 0.0F, 0.0F, 15.0F);
                GlStateManager.rotate(-equipProgressAffect * sqrt * 70.0F, 1.0F, -0.4F, -0.0F);
                GL11.glTranslatef(-0.05F, -0.6F, 0.1F);
                break;
            }
            case "bump":
            {
                transformFirstPersonItem(equipProgress, -0.4f);
                doBlockTransformations();
                GlStateManager.translate(-0.4F, 0.3F, 0.0F);
                GlStateManager.rotate(-equipProgressAffect * sqrt * 67.5F, -35.0F, -0.0F, 15.0F);
                GlStateManager.rotate(-equipProgressAffect * sqrt * 70.0F, 1.0F, -0.4F, -0.0F);
                GL11.glTranslatef(-0.05F, -0.6F, 0.1F);
                break;
            }
            case "slap":
            {
                GL11.glTranslated(-0.1D, 0.0D, 0.0D);
                transformFirstPersonItem(-0.4F, 0.0F);
                GlStateManager.rotate(-sqrt * 20.0f, sqrt * 0.5F, -0.0F, 4.0F);
                GlStateManager.rotate(-sqrt * 30.0F, 1.0F, sqrt * 0.5F, -0.0F);
                doBlockTransformations();
                break;
            }
        }
    }

    private void translate(final SwingAnimation swingAnimation, final float swingProgress)
    {
        if (!swingAnimation.getState())
        {
            GlStateManager.translate(0.56, -0.52, -0.72);
            return;
        }

        final float expBoost = swingAnimation.exponentBoost;
        float exp = swingAnimation.getSwingTranslationRelativeXExp().get() + expBoost;
        float newSwingProgress;
        switch (swingAnimation.getSwingTranslationRelativeXMode().get().toLowerCase(Locale.ENGLISH))
        {
            case "root":
                newSwingProgress = (float) StrictMath.pow(swingProgress, 1 / exp);
                break;
            case "power":
                newSwingProgress = (float) StrictMath.pow(swingProgress, exp);
                break;
            default:
                newSwingProgress = swingProgress;
        }
        double smooth = MathHelper.sin((swingAnimation.getSwingTranslationRelativeXReverse().get() ? 1 - newSwingProgress : newSwingProgress) * MathExtensionKt.PI);
        final double x = 0.56 + swingAnimation.getSwingTranslationAbsoluteX().get() + smooth * swingAnimation.getSwingTranslationRelativeX().get();

        exp = swingAnimation.getSwingTranslationRelativeYExp().get() + expBoost;
        switch (swingAnimation.getSwingTranslationRelativeYMode().get().toLowerCase(Locale.ENGLISH))
        {
            case "root":
                newSwingProgress = (float) StrictMath.pow(swingProgress, 1 / exp);
                break;
            case "power":
                newSwingProgress = (float) StrictMath.pow(swingProgress, exp);
                break;
            default:
                newSwingProgress = swingProgress;
        }
        smooth = MathHelper.sin((swingAnimation.getSwingTranslationRelativeYReverse().get() ? 1 - newSwingProgress : newSwingProgress) * MathExtensionKt.PI);
        final double y = -0.52 + swingAnimation.getSwingTranslationAbsoluteY().get() + smooth * swingAnimation.getSwingTranslationRelativeY().get();

        exp = swingAnimation.getSwingTranslationRelativeZExp().get() + expBoost;
        switch (swingAnimation.getSwingTranslationRelativeZMode().get().toLowerCase(Locale.ENGLISH))
        {
            case "root":
                newSwingProgress = (float) StrictMath.pow(swingProgress, 1 / exp);
                break;
            case "power":
                newSwingProgress = (float) StrictMath.pow(swingProgress, exp);
                break;
            default:
                newSwingProgress = swingProgress;
        }
        smooth = MathHelper.sin((swingAnimation.getSwingTranslationRelativeZReverse().get() ? 1 - newSwingProgress : newSwingProgress) * MathExtensionKt.PI);
        final double z = -0.72 + swingAnimation.getSwingTranslationAbsoluteZ().get() + smooth * swingAnimation.getSwingTranslationRelativeZ().get();

        GlStateManager.translate(x, y, z);
    }

    private void translateBlock(final SwingAnimation swingAnimation, final float swingProgress)
    {
        if (!swingAnimation.getState())
        {
            GlStateManager.translate(0.56, -0.52, -0.72);
            return;
        }

        final float expBoost = swingAnimation.exponentBoost;
        float exp = swingAnimation.getBlockTranslationRelativeXExp().get() + expBoost;
        float newSwingProgress;
        switch (swingAnimation.getBlockTranslationRelativeXMode().get().toLowerCase(Locale.ENGLISH))
        {
            case "root":
                newSwingProgress = (float) StrictMath.pow(swingProgress, 1 / exp);
                break;
            case "power":
                newSwingProgress = (float) StrictMath.pow(swingProgress, exp);
                break;
            default:
                newSwingProgress = swingProgress;
        }
        double smooth = MathHelper.sin((swingAnimation.getBlockTranslationRelativeXReverse().get() ? 1 - newSwingProgress : newSwingProgress) * MathExtensionKt.PI);
        final double x = 0.56 + swingAnimation.getBlockTranslationAbsoluteX().get() + smooth * swingAnimation.getBlockTranslationRelativeX().get();

        exp = swingAnimation.getBlockTranslationRelativeYExp().get() + expBoost;
        switch (swingAnimation.getBlockTranslationRelativeYMode().get().toLowerCase(Locale.ENGLISH))
        {
            case "root":
                newSwingProgress = (float) StrictMath.pow(swingProgress, 1 / exp);
                break;
            case "power":
                newSwingProgress = (float) StrictMath.pow(swingProgress, exp);
                break;
            default:
                newSwingProgress = swingProgress;
        }
        smooth = MathHelper.sin((swingAnimation.getBlockTranslationRelativeYReverse().get() ? 1 - newSwingProgress : newSwingProgress) * MathExtensionKt.PI);
        final double y = -0.52 + swingAnimation.getBlockTranslationAbsoluteY().get() + smooth * swingAnimation.getBlockTranslationRelativeY().get();

        exp = swingAnimation.getBlockTranslationRelativeZExp().get() + expBoost;
        switch (swingAnimation.getBlockTranslationRelativeZMode().get().toLowerCase(Locale.ENGLISH))
        {
            case "root":
                newSwingProgress = (float) StrictMath.pow(swingProgress, 1 / exp);
                break;
            case "power":
                newSwingProgress = (float) StrictMath.pow(swingProgress, exp);
                break;
            default:
                newSwingProgress = swingProgress;
        }
        smooth = MathHelper.sin((swingAnimation.getBlockTranslationRelativeZReverse().get() ? 1 - newSwingProgress : newSwingProgress) * MathExtensionKt.PI);
        final double z = -0.72 + swingAnimation.getBlockTranslationAbsoluteZ().get() + smooth * swingAnimation.getBlockTranslationRelativeZ().get();

        GlStateManager.translate(x, y, z);
    }

    @Inject(method = "renderFireInFirstPerson", at = @At("HEAD"), cancellable = true)
    private void renderFireInFirstPerson(final CallbackInfo callbackInfo)
    {
        final AntiBlind antiBlind = (AntiBlind) LiquidBounce.moduleManager.get(AntiBlind.class);

        if (antiBlind.getState() && antiBlind.getFireEffect().get())
        {
            // Vanilla method
            GlStateManager.color(1.0F, 1.0F, 1.0F, 0.9F);
            GlStateManager.depthFunc(519);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(515);

            callbackInfo.cancel();
        }
    }

    /**
     * @author CCBlueX
     * @reason SwingAnimation EquipProgress Smoothing
     * @see    SwingAnimation
     */
    @Overwrite
    public void updateEquippedItem()
    {
        prevEquippedProgress = equippedProgress;

        final EntityPlayer entityplayer = mc.thePlayer;

        final int currentItem = entityplayer.inventory.currentItem;
        final ItemStack currentItemStack = entityplayer.inventory.getCurrentItem();

        boolean shouldCauseReequipAnimation = false;

        if (itemToRender != null && currentItemStack != null)
        {
            if (!itemToRender.getIsItemStackEqual(currentItemStack))
            {
                if (!itemToRender.getItem().shouldCauseReequipAnimation(itemToRender, currentItemStack, equippedItemSlot != currentItem))
                {
                    itemToRender = currentItemStack;
                    equippedItemSlot = currentItem;
                    return;
                }

                shouldCauseReequipAnimation = true;
            }
        }
        else
            shouldCauseReequipAnimation = itemToRender != null || currentItemStack != null;

        final SwingAnimation swingAnimation = (SwingAnimation) LiquidBounce.moduleManager.get(SwingAnimation.class);

        final float deltaLimit = 0.4F;

        final float unclampedDelta = MathHelper.clamp_float((shouldCauseReequipAnimation ? 0.0F /* Going DOWN */ : 1.0F /* Going UP */) - equippedProgress, -1.0F, 1.0F);
        final float clampedDelta = MathHelper.clamp_float(unclampedDelta, -deltaLimit, deltaLimit);
        final String smoothMode = swingAnimation.getEquipProgressSmoothingModeValue().get().toLowerCase(Locale.ENGLISH);
        final int downSpeed = swingAnimation.getEquipProgressSmoothingDownSpeedValue().get();
        final int upSpeed = swingAnimation.getEquipProgressSmoothingUpSpeedValue().get();
        final float downSpeedMultiplier = swingAnimation.getEquipProgressSmoothingDownSpeedMultiplierValue().get();
        final float upSpeedMultiplier = swingAnimation.getEquipProgressSmoothingUpSpeedMultiplierValue().get();
        final float multiplier = clampedDelta < 0 ? downSpeedMultiplier : upSpeedMultiplier;
        switch (smoothMode)
        {
            case "linear":
                equippedProgress += clampedDelta * multiplier;
                break;

            case "square":
                equippedProgress += Math.signum(clampedDelta) * clampedDelta * clampedDelta * multiplier;
                break;

            case "cube":
                equippedProgress += clampedDelta * clampedDelta * clampedDelta * multiplier;
                break;

            case "quadratic-function":
                equippedProgress += clampedDelta < 0 ? -StrictMath.pow(-clampedDelta, 1.0f + downSpeed * 0.1f) * downSpeedMultiplier : MathHelper.clamp_double(MathHelper.sqrt_double((1.0 - unclampedDelta) / upSpeed), 0.0, 1.0) * upSpeedMultiplier;
                break;

            case "reverse-quadratic-function":
                final double v = 0.001 * StrictMath.pow(10.0, swingAnimation.getEquipProgressSmoothingSpeedModifierValue().get());
                equippedProgress += clampedDelta < 0 ? -MathHelper.clamp_double(MathHelper.sqrt_double((unclampedDelta + 1.0 + v) / downSpeed), 0.0, 1.0) * downSpeedMultiplier : StrictMath.pow(clampedDelta, 1.0f + upSpeed * 0.1f) * upSpeedMultiplier;
                break;

            default:
                equippedProgress += clampedDelta;
                break;
        }

        if (equippedProgress < 0.1F)
        {
            itemToRender = currentItemStack;
            equippedItemSlot = currentItem;

            swingAnimation.swingSpeedBoost = swingAnimation.getSwingSpeedBoostAmount().get();
            swingAnimation.swingProgressEndBoost = swingAnimation.getSwingProgressEndBoostAmount().get();
            swingAnimation.exponentBoost = swingAnimation.getEquipProgressBoostSmoothingExponentAmount().get();
        }
        else if (equippedProgress >= 0.995)
            equippedProgress = 1.0F;

        final int ticks = mc.thePlayer.ticksExisted;

        if (swingAnimation.swingSpeedBoost != 0 && ticks % swingAnimation.getSwingSpeedBoostReturnDelay().get() == 0)
            if (swingAnimation.swingSpeedBoost < 0)
                swingAnimation.swingSpeedBoost++;
            else
                swingAnimation.swingSpeedBoost--;

        if (swingAnimation.swingProgressEndBoost != 0 && ticks % swingAnimation.getSwingProgressEndBoostReturnDelay().get() == 0)
            if (swingAnimation.swingProgressEndBoost < 0)
                swingAnimation.swingProgressEndBoost++;
            else
                swingAnimation.swingProgressEndBoost--;

        if (ticks % swingAnimation.getEquipProgressBoostSmoothingExponentReturnDelay().get() == 0)
            if (swingAnimation.exponentBoost > 0)
                swingAnimation.exponentBoost = Math.max(swingAnimation.exponentBoost - swingAnimation.getEquipProgressBoostSmoothingExponentReturnStep().get(), 0.0f);
            else if (swingAnimation.exponentBoost < 0)
                swingAnimation.exponentBoost = Math.min(swingAnimation.exponentBoost + swingAnimation.getEquipProgressBoostSmoothingExponentReturnStep().get(), 0.0f);
    }
}
