/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack;
import net.ccbluex.liquidbounce.features.module.modules.combat.HitBox;
import net.ccbluex.liquidbounce.injection.implementations.IMixinEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.render.OpenGlHelper;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.Box;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
@SideOnly(Side.CLIENT)
public abstract class MixinEntityRenderDispatcher {

    @Shadow public abstract boolean doRenderEntity(Entity p_doRenderEntity_1_, double p_doRenderEntity_2_, double p_doRenderEntity_4_, double p_doRenderEntity_4_2, float p_doRenderEntity_6_, float p_doRenderEntity_6_2, boolean p_doRenderEntity_8_);

    @Shadow public double cameraX;

    @Shadow public double cameraY;

    @Shadow public double cameraZ;

    @Redirect(method = "renderDebugBoundingBox", at = @At(value = "INVOKE", target="Lnet/minecraft/entity/Entity;getEntityBoundingBox()Lnet/minecraft/util/Box;", ordinal = 0), require = 1, allow = 1)
    private Box getEntityBoundingBox(Entity entity) {
		final HitBox hitBox = HitBox.INSTANCE;

        if (!hitBox.handleEvents()) {
            return entity.getEntityBoundingBox();
        }

        float size = hitBox.determineSize(entity);
        return entity.getEntityBoundingBox().expand(size, size, size);
    }

    @Inject(method = "renderEntityStatic", at = @At(value = "HEAD"))
    private void renderEntityStatic(Entity p_renderEntityStatic_1_, float p_renderEntityStatic_2_, boolean p_renderEntityStatic_3_, CallbackInfoReturnable<Boolean> cir) {
        Backtrack backtrack = Backtrack.INSTANCE;
        IMixinEntity targetEntity = (IMixinEntity) backtrack.getTarget();

        if (backtrack.getEspMode().equals("Player")) {
            if (backtrack.handleEvents() && backtrack.getShouldRender()
                    && backtrack.shouldBacktrack() && backtrack.getTarget() == p_renderEntityStatic_1_) {

                if (targetEntity != null && targetEntity.getTruePos()) {
                    if (p_renderEntityStatic_1_.ticksAlive == 0) {
                        p_renderEntityStatic_1_.prevTickX = p_renderEntityStatic_1_.x;
                        p_renderEntityStatic_1_.prevTickY = p_renderEntityStatic_1_.y;
                        p_renderEntityStatic_1_.prevTickZ = p_renderEntityStatic_1_.z;
                    }

                    double d0 = targetEntity.getTrueX();
                    double d1 = targetEntity.getTrueY();
                    double d2 = targetEntity.getTrueZ();
                    float f = p_renderEntityStatic_1_.prevYaw + (p_renderEntityStatic_1_.rotationYaw - p_renderEntityStatic_1_.prevYaw) * p_renderEntityStatic_2_;
                    int i = p_renderEntityStatic_1_.getBrightnessForRender(p_renderEntityStatic_2_);
                    if (p_renderEntityStatic_1_.isBurning()) {
                        i = 15728880;
                    }

                    int j = i % 65536;
                    int k = i / 65536;
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) j, (float) k);
                    // Darker color to differentiate fake player & real player.
                    GlStateManager.color(0.5F, 0.5F, 0.5F, 1.0F);
                    this.doRenderEntity(p_renderEntityStatic_1_, d0 - this.cameraX, d1 - this.cameraY, d2 - this.cameraZ, f, p_renderEntityStatic_2_, p_renderEntityStatic_3_);
                }
            }
        }
    }
}
