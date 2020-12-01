package net.ccbluex.liquidbounce.features.module.modules.render;

import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

@ModuleInfo(name = "XRayBypass", description = "", category = ModuleCategory.RENDER)
public class XRayBypass extends Module {

    private static final int i = 16;
    private int retardedInt;
    private int retardedInt2;

    private int retardedInt3;

    public void onEnable() {
        for (int var1 = -i; var1 < i; ++var1) {
            for (int var2 = i; var2 > -i; --var2) {
                for (int var3 = -i; var3 < i; ++var3) {
                    this.retardedInt3 = (int) Minecraft.getMinecraft().player.posX + var1;
                    this.retardedInt2 = (int) Minecraft.getMinecraft().player.posY + var2;
                    this.retardedInt = (int) Minecraft.getMinecraft().player.posZ + var3;
                    BlockPos var4 = new BlockPos(this.retardedInt3, this.retardedInt2, this.retardedInt);
                    Block var5 = Minecraft.getMinecraft().world.getBlockState(var4).getBlock();
                    if (Block.getIdFromBlock(var5) == 56) {
                        Minecraft.getMinecraft().playerController.clickBlock(var4, EnumFacing.DOWN);
                    }
                }
            }
        }

        Minecraft.getMinecraft().renderGlobal.loadRenderers();
    }

    public void onDisable() {
        Minecraft.getMinecraft().renderGlobal.loadRenderers();
    }
}
