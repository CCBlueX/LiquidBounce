package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import static org.lwjgl.opengl.GL11.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.Optional;

import javax.imageio.ImageIO;

import net.ccbluex.liquidbounce.utils.AsyncUtils;
import net.ccbluex.liquidbounce.utils.AsyncUtilsKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.ClickEvent.Action;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ScreenShotHelper;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ScreenShotHelper.class)
public abstract class MixinScreenShotHelper
{
    @Shadow
    @Final
    private static Logger logger;
    @Shadow
    private static IntBuffer pixelBuffer;
    @Shadow
    private static int[] pixelValues;

    @Contract("_ -> !null")
    @Shadow
    private static File getTimestampedPNGFileForDirectory(final File gameDirectory)
    {
        // noinspection Contract - this method will be shadowed
        return null;
    }

    /**
     * @reason Asynchronous screenshot save
     * @author OrangeMarshall & eric0210
     */
    @Overwrite
    public static IChatComponent saveScreenshot(final File gameDirectory, final String screenshotName, final int widthIn, final int heightIn, final Framebuffer buffer)
    {
        final GuiNewChat chatGUI = Minecraft.getMinecraft().ingameGUI.getChatGUI();

        try
        {
            final File screenshotsFolder = new File(gameDirectory, "screenshots");
            screenshotsFolder.mkdir();

            final int width;
            final int height;

            final int framebufferWidth = buffer.framebufferWidth;
            final int framebufferHeight = buffer.framebufferHeight;

            final int framebufferTextureWidth = buffer.framebufferTextureWidth;
            final int framebufferTextureHeight = buffer.framebufferTextureHeight;

            if (OpenGlHelper.isFramebufferEnabled())
            {
                width = framebufferTextureWidth;
                height = framebufferTextureHeight;
            }
            else
            {
                width = widthIn;
                height = heightIn;
            }

            // Re-allocate pixel buffer as needed
            final int pixelBufferCapacity = width * height;
            if (pixelBuffer == null || pixelBuffer.capacity() < pixelBufferCapacity)
            {
                pixelBuffer = BufferUtils.createIntBuffer(pixelBufferCapacity);
                pixelValues = new int[pixelBufferCapacity];
            }

            // Save pixels to pixel buffer
            glPixelStorei(GL_PACK_ALIGNMENT, 1);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            pixelBuffer.clear();

            // Read current screen into pixel buffer
            if (OpenGlHelper.isFramebufferEnabled())
            {
                GlStateManager.bindTexture(buffer.framebufferTexture);
                glGetTexImage(GL_TEXTURE_2D, 0, 32993, 33639, pixelBuffer);
            }
            else
                glReadPixels(0, 0, width, height, 32993, 33639, pixelBuffer);

            // Copy pixel buffer into array
            pixelBuffer.get(pixelValues);

            // Get the screenshot file name
            final File screenshotFile = Optional.ofNullable(screenshotName).map(name -> new File(screenshotsFolder, name)).orElseGet(() -> getTimestampedPNGFileForDirectory(screenshotsFolder));

            // Create the BufferedImage and Save to the file ASYNCHRONOUSLY!
            AsyncUtilsKt.runAsync(() ->
            {
                // Process pixels
                TextureUtil.processPixelValues(pixelValues, width, height);

                BufferedImage screenShot = null;
                try
                {
                    // Create BufferedImage with cached pixel values
                    if (OpenGlHelper.isFramebufferEnabled())
                    {
                        screenShot = new BufferedImage(framebufferWidth, framebufferHeight, 1);

                        final int heightDelta = framebufferTextureHeight - framebufferHeight;
                        for (int ypos = heightDelta; ypos < framebufferTextureHeight; ++ypos)
                            for (int xpos = 0; xpos < framebufferWidth; ++xpos)
                                screenShot.setRGB(xpos, ypos - heightDelta, pixelValues[ypos * framebufferTextureWidth + xpos]);
                    }
                    else
                    {
                        screenShot = new BufferedImage(width, height, 1);
                        screenShot.setRGB(0, 0, width, height, pixelValues, 0, width);
                    }
                }
                catch (final Throwable t)
                {
                    logger.warn("Couldn't take screenshot asynchronously", t);
                    chatGUI.printChatMessage(new ChatComponentTranslation("screenshot.failure", t.getMessage()));
                }

                File _screenshotFile = screenshotFile;

                // Write the screenshot file
                if (screenShot != null)
                    try
                    {
                        ImageIO.write(screenShot, "png", _screenshotFile);
                    }
                    catch (final IOException e)
                    {
                        logger.warn("Couldn't save screenshot file asynchronously; Try fallback strategy", e);

                        // Try fallback strategy
                        try
                        {
                            _screenshotFile = new File(_screenshotFile + ".bak");
                            ImageIO.write(screenShot, "png", new File(_screenshotFile + ".bak"));
                            logger.warn("Saved screenshot file asynchronously with fallback strategy", e);
                        }
                        catch (final IOException e2)
                        {
                            // If fallback strategy failed, nothing we can to anymore.
                            logger.warn("Couldn't save screenshot file asynchronously with fallback strategy", e);
                            chatGUI.printChatMessage(new ChatComponentTranslation("screenshot.failure", e.getMessage()));
                        }
                    }

                final IChatComponent successChat = new ChatComponentText(_screenshotFile.getName());
                successChat.getChatStyle().setChatClickEvent(new ClickEvent(Action.OPEN_FILE, _screenshotFile.getAbsolutePath()));
                successChat.getChatStyle().setUnderlined(true);
                chatGUI.printChatMessage(new ChatComponentTranslation("screenshot.success", successChat));
            });

            return new ChatComponentText("[AsyncScreenShot] Queued taking screenshot as " + screenshotFile.getName() + "...");
        }
        catch (final RuntimeException e)
        {
            logger.warn("Couldn't save screenshot asynchronously", e);
            return new ChatComponentTranslation("screenshot.failure", e.getMessage());
        }
    }
}
