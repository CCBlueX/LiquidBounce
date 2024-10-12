/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack;
import net.ccbluex.liquidbounce.features.module.modules.combat.ForwardTrack;
import net.ccbluex.liquidbounce.features.module.modules.combat.HitBox;
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam;
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRenderManager {

    @Shadow
    public abstract boolean doRenderEntity(Entity p_doRenderEntity_1_, double p_doRenderEntity_2_, double p_doRenderEntity_4_, double p_doRenderEntity_4_2, float p_doRenderEntity_6_, float p_doRenderEntity_6_2, boolean p_doRenderEntity_8_);

    @Shadow
    public double renderPosX;

    @Shadow
    public double renderPosY;

    @Shadow
    public double renderPosZ;

    @Redirect(method = "renderDebugBoundingBox", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/AxisAlignedBB;", ordinal = 0), require = 1, allow = 1)
    private AxisAlignedBB getEntityBoundingBox(Entity entity) {
        final HitBox hitBox = HitBox.INSTANCE;

        if (!hitBox.handleEvents()) {
            return entity.getEntityBoundingBox();
        }

        float size = hitBox.determineSize(entity);
        return entity.getEntityBoundingBox().expand(size, size, size);
    }

    @Inject(method = "renderEntityStatic", at = @At(value = "HEAD"))
    private void renderEntityStatic(Entity entity, float tickDelta, boolean bool, CallbackInfoReturnable<Boolean> cir) {
        FreeCam.INSTANCE.restoreOriginalPosition();

        if (entity instanceof EntityPlayerSP)
            return;

        Backtrack backtrack = Backtrack.INSTANCE;
        IMixinEntity targetEntity = (IMixinEntity) backtrack.getTarget();

        boolean shouldBacktrackRenderEntity = backtrack.handleEvents() && backtrack.getShouldRender()
                && backtrack.shouldBacktrack() && backtrack.getTarget() == entity;

        if (backtrack.getEspMode().equals("Model")) {
            if (shouldBacktrackRenderEntity && targetEntity != null && targetEntity.getTruePos()) {
                if (entity.ticksExisted == 0) {
                    entity.lastTickPosX = entity.posX;
                    entity.lastTickPosY = entity.posY;
                    entity.lastTickPosZ = entity.posZ;
                }

                double d0 = targetEntity.getTrueX();
                double d1 = targetEntity.getTrueY();
                double d2 = targetEntity.getTrueZ();
                float f = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * tickDelta;
                int i = entity.getBrightnessForRender(tickDelta);
                if (entity.isBurning()) {
                    i = 15728880;
                }

                int j = i % 65536;
                int k = i / 65536;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                // Darker color to differentiate fake player & real player.
                GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
                this.doRenderEntity(entity, d0 - this.renderPosX, d1 - this.renderPosY, d2 - this.renderPosZ, f, tickDelta, bool);
            }
        }

        ForwardTrack forwardTrack = ForwardTrack.INSTANCE;

        if (forwardTrack.handleEvents() && forwardTrack.getEspMode().equals("Model") && !shouldBacktrackRenderEntity) {
            if (entity.ticksExisted == 0) {
                entity.lastTickPosX = entity.posX;
                entity.lastTickPosY = entity.posY;
                entity.lastTickPosZ = entity.posZ;
            }

            Vec3 pos = forwardTrack.usePosition(entity);

            float f = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * tickDelta;
            int i = entity.getBrightnessForRender(tickDelta);
            if (entity.isBurning()) {
                i = 15728880;
            }

            int j = i % 65536;
            int k = i / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
            // Darker color to differentiate fake player & real player.
            GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
            this.doRenderEntity(entity, pos.xCoord - this.renderPosX, pos.yCoord - this.renderPosY, pos.zCoord - this.renderPosZ, f, tickDelta, bool);
        }
    }

    @Inject(method = "renderEntityStatic", at = @At("TAIL"))
    private void injectFreeCam(Entity p_renderEntityStatic_1_, float p_renderEntityStatic_2_, boolean p_renderEntityStatic_3_, CallbackInfoReturnable<Boolean> cir) {
        FreeCam.INSTANCE.useModifiedPosition();
    }
}
