package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ServerListEntryLanScan;
import net.minecraft.client.resources.I18n;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerListEntryLanScan.class)
public class MixinServerListEntryLanScan
{
    @Shadow
    @Final
    private Minecraft mc;

    /**
     * @author eric0210
     * @reason LiquidBounce advertisement (xd)
     */
    @Overwrite
    public void drawEntry(final int slotIndex, final int x, final int y, final int listWidth, final int slotHeight, final int mouseX, final int mouseY, final boolean isSelected)
    {
        final int middleWidth = mc.currentScreen.width >> 1;
        final int ypos = y + (slotHeight >> 1) - (mc.fontRendererObj.FONT_HEIGHT >> 1);

        mc.fontRendererObj.drawString(I18n.format("lanServer.scanning"), middleWidth - (mc.fontRendererObj.getStringWidth(I18n.format("lanServer.scanning")) >> 1), ypos, 16777215);

        // xd
        final String text;
        switch ((int) (Minecraft.getSystemTime() / 200L % 22L))
        {
            case 0:
                // noinspection DefaultNotLastCaseInSwitch
            default:
                text = "Liquidbounce";
                break;
            case 1:
            case 21:
                text = "lIquidbounce";
                break;
            case 2:
            case 20:
                text = "liQuidbounce";
                break;
            case 3:
            case 19:
                text = "liqUidbounce";
                break;
            case 4:
            case 18:
                text = "liquIdbounce";
                break;
            case 5:
            case 17:
                text = "liquiDbounce";
                break;
            case 6:
            case 16:
                text = "liquidBounce";
                break;
            case 7:
            case 15:
                text = "liquidbOunce";
                break;
            case 8:
            case 14:
                text = "liquidboUnce";
                break;
            case 9:
            case 13:
                text = "liquidbouNce";
                break;
            case 10:
            case 12:
                text = "liquidbounCe";
                break;
            case 11:
                text = "liquidbouncE";
                break;
        }

        mc.fontRendererObj.drawString(text, middleWidth - (mc.fontRendererObj.getStringWidth(text) >> 1), ypos + mc.fontRendererObj.FONT_HEIGHT, ColorUtils.rainbow(255, 400000L, 10, 1.0f, 1.0f).getRGB()/* 8421504 */);
    }
}
