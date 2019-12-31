package net.ccbluex.liquidbounce.ui.client.hud.element;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.ui.client.hud.GuiHudDesigner;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.*;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public class EditorPanel {

    private final GuiHudDesigner hudDesigner;

    private int x;
    private int y;
    private int width = 80;
    private int height = 20;
    private int realHeight = 20;

    private boolean mouseDown;
    private boolean rightMouseDown;

    private boolean drag;
    private int dragX;
    private int dragY;

    private int scroll;

    private Element element;

    public EditorPanel(GuiHudDesigner hudDesigner, int x, int y) {
        this.hudDesigner = hudDesigner;
        this.x = x;
        this.y = y;
    }

    public void drawPanel(final int mouseX, int mouseY, final int wheel) {
        // Drag panel
        drag(mouseX, mouseY);

        if(element != hudDesigner.selectedElement)
            scroll = 0;

        element = hudDesigner.selectedElement;

        boolean shouldScroll = realHeight > 200;

        if(shouldScroll) {
            glPushMatrix();
            RenderUtils.makeScissorBox(x, y + 1, x + width, y + 200);
            glEnable(GL_SCISSOR_TEST);

            if(y + 200 < mouseY)
                mouseY = -1;

            if(mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 200 && Mouse.hasWheel()) {
                if(wheel < 0 && -scroll + 205 <= realHeight) {
                    scroll -= 12;
                }else if(wheel > 0) {
                    scroll += 12;

                    if(scroll > 0)
                        scroll = 0;
                }
            }
        }

        // Draw panel
        Gui.drawRect(x, y + 12, x + width, y + realHeight, new Color(27, 34, 40).getRGB());

        if(hudDesigner.selectedElement != null) {
            height = scroll + 15;
            realHeight = 15;

            width = 80;

            Fonts.font35.drawString("X: " + hudDesigner.selectedElement.getLocationFromFacing()[0] + "" +
                    " (" + hudDesigner.selectedElement.getX() + ")", x + 2, y + height, Color.WHITE.getRGB());
            height += 10;
            realHeight += 10;
            Fonts.font35.drawString("Y: " + hudDesigner.selectedElement.getLocationFromFacing()[1] + "" +
                    " (" + hudDesigner.selectedElement.getY() + ")", x + 2, y + height, Color.WHITE.getRGB());
            height += 10;
            realHeight += 10;

            BigDecimal bd = new BigDecimal(Float.toString(hudDesigner.selectedElement.getScale()));
            bd = bd.setScale(2, 4);

            Fonts.font35.drawString("Scale: " + bd.toString(), x + 2, y + height, Color.WHITE.getRGB());
            height += 10;
            realHeight += 10;

            Fonts.font35.drawString("H:", x + 2, y + height, Color.WHITE.getRGB());
            Fonts.font35.drawString(hudDesigner.selectedElement.getFacing().getHorizontal().getName(),
                    x + 12, y + height, Color.GRAY.getRGB());

            if(Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width &&
                    mouseY >= y + height && mouseY <= y + height + 10) {
                for(int i = 0; i < Facing.Horizontal.values().length; i++) {
                    if(Facing.Horizontal.values()[i] == hudDesigner.selectedElement.getFacing().getHorizontal()) {
                        i++;

                        if(i >= Facing.Horizontal.values().length)
                            i = 0;

                        final int[] location = hudDesigner.selectedElement.getLocationFromFacing();

                        hudDesigner.selectedElement.getFacing().setHorizontal(Facing.Horizontal.values()[i]);
                        hudDesigner.selectedElement.setScreenX(location[0]).setScreenY(location[1]);
                        break;
                    }
                }
            }

            height += 10;
            realHeight += 10;

            if(Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width && mouseY >= y + height
                    && mouseY <= y + height + 10) {
                for(int i = 0; i < Facing.Vertical.values().length; i++) {
                    if(Facing.Vertical.values()[i] == hudDesigner.selectedElement.getFacing().getVertical()) {
                        i++;

                        if(i >= Facing.Vertical.values().length)
                            i = 0;

                        final int[] location = hudDesigner.selectedElement.getLocationFromFacing();

                        hudDesigner.selectedElement.getFacing().setVertical(Facing.Vertical.values()[i]);
                        hudDesigner.selectedElement.setScreenX(location[0]).setScreenY(location[1]);
                        break;
                    }
                }
            }

            Fonts.font35.drawString("V:", x + 2, y + height, Color.WHITE.getRGB());
            Fonts.font35.drawString(hudDesigner.selectedElement.getFacing().getVertical().getName(),
                    x + 12, y + height, Color.GRAY.getRGB());
            height += 10;
            realHeight += 10;

            width = 100;

            for(final Field field : hudDesigner.selectedElement.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);

                    final Object o = field.get(hudDesigner.selectedElement);

                    if(o instanceof Value) {
                        final Value value = (Value) o;

                        if(value instanceof BoolValue) {
                            final BoolValue boolValue = (BoolValue) value;

                            Fonts.font35.drawString(value.getName(), x + 2, y + height, boolValue.get() ?
                                    Color.WHITE.getRGB() : Color.GRAY.getRGB());

                            int stringWidth = Fonts.font35.getStringWidth(value.getName());

                            if(width < stringWidth + 8)
                                width = stringWidth + 8;

                            if(Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width &&
                                    mouseY >= y + height && mouseY <= y + height + 10) {
                                value.set(!boolValue.get());
                            }

                            height += 10;
                            realHeight += 10;
                        }else if(o instanceof FloatValue) {
                            final float valueNumber = ((FloatValue) o).get();
                            final float minNumber = ((FloatValue) o).getMinimum();
                            final float maxNumber = ((FloatValue) o).getMaximum();

                            BigDecimal floatBigDecimal = new BigDecimal(Float.toString(valueNumber));
                            floatBigDecimal = floatBigDecimal.setScale(2, 4);

                            Fonts.font35.drawString(value.getName() + ": §c" + floatBigDecimal.toString(),
                                    x + 2, y + height, Color.WHITE.getRGB());

                            int stringWidth = Fonts.font35.getStringWidth(value.getName() + ": §c" + valueNumber);

                            if(width < stringWidth + 8)
                                width = stringWidth + 8;

                            RenderUtils.drawRect(x + 8, y + height + 12, x + width, y + height + 13, Color.WHITE);
                            final float sliderValue = x + ((width - 12) * (valueNumber - minNumber) / (maxNumber - minNumber));
                            RenderUtils.drawRect(8 + sliderValue, y + height + 9, sliderValue + 11,
                                    y + height + 15, new Color(37, 126, 255).getRGB());

                            if(mouseX >= x + 8 && mouseX <= x + width && mouseY >= y + height + 9 && mouseY <= y + height + 15) {
                                if(Mouse.isButtonDown(0)) {
                                    float i = MathHelper.clamp_float((float) (mouseX - x - 8) / (width - 12), 0, 1);
                                    value.set(minNumber + (maxNumber - minNumber) * i);
                                }
                            }

                            height += 20;
                            realHeight += 20;
                        }else if(o instanceof IntegerValue) {
                            final int valueNumber = ((IntegerValue) o).get();
                            final int minNumber = ((IntegerValue) o).getMinimum();
                            final int maxNumber = ((IntegerValue) o).getMaximum();

                            Fonts.font35.drawString(value.getName() + ": §c" + valueNumber, x + 2, y + height, Color.WHITE.getRGB());

                            BigDecimal integerBigDecimal = new BigDecimal(Float.toString(valueNumber));
                            integerBigDecimal = integerBigDecimal.setScale(2, 4);

                            int stringWidth = Fonts.font35.getStringWidth(value.getName() + ": §c" + integerBigDecimal.toString());

                            if(width < stringWidth + 8)
                                width = stringWidth + 8;

                            RenderUtils.drawRect(x + 8, y + height + 12, x + width - 8, y + height + 13, Color.WHITE);
                            final float sliderValue = x + ((width - 18) * (valueNumber - minNumber) / (maxNumber - minNumber));
                            RenderUtils.drawRect(8 + sliderValue, y + height + 9, sliderValue + 11,
                                    y + height + 15, new Color(37, 126, 255).getRGB());

                            if(mouseX >= x + 8 && mouseX <= x + width && mouseY >= y + height + 9 && mouseY <= y + height + 15) {
                                if(Mouse.isButtonDown(0)) {
                                    float i = MathHelper.clamp_float((float) (mouseX - x - 8) / (width - 18), 0, 1);
                                    value.set((int) (minNumber + (maxNumber - minNumber) * i));
                                }
                            }

                            height += 20;
                            realHeight += 20;
                        }else if(o instanceof ListValue) {
                            final ListValue listValue = (ListValue) o;

                            Fonts.font35.drawString(value.getName(), x + 2, y + height, Color.WHITE.getRGB());

                            height += 10;
                            realHeight += 10;

                            for(final String s : listValue.getValues()) {
                                Fonts.font35.drawString("§c> §r" + s, x + 2, y + height,
                                        s.equals(listValue.get()) ? Color.WHITE.getRGB() : Color.GRAY.getRGB());

                                final int stringWidth = Fonts.font35.getStringWidth("§c> §r" + s);

                                if(width < stringWidth + 8)
                                    width = stringWidth + 8;

                                if(Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width &&
                                        mouseY >= y + height && mouseY <= y + height + 10)
                                    value.set(s);

                                height += 10;
                                realHeight += 10;
                            }
                        }
                    }else if(o instanceof FontRenderer) {
                        final FontRenderer fontRenderer = (FontRenderer) o;

                        String displayString = "Font: Unknown";

                        if (fontRenderer instanceof GameFontRenderer) {
                            final GameFontRenderer liquidFontRenderer = (GameFontRenderer) fontRenderer;

                            displayString = "Font: " + liquidFontRenderer.getDefaultFont().getFont().getName() + " - " +
                                    liquidFontRenderer.getDefaultFont().getFont().getSize();
                        }else if(fontRenderer == Fonts.minecraftFont)
                            displayString = "Font: Minecraft";

                        Fonts.font35.drawString(displayString, x + 2, y + height, Color.WHITE.getRGB());
                        int stringWidth = Fonts.font35.getStringWidth(displayString);

                        if(width < stringWidth + 8)
                            width = stringWidth + 8;

                        if((Mouse.isButtonDown(0) && !mouseDown || Mouse.isButtonDown(1) && !rightMouseDown)
                                && mouseX >= x && mouseX <= x + width && mouseY >= y + height && mouseY <= y + height + 10) {
                            final List<FontRenderer> fonts = Fonts.getFonts();

                            if(Mouse.isButtonDown(0)) {
                                for(int i = 0; i < fonts.size(); i++) {
                                    final FontRenderer font = fonts.get(i);

                                    if(font == fontRenderer) {
                                        i++;

                                        if(i >= fonts.size())
                                            i = 0;

                                        field.set(hudDesigner.selectedElement, fonts.get(i));
                                        break;
                                    }
                                }
                            }else{
                                for(int i = fonts.size() - 1; i >= 0; i--) {
                                    final FontRenderer font = fonts.get(i);

                                    if(font == fontRenderer) {
                                        i--;

                                        if(i >= fonts.size())
                                            i = 0;

                                        if(i < 0)
                                            i = fonts.size() - 1;

                                        field.set(hudDesigner.selectedElement, fonts.get(i));
                                        break;
                                    }
                                }
                            }
                        }

                        height += 10;
                        realHeight += 10;
                    }
                }catch(final IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            Gui.drawRect(x, y, x + width, y + 12, new Color(37, 126, 255).getRGB());
            Fonts.font35.drawString("§l" + hudDesigner.selectedElement.getName(),
                    x + 2, y + 3.5F, Color.WHITE.getRGB());
        }else{
            height = 15 + scroll;
            realHeight = 15;
            width = 90;

            for(final Element element : LiquidBounce.CLIENT.hud.getElements()) {
                Fonts.font35.drawString(element.getName(), x + 2, y + height, Color.WHITE.getRGB());

                int stringWidth = Fonts.font35.getStringWidth(element.getName());

                if(width < stringWidth + 8)
                    width = stringWidth + 8;

                if(Mouse.isButtonDown(0) && !mouseDown && mouseX >= x && mouseX <= x + width &&
                        mouseY >= y + height && mouseY <= y + height + 10) {
                    hudDesigner.selectedElement = element;
                }

                height += 10;
                realHeight += 10;
            }

            Gui.drawRect(x, y, x + width, y + 12, new Color(37, 126, 255).getRGB());
            Fonts.font35.drawString("§lAvailable Elements", x + 2, y + 3.5F, Color.WHITE.getRGB());
        }

        if(shouldScroll) {
            Gui.drawRect(x + width - 5, y + 15, x + width - 2, y + 197,
                    new Color(41, 41, 41).getRGB());

            final float v = 197 * ((float) -scroll / ((float) realHeight - 170F));
            RenderUtils.drawRect(x + width - 5, y + 15 + v, x + width - 2, y + 15 + v + 5,
                    new Color(37, 126, 255).getRGB());

            glDisable(GL_SCISSOR_TEST);
            glPopMatrix();
        }

        mouseDown = Mouse.isButtonDown(0);
        rightMouseDown = Mouse.isButtonDown(1);
    }

    private void drag(final int mouseX, final int mouseY) {
        if(mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 12 && Mouse.isButtonDown(0) && !mouseDown) {
            drag = true;
            dragX = mouseX - x;
            dragY = mouseY - y;
        }

        if(Mouse.isButtonDown(0) && drag) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }else
            drag = false;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRealHeight() {
        return realHeight;
    }
}