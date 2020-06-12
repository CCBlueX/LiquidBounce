/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.utils.render.shader.shaders;

import net.ccbluex.liquidbounce.utils.render.shader.Shader;
import org.lwjgl.opengl.GL20;

public class RainbowFontShader extends Shader {
    public static final RainbowFontShader INSTANCE = new RainbowFontShader();

    private boolean inUse = false;

    private float strengthX;
    private float strengthY;
    private float offset;

    public RainbowFontShader() {
        super("rainbow_font_shader.frag");
    }

    @Override
    public void setupUniforms() {
        setupUniform("offset");
        setupUniform("strength");
    }

    @Override
    public void updateUniforms() {
        GL20.glUniform2f(getUniform("strength"), strengthX, strengthY);
        GL20.glUniform1f(getUniform("offset"), offset);
    }

    public void setStrengthX(float strengthX) {
        this.strengthX = strengthX;
    }

    public void setStrengthY(float strengthY) {
        this.strengthY = strengthY;
    }

    public void setOffset(float offset) {
        this.offset = offset;
    }

    @Override
    public void startShader() {
        super.startShader();

        inUse = true;
    }

    @Override
    public void stopShader() {
        super.stopShader();

        inUse = false;
    }

    public boolean isInUse() {
        return inUse;
    }
}
