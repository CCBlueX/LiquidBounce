/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.combat.HitBox;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderManager.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRenderManager {

    @Redirect(method = "renderDebugBoundingBox", at = @At(value = "INVOKE", target="Lnet/minecraft/client/renderer/RenderGlobal;drawOutlinedBoundingBox(Lnet/minecraft/util/AxisAlignedBB;IIII)V", ordinal = 0), require = 1, allow = 1)
    private void drawHitBox(AxisAlignedBB axisAlignedBB, int red, int green, int blue, int alpha) {
		final HitBox hitBox = HitBox.INSTANCE;
		
        if (hitBox.getState()) {
            final float size = hitBox.getSize();
            axisAlignedBB = axisAlignedBB.expand(size, size, size);
        }
        RenderGlobal.drawOutlinedBoundingBox(axisAlignedBB, red, green, blue, alpha);

    }
}
