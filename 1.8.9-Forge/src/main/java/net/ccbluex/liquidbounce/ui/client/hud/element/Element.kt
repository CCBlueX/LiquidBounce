package net.ccbluex.liquidbounce.ui.client.hud.element;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public abstract class Element extends MinecraftInstance {

    private final String name = getClass().getAnnotation(ElementInfo.class).name();

    private int x;
    private int y;
    private float scale = 1;
    private Facing facing;

    public boolean drag;
    public int prevMouseX;
    public int prevMouseY;

    public abstract void drawElement();

    public abstract void destroyElement();

    public abstract void updateElement();

    public abstract void handleMouseClick(final int mouseX, final int mouseY, final int mouseButton);

    public abstract void handleKey(final char c, final int keyCode);

    public abstract boolean isMouseOverElement(final int mouseX, final int mouseY);

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public Element setX(int x) {
        this.x = x;
        return this;
    }

    public int getY() {
        return y;
    }

    public Element setY(int y) {
        this.y = y;
        return this;
    }

    public Element setScale(float scale) {
        if(scale < 0)
            scale = 0;

        this.scale = scale;
        return this;
    }

    public float getScale() {
        return scale;
    }

    public Facing getFacing() {
        return facing;
    }

    public Element setFacing(Facing facing) {
        this.facing = facing;
        return this;
    }

    public int[] getLocationFromFacing() {
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        return new int[] {facing.getHorizontal() == Facing.Horizontal.RIGHT ? scaledResolution.getScaledWidth() - x : facing.getHorizontal() == Facing.Horizontal.MIDDLE ? (scaledResolution.getScaledWidth() / 2) + x : x, facing.getVertical() == Facing.Vertical.DOWN ? scaledResolution.getScaledHeight() - y : facing.getVertical() == Facing.Vertical.MIDDLE ? (scaledResolution.getScaledHeight() / 2) + y : y};
    }

    public Element setScreenX(final int x) {
        final ScaledResolution scaledResolution = new ScaledResolution(mc);

        switch(facing.getHorizontal()) {
            case LEFT:
                this.x = x;
                break;
            case MIDDLE:
                this.x = x - (scaledResolution.getScaledWidth() / 2);
                break;
            case RIGHT:
                this.x = scaledResolution.getScaledWidth() - x;
                break;
        }

        return this;
    }

    public Element setScreenY(final int y) {
        final ScaledResolution scaledResolution = new ScaledResolution(mc);

        switch(facing.getVertical()) {
            case UP:
                this.y = y;
                break;
            case MIDDLE:
                this.y = y - (scaledResolution.getScaledHeight() / 2);
                break;
            case DOWN:
                this.y = scaledResolution.getScaledHeight() - y;
                break;
        }

        return this;
    }
}