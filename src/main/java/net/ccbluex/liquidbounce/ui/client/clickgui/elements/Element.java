/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.elements;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;

public class Element extends MinecraftInstance {

    private int x;
    private int y;
    private int width;
    private int height;
    private boolean visible;

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void drawScreen(int mouseX, int mouseY, float button) {
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        return false;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
