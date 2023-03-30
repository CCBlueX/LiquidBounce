/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders;

import net.ccbluex.liquidbounce.utils.render.shader.Shader;
import org.lwjgl.opengl.Display;

import java.io.File;
import java.io.IOException;

import static net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform2f;

public final class BackgroundShader extends Shader {

    public final static BackgroundShader BACKGROUND_SHADER = new BackgroundShader();

    private float time;

    public BackgroundShader() {
        super("background.frag");
    }

    public BackgroundShader(final File fragmentShader) throws IOException {
        super(fragmentShader);
    }

    @Override
    public void setupUniforms() {
        setupUniform("iResolution");
        setupUniform("iTime");
    }

    @Override
    public void updateUniforms() {
        final int resolutionID = getUniform("iResolution");
        if(resolutionID > -1)
            glUniform2f(resolutionID, (float) Display.getWidth(), (float) Display.getHeight());
        final int timeID = getUniform("iTime");
        if(timeID > -1) glUniform1f(timeID, time);

        time += 0.003F * deltaTime;
    }

}
