/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ServerSelectionList.class)
public abstract class MixinServerSelectionList extends GuiSlot {

    public MixinServerSelectionList(Minecraft mcIn, int width, int height, int topIn, int bottomIn, int slotHeightIn) {
        super(mcIn, width, height, topIn, bottomIn, slotHeightIn);
    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    protected int getScrollBarX() {
        return this.width - 5;
    }
}
