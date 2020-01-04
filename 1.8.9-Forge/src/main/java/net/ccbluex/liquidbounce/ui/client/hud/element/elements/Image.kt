package net.ccbluex.liquidbounce.ui.client.hud.element.elements;

import com.google.gson.JsonElement;
import net.ccbluex.liquidbounce.ui.client.hud.GuiHudDesigner;
import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo;
import net.ccbluex.liquidbounce.utils.misc.RandomUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.TextValue;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ElementInfo(name = "Image")
public class Image extends Element {

    private final TextValue image = new TextValue("Image", "") {

        @Override
        public void fromJson(JsonElement element) {
            super.fromJson(element);

            if(image.get().isEmpty()) return;

            setImage(image.get());
        }

        @Override
        protected void onChanged(final String oldValue, final String newValue) {
            if(image.get().isEmpty()) return;

            setImage(image.get());
        }
    };

    private final ResourceLocation resourceLocation = new ResourceLocation(RandomUtils.randomNumber(128));

    private int width = 64;
    private int height = 64;

    @Override
    public void drawElement() {
        final int[] location = getLocationFromFacing();

        RenderUtils.drawImage(resourceLocation, location[0], location[1], width / 2, height / 2);

        if (mc.currentScreen instanceof GuiHudDesigner)
            RenderUtils.drawBorderedRect(location[0], location[1], location[0] + width / 2, location[1] + height / 2, 3, Integer.MIN_VALUE, 0);
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
    }

    @Override
    public boolean isMouseOverElement(int mouseX, int mouseY) {
        final int[] location = getLocationFromFacing();

        return mouseX >= location[0] && mouseY >= location[1] && mouseX <= location[0] + width && mouseY <= location[1] + height;
    }

    private Image setImage(final String image) {
        try {
            this.image.changeValue(image);

            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(Base64.getDecoder().decode(image));
            final BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);
            byteArrayInputStream.close();

            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();

            mc.getTextureManager().loadTexture(resourceLocation, new DynamicTexture(bufferedImage));
        }catch(final Exception e) {
            e.printStackTrace();
        }

        return this;
    }

    public Image setImage(final File image) {
        try {
            setImage(Base64.getEncoder().encodeToString(Files.readAllBytes(image.toPath())));
        }catch(final Exception e) {
            e.printStackTrace();
        }

        return this;
    }
}