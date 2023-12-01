/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.features.module.modules.render.Chams;
import net.minecraft.client.renderer.tileentity.TileEntityChestRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(TileEntityChestRenderer.class)
public class MixinTileEntityChestRenderer {

    @Inject(method = "renderTileEntityAt", at = @At("HEAD"))
    private void injectChamsPre(CallbackInfo callbackInfo) {
        final Chams chams = Chams.INSTANCE;

        if (chams.handleEvents() && chams.getChests()) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1f, -1000000F);
        }
    }

    @Inject(method = "renderTileEntityAt", at = @At("RETURN"))
    private void injectChamsPost(CallbackInfo callbackInfo) {
        final Chams chams = Chams.INSTANCE;

        if (chams.handleEvents() && chams.getChests()) {
            glPolygonOffset(1f, 1000000F);
            glDisable(GL_POLYGON_OFFSET_FILL);
        }
    }
}
