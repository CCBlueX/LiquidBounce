/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.render;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.Chams;
import net.ccbluex.liquidbounce.features.module.modules.render.ItemPhysics;
import net.minecraft.client.render.block.model.ItemCameraTransforms;
import net.minecraft.client.render.entity.Render;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mojang.blaze3d.platform.GlStateManager.*;
import static net.minecraft.util.math.MathHelper.sin;
import static org.lwjgl.opengl.GL11.*;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer extends Render<ItemEntity> {
    protected MixinItemEntityRenderer(final EntityRenderDispatcher p_i46179_1_) {
        super(p_i46179_1_);
    }

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
     *
     * @reason
     * Original simplified code by FDPClient & Modified by Eclipses:
     * https://github.com/SkidderMC/FDPClient/blob/main/src/main/java/net/ccbluex/liquidbounce/injection/forge/mixins/render/MixinItemEntityRenderer.java
     *
     * Original code from:
     * https://github.com/CreativeMD/ItemPhysic/blob/1.8.9/src/main/java/com/creativemd/itemphysic/physics/ClientPhysic.java
     */
    @Overwrite
    private int func_177077_a(ItemEntity itemIn, double x, double y, double z, float p_177077_8_, IBakedModel ibakedmodel) {
        final ItemPhysics itemPhysics = (ItemPhysics) LiquidBounce.INSTANCE.getModuleManager().getModule(ItemPhysics.class);

        ItemStack itemStack = itemIn.getEntityItem();
        Item item = itemStack.getItem();

        if (item == null || itemStack == null) {
            return 0;
        }

        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        boolean isGui3d = ibakedmodel.isGui3d();
        int count = getItemCount(itemStack);
        float yOffset = 0.25F;

        float age = (float) itemIn.getAge() + p_177077_8_;
        float hoverStart = itemIn.hoverStart;
        boolean isPhysicsState = itemPhysics.getState();
        boolean isRealistic = itemPhysics.getRealistic();
        float weight = isPhysicsState ? itemPhysics.getWeight() : 0.0f;

        float sinValue = sin((age / 10.0F + hoverStart)) * 0.1F + 0.1F;
        if (isPhysicsState) {
            sinValue = 0.0f;
        }
        float scaleY = ibakedmodel.getItemCameraTransforms().getTransform(ItemCameraTransforms.TransformType.GROUND).scale.y;

        if (isPhysicsState) {
            translate((float)x, (float)y, (float)z);
        } else {
            translate((float) x, (float) y + sinValue + yOffset * scaleY, (float) z);
        }

        if (isGui3d) {
            translate(0, 0, -0.08);
        } else {
            translate(0, 0, -0.04);
        }

        if (isGui3d || this.renderManager.options != null) {
            float rotationYaw = (age / 20.0F + hoverStart) * (180F / (float) Math.PI);

            rotationYaw *= itemPhysics.getRotationSpeed() * (1.0F + Math.min(age / 360.0F, 1.0F));

            if (isPhysicsState) {
                if (itemIn.onGround) {
                    GL11.glRotatef(90.0f, 1.0f, 0.0f, 0.0f);
                    if (!isRealistic) {
                        GL11.glRotatef(itemIn.rotationYaw, 0.0f, 0.0f, 1.0f);
                    } else {
                        GL11.glRotatef(itemIn.rotationYaw, 0.0f, 1.0f, 0.6f);
                    }
                } else {
                    for (int a = 0; a < 7; ++a) {
                        GL11.glRotatef(rotationYaw, weight, weight, 1.35f);
                    }
                }
            } else {
                rotate(rotationYaw, 0.0F, 1.0F, 0.0F);
            }
        }

        if (!isGui3d) {
            float offsetX = -0.0F * (float) (count - 1) * 0.5F;
            float offsetY = -0.0F * (float) (count - 1) * 0.5F;
            float offsetZ = -0.09375F * (float) (count - 1) * 0.5F;
            translate(offsetX, offsetY, offsetZ);
        }

        glDisable(GL_CULL_FACE);

        color(1.0F, 1.0F, 1.0F, 1.0F);
        return count;
    }

    private int getItemCount(ItemStack stack) {
        int size = stack.count;

        if (size > 48) {
            return 5;
        } else if (size > 32) {
            return 4;
        } else if (size > 16) {
            return 3;
        } else if (size > 1) {
            return 2;
        }

        return 1;
    }
}
