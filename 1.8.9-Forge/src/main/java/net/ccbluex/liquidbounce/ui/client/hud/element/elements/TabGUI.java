package net.ccbluex.liquidbounce.ui.client.hud.element.elements;

import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo;
import net.ccbluex.liquidbounce.ui.client.hud.element.Facing;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.input.Keyboard.*;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ElementInfo(name = "TabGUI")
public class TabGUI extends Element {

    private final IntegerValue redValue = new IntegerValue("Rectangle Red", 0, 0, 255);
    private final IntegerValue greenValue = new IntegerValue("Rectangle Green", 148, 0, 255);
    private final IntegerValue blueValue = new IntegerValue("Rectangle Blue", 255, 0, 255);
    private final IntegerValue alphaValue = new IntegerValue("Rectangle Alpha", 140, 0, 255);
    private final BoolValue rectangleRainbow = new BoolValue("Rectangle Rainbow", false);

    private final IntegerValue backgroundRedValue = new IntegerValue("Background Red", 0, 0, 255);
    private final IntegerValue backgroundGreenValue = new IntegerValue("Background Green", 0, 0, 255);
    private final IntegerValue backgroundBlueValue = new IntegerValue("Background Blue", 0, 0, 255);
    private final IntegerValue backgroundAlphaValue = new IntegerValue("Background Alpha", 150, 0, 255);

    private final BoolValue borderValue = new BoolValue("Border", true);

    private final IntegerValue borderStrength = new IntegerValue("Border Strength", 2, 1, 5);

    private final IntegerValue borderRedValue = new IntegerValue("Border Red", 0, 0, 255);
    private final IntegerValue borderGreenValue = new IntegerValue("Border Green", 0, 0, 255);
    private final IntegerValue borderBlueValue = new IntegerValue("Border Blue", 0, 0, 255);
    private final IntegerValue borderAlphaValue = new IntegerValue("Border Alpha", 150, 0, 255);

    private final BoolValue borderRainbow = new BoolValue("Border Rainbow", false);

    private final BoolValue arrowsValue = new BoolValue("Arrows", true);

    private FontRenderer fontRenderer = Fonts.font35;
    private final BoolValue textShadow = new BoolValue("TextShadow", false);
    private final BoolValue textFade = new BoolValue("TextFade", true);
    private final IntegerValue textPositionY = new IntegerValue("TextPosition-Y", 2, 0, 5);

    private final IntegerValue width = new IntegerValue("Width", 60, 55, 100);
    private final IntegerValue tabHeight = new IntegerValue("TabHeight", 12, 10, 15);

    private final BoolValue upperCaseValue = new BoolValue("UpperCase", false);

    private int guiHeight;

    private float tabY;
    private float itemY;

    private int selectedTab;
    private int selectedItem;
    private boolean mainMenu = true;

    private final List<Tab> tabs = new ArrayList<>();

    public TabGUI() {
        for(final ModuleCategory category : ModuleCategory.values()) {
            final Tab tab = new Tab(category.getDisplayName());

            ModuleManager.getModules().stream().filter(module -> category.equals(module.getCategory())).forEach(tab.modules :: add);

            tabs.add(tab);
        }
    }

    @Override
    public void drawElement() {
        final int delta = RenderUtils.deltaTime;

        int xPos = tabHeight.get() * selectedTab;
        if((int) tabY != xPos) {
            if(xPos > tabY)
                tabY += 0.1F * delta;
            else
                tabY -= 0.1F * delta;
        }else tabY = xPos;

        int xPos2 = tabHeight.get() * selectedItem;
        if((int) itemY != xPos2) {
            if(xPos2 > itemY)
                itemY += 0.1F * delta;
            else
                itemY -= 0.1F * delta;
        }else itemY = xPos2;

        if(mainMenu)
            itemY = 0;

        if(textFade.get()) {
            for(int i = 0; i < tabs.size(); ++i) {
                final Tab tab = tabs.get(i);

                if(i == selectedTab) {
                    if(tab.textFade < 4) tab.textFade += 0.05F * delta;
                    if(tab.textFade > 4) tab.textFade = 4;
                }else{
                    if(tab.textFade > 0) tab.textFade -= 0.05F * delta;
                    if(tab.textFade < 0) tab.textFade = 0;
                }
            }
        }else{
            for(final Tab tab : tabs) {
                if(tab.textFade > 0) tab.textFade -= 0.05F * delta;
                if(tab.textFade < 0) tab.textFade = 0;
            }
        }

        final int[] loc = getLocationFromFacing();

        drawGui(loc[0], loc[1]);
    }

    @Override
    public void destroyElement() {

    }

    @Override
    public void updateElement() {

    }

    @Override
    public void handleMouseClick(int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    public void handleKey(char c, int keyCode) {
        switch(keyCode) {
            case KEY_UP:
                parseAction(Action.UP);
                break;
            case KEY_DOWN:
                parseAction(Action.DOWN);
                break;
            case KEY_RIGHT:
                parseAction(getFacing().getHorizontal() == Facing.Horizontal.RIGHT ? Action.LEFT : Action.RIGHT);
                break;
            case KEY_LEFT:
                parseAction(getFacing().getHorizontal() == Facing.Horizontal.RIGHT ? Action.RIGHT : Action.LEFT);
                break;
            case KEY_RETURN:
                parseAction(Action.TOGGLE);
                break;
        }
    }

    @Override
    public boolean isMouseOverElement(int mouseX, int mouseY) {
        final int[] location = getLocationFromFacing();

        return mouseX >= location[0] && mouseY >= location[1] && mouseX <= location[0] + width.get() && mouseY <= location[1] + guiHeight;
    }

    private void drawGui(final int posX, final int posY) {
        final Color color = !rectangleRainbow.get() ? new Color(redValue.get(), greenValue.get(), blueValue.get(), alphaValue.get()) : ColorUtils.rainbow(400000000L, alphaValue.get());
        final Color backgroundColor = new Color(backgroundRedValue.get(), backgroundGreenValue.get(), backgroundBlueValue.get(), backgroundAlphaValue.get());
        final Color borderColor = !borderRainbow.get() ? new Color(borderRedValue.get(), borderGreenValue.get(), borderBlueValue.get(), borderAlphaValue.get()) : ColorUtils.rainbow(400000000L, borderAlphaValue.get());

        guiHeight = tabs.size() * tabHeight.get();
        if(borderValue.get())
            RenderUtils.drawBorderedRect(posX - 1, posY, posX + width.get(), posY + guiHeight, borderStrength.get(), borderColor.getRGB(), backgroundColor.getRGB());
        else
            RenderUtils.drawRect(posX - 1, posY, posX + width.get(), posY + guiHeight, backgroundColor.getRGB());
        RenderUtils.drawRect(posX - 1, posY + 1 + tabY - 1, posX + width.get(), posY + tabY + tabHeight.get(), color);
        GlStateManager.resetColor();

        int yOff = posY + 1;
        for(int i = 0; i < tabs.size(); ++i) {
            final String tabName = upperCaseValue.get() ? tabs.get(i).tabName.toUpperCase() : tabs.get(i).tabName;
            fontRenderer.drawString(tabName, getFacing().getHorizontal() == Facing.Horizontal.RIGHT ? posX + width.get() - fontRenderer.getStringWidth(tabName) - tabs.get(i).textFade - 3 : posX + tabs.get(i).textFade + 2, yOff + textPositionY.get(), selectedTab == i ? 0xffffff : new Color(210, 210, 210).getRGB(), textShadow.get());

            if(arrowsValue.get()) {
                if(getFacing().getHorizontal() == Facing.Horizontal.RIGHT)
                    fontRenderer.drawString(!mainMenu && selectedTab == i ? ">" : "<", posX + 3, yOff + 2, 0xffffff, textShadow.get());
                else
                    fontRenderer.drawString(!mainMenu && selectedTab == i ? "<" : ">", posX + width.get() - 8, yOff + 2, 0xffffff, textShadow.get());
            }

            if(i == selectedTab && !mainMenu)
                tabs.get(i).drawTab(getFacing().getHorizontal() == Facing.Horizontal.RIGHT ? posX - 3 - tabs.get(i).menuWidth : posX + width.get() + 3, yOff - 2, color.getRGB(), backgroundColor.getRGB(), borderColor.getRGB(), borderStrength.get(), upperCaseValue.get());
            yOff += tabHeight.get();
        }
    }

    private void parseAction(final Action action) {
        switch(action) {
            case UP:
                if(mainMenu) {
                    --selectedTab;
                    if(selectedTab < 0) {
                        selectedTab = tabs.size() - 1;
                        tabY = tabHeight.get() * selectedTab;
                    }
                }else{
                    --selectedItem;
                    if(selectedItem < 0) {
                        selectedItem = tabs.get(selectedTab).modules.size() - 1;
                        itemY = tabHeight.get() * selectedItem;
                    }
                }
                break;
            case DOWN:
                if(mainMenu) {
                    ++selectedTab;
                    if(selectedTab > tabs.size() - 1) {
                        selectedTab = 0;
                        tabY = tabHeight.get() * selectedTab;
                    }
                }else{
                    ++selectedItem;
                    if(selectedItem > tabs.get(selectedTab).modules.size() - 1) {
                        selectedItem = 0;
                        itemY = tabHeight.get() * selectedItem;
                    }
                }
                break;
            case LEFT:
                if(!mainMenu)
                    mainMenu = true;
                break;
            case RIGHT:
                if(mainMenu) {
                    mainMenu = false;
                    selectedItem = 0;
                }
                break;
            case TOGGLE:
                if(!mainMenu) {
                    int sel = selectedItem;
                    tabs.get(selectedTab).modules.get(sel).toggle();
                }
                break;
        }
    }

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    public TabGUI setFontRenderer(final FontRenderer fontRenderer) {
        this.fontRenderer = fontRenderer;
        return this;
    }

    public TabGUI setRainbow(final boolean rainbow) {
        this.rectangleRainbow.set(rainbow);
        return this;
    }

    public TabGUI setColor(final Color c) {
        redValue.set(c.getRed());
        greenValue.set(c.getGreen());
        blueValue.set(c.getBlue());
        alphaValue.set(c.getAlpha());
        return this;
    }

    private class Tab {

        private final String tabName;
        private final ArrayList<Module> modules = new ArrayList<>();

        private int menuWidth;

        private float textFade;

        private Tab(final String name) {
            tabName = name;
        }

        private void drawTab(int x, int y, final int color, final int backgroundColor, final int borderColor, final int borderStrength, final boolean upperCase) {
            int maxWidth = 0;

            for(final Module module : modules)
                if(fontRenderer.getStringWidth(upperCase ? module.getName().toUpperCase() : module.getName()) + 4 > maxWidth)
                    maxWidth = (int) (fontRenderer.getStringWidth(upperCase ? module.getName().toUpperCase() : module.getName()) + 7F);

            menuWidth = maxWidth;
            int menuHeight = modules.size() * tabHeight.get();

            x += 2;
            y += 2;

            if(borderValue.get())
                RenderUtils.drawBorderedRect(x - 1, y - 1, x + menuWidth - 2, y + menuHeight - 1, borderStrength, borderColor, backgroundColor);
            else
                RenderUtils.drawRect(x - 1, y - 1, x + menuWidth - 2, y + menuHeight - 1, backgroundColor);
            RenderUtils.drawRect(x - 1, y + itemY - 1, x + menuWidth - 2, y + itemY + tabHeight.get() - 1, color);
            GlStateManager.resetColor();
            for(int i = 0; i < modules.size(); ++i)
                fontRenderer.drawString(upperCase ? modules.get(i).getName().toUpperCase() : modules.get(i).getName(), x + 2, y + tabHeight.get() * i + textPositionY.get(), modules.get(i).getState() ? 0xffffff : new Color(205, 205, 205).getRGB(), textShadow.get());
        }
    }

    public enum Action {
        UP, DOWN, LEFT, RIGHT, TOGGLE
    }
}