/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.Chams;
import net.ccbluex.liquidbounce.features.module.modules.render.ItemPhysics;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderEntityItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.util.MathHelper.sin;
import static org.lwjgl.opengl.GL11.*;

@Mixin(RenderEntityItem.class)
public abstract class MixinRenderEntityItem extends Render<EntityItem> {
    protected MixinRenderEntityItem(final RenderManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    @Shadow
    protected abstract int func_177078_a(final ItemStack p0);

    @Inject(method = "doRender", at = @At("HEAD"))
    private void injectChamsPre(CallbackInfo callbackInfo) {
        final Chams chams = Chams.INSTANCE;

        if (chams.handleEvents() && chams.getItems()) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1f, -1000000F);
        }
    }

    @Inject(method = "doRender", at = @At("RETURN"))
    private void injectChamsPost(CallbackInfo callbackInfo) {
        final Chams chams = Chams.INSTANCE;

        if (chams.handleEvents() && chams.getItems()) {
            glPolygonOffset(1f, 1000000F);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }

    /**
     * @author Eclipses
     * @reason Original by FDPClient & Modified by Eclipses
     */
    @Overwrite
    private int func_177077_a(EntityItem itemIn, double p_177077_2_, double p_177077_4_, double p_177077_6_, float p_177077_8_, IBakedModel p_177077_9_) {
        final ItemPhysics itemPhysics = (ItemPhysics) LiquidBounce.INSTANCE.getModuleManager().getModule(ItemPhysics.class);
        ItemStack itemstack = itemIn.getEntityItem();
        Item item = itemstack.getItem();

        if (item == null || itemstack == null) {
            return 0;
        } else {
            boolean flag = p_177077_9_.isGui3d();
            int i = this.func_177078_a(itemstack);
            float f = 0.25F;

            // Cache frequently used values
            float age = (float)itemIn.getAge() + p_177077_8_;
            float hoverStart = itemIn.hoverStart;
            boolean isPhysicsState = itemPhysics.getState();
            float weight = isPhysicsState ? itemPhysics.getWeight().get() : 0.0f;

            float f1 = sin((age / 10.0F + hoverStart)) * 0.1F + 0.1F;
            if (isPhysicsState) {
                f1 = 0.0f;
            }
            float f2 = p_177077_9_.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;

            if (isPhysicsState) {
                GlStateManager.translate((float) p_177077_2_, (float) p_177077_4_ + f1 + f * f2 + (-0.2), (float) p_177077_6_);
            } else {
                // Re-Adjust item rendering position on disable to prevent clipping through blocks.
                GlStateManager.translate((float)p_177077_2_, (float) p_177077_4_ + f1 + f * f2, (float)p_177077_6_);
            }

            if (flag || this.renderManager.options != null) {
                float f3 = (age / 20.0F + hoverStart) * (180F / (float)Math.PI);

                f3 *= itemPhysics.getRotationSpeed().get() * (1.0F + Math.min(age / 100.0F, 1.0F));

                if (isPhysicsState) {
                    if (itemIn.onGround) {
                        GL11.glRotatef(itemIn.rotationYaw, 0.0f, 1.0f, 0.0f);
                        GL11.glRotatef(itemIn.rotationPitch + 90.0f, 1.0f, 0.0f, 0.0f);
                    } else {
                        for (int a = 0; a < 10; ++a) {
                            GL11.glRotatef(f3, weight, weight, 0.0f);
                        }
                    }
                } else {
                    GlStateManager.rotate(f3, 0.0F, 1.0F, 0.0F);
                }
            }

            if (!flag) {
                float f6 = -0.0F * (float)(i - 1) * 0.5F;
                float f4 = -0.0F * (float)(i - 1) * 0.5F;
                float f5 = -0.046875F * (float)(i - 1) * 0.5F;
                GlStateManager.translate(f6, f4, f5);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            return i;
        }
    }
}
