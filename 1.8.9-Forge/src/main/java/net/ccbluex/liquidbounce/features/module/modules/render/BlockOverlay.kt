package net.ccbluex.liquidbounce.features.module.modules.render;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render2DEvent;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;

import java.awt.Color;

import static net.minecraft.client.renderer.GlStateManager.*;
import static net.minecraft.client.renderer.RenderGlobal.drawSelectionBoundingBox;
import static org.lwjgl.opengl.GL11.*;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "BlockOverlay", description = "Allows you to change the design of the block overlay.", category = ModuleCategory.RENDER)
public class BlockOverlay extends Module {

    private final IntegerValue colorRedValue = new IntegerValue("R", 68, 0, 255);
    private final IntegerValue colorGreenValue = new IntegerValue("G", 117, 0, 255);
    private final IntegerValue colorBlueValue = new IntegerValue("B", 255, 0, 255);
    private final BoolValue colorRainbow = new BoolValue("Rainbow", false);

    public final BoolValue infoValue = new BoolValue("Info", false);

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null &&
                mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() != null &&
                BlockUtils.canBeClicked(mc.objectMouseOver.getBlockPos())) {
            float partialTicks = event.getPartialTicks();

            enableBlend();
            tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
            final Color color = colorRainbow.get() ? ColorUtils.rainbow(0.4F) : new Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), (int) (0.4F * 255));
            RenderUtils.glColor(color);
            glLineWidth(2F);
            disableTexture2D();

            depthMask(false);
            final BlockPos blockPos = mc.objectMouseOver.getBlockPos();
            final Block block = mc.theWorld.getBlockState(blockPos).getBlock();

            block.setBlockBoundsBasedOnState(mc.theWorld, blockPos);
            final double x = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * (double) partialTicks;
            final double y = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * (double) partialTicks;
            final double z = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * (double) partialTicks;

            final AxisAlignedBB axisAlignedBB = block.getSelectedBoundingBox(mc.theWorld, blockPos)
                    .expand(0.0020000000949949026D, 0.0020000000949949026D, 0.0020000000949949026D)
                    .offset(-x, -y, -z);

            drawSelectionBoundingBox(axisAlignedBB);
            RenderUtils.drawFilledBox(axisAlignedBB);

            depthMask(true);
            enableTexture2D();

            disableBlend();
            GlStateManager.resetColor();
        }
    }

    @EventTarget
    public void onRender2D(final Render2DEvent event) {
        if(mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null && mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock() != null && BlockUtils.canBeClicked(mc.objectMouseOver.getBlockPos()) && infoValue.get()) {
            final BlockPos blockPos = mc.objectMouseOver.getBlockPos();
            final Block block = mc.theWorld.getBlockState(blockPos).getBlock();

            if(mc.theWorld.getWorldBorder().contains(blockPos)) {
                final String info = block.getLocalizedName() + " §7ID: " + Block.getIdFromBlock(block);
                final ScaledResolution scaledResolution = new ScaledResolution(mc);
                RenderUtils.drawBorderedRect((scaledResolution.getScaledWidth() / 2) - 2, (scaledResolution.getScaledHeight() / 2) + 5, (scaledResolution.getScaledWidth() / 2) + Fonts.font40.getStringWidth(info) + 2, (scaledResolution.getScaledHeight() / 2) + 16, 3, Color.BLACK.getRGB(), Color.BLACK.getRGB());
                GlStateManager.resetColor();
                Fonts.font40.drawString(info, scaledResolution.getScaledWidth() / 2, scaledResolution.getScaledHeight() / 2 + 7, Color.WHITE.getRGB());
            }
        }
    }
}
