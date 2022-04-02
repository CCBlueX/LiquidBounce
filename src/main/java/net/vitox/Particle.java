package net.vitox;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.vitox.particle.util.RenderUtils;

import java.util.Random;

/**
 * Particle API
 * This Api is free2use
 * But u have to mention me.
 *
 * @author Vitox
 * @version 3.0
 */
@SideOnly(Side.CLIENT)
class Particle {

    public float x;
    public float y;
    public final float size;
    private final float ySpeed = new Random().nextInt(5);
    private final float xSpeed = new Random().nextInt(5);
    private int height;
    private int width;

    Particle(int x, int y) {
        this.x = x;
        this.y = y;
        this.size = genRandom();
    }

    private float lint1(float f) {
        return ((float) 1.02 * (1.0f - f)) + ((float) 1.0 * f);
    }

    private float lint2(float f) {
        return (float) 1.02 + f * ((float) 1.0 - (float) 1.02);
    }

    void connect(float x, float y) {
        RenderUtils.connectPoints(getX(), getY(), x, y);
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public float getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    void interpolation() {
        for(int n = 0; n <= 64; ++n) {
            final float f = n / 64.0f;
            final float p1 = lint1(f);
            final float p2 = lint2(f);

            if(p1 != p2) {
                y -= f;
                x -= f;
            }
        }
    }

    void fall() {
        final Minecraft mc = Minecraft.getMinecraft();
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        y = (y + ySpeed);
        x = (x + xSpeed);

        if(y > mc.displayHeight)
            y = 1;

        if(x > mc.displayWidth)
            x = 1;

        if(x < 1)
            x = scaledResolution.getScaledWidth();

        if(y < 1)
            y = scaledResolution.getScaledHeight();
    }

    private float genRandom() {
        return (float) (0.3f + Math.random() * (0.6f - 0.3f + 1.0F));
    }
}

