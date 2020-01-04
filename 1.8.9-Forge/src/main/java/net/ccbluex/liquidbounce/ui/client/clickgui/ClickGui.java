package net.ccbluex.liquidbounce.ui.client.clickgui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleManager;
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.Element;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement;
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style;
import net.ccbluex.liquidbounce.ui.client.clickgui.style.styles.SlowlyStyle;
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner;
import net.ccbluex.liquidbounce.utils.EntityUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class ClickGui extends GuiScreen {

    public final List<Panel> panels = new ArrayList<>();
    private Panel clickedPanel;
    public Style style = new SlowlyStyle();

    private int mouseX;
    private int mouseY;

    private final ResourceLocation hudIcon = new ResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + "/custom_hud_icon.png");

    public ClickGui() {
        final int width = 100;
        final int height = 18;

        int yPos = 5;
        for(final ModuleCategory category : ModuleCategory.values()) {
            panels.add(new Panel(category.getDisplayName(), 100, yPos, width, height, false) {

                @Override
                public void setupItems() {
                    for(Module module : ModuleManager.getModules())
                        if(module.getCategory() == category)
                            getElements().add(new ModuleElement(module));
                }
            });

            yPos += 20;
        }

        yPos += 20;

        panels.add(new Panel("Targets", 100, yPos, width, height, false) {

            @Override
            public void setupItems() {
                getElements().add(new ButtonElement("Players") {

                    @Override
                    public void createButton(String displayName) {
                        color = EntityUtils.targetPlayer ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        super.createButton(displayName);
                    }

                    @Override
                    public String getDisplayName() {
                        displayName = "Players";
                        color = EntityUtils.targetPlayer ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        return super.getDisplayName();
                    }

                    @Override
                    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
                        if(mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible()) {
                            EntityUtils.targetPlayer = !EntityUtils.targetPlayer;
                            displayName = "Players";
                            color = EntityUtils.targetPlayer ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                        }
                    }
                });

                getElements().add(new ButtonElement("Mobs") {

                    @Override
                    public void createButton(String displayName) {
                        color = EntityUtils.targetMobs ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        super.createButton(displayName);
                    }

                    @Override
                    public String getDisplayName() {
                        displayName = "Mobs";
                        color = EntityUtils.targetMobs ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        return super.getDisplayName();
                    }

                    @Override
                    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
                        if(mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible()) {
                            EntityUtils.targetMobs = !EntityUtils.targetMobs;
                            displayName = "Mobs";
                            color = EntityUtils.targetMobs ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                        }
                    }
                });

                getElements().add(new ButtonElement("Animals") {

                    @Override
                    public void createButton(String displayName) {
                        color = EntityUtils.targetAnimals ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        super.createButton(displayName);
                    }

                    @Override
                    public String getDisplayName() {
                        displayName = "Animals";
                        color = EntityUtils.targetAnimals ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        return super.getDisplayName();
                    }

                    @Override
                    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
                        if(mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible()) {
                            EntityUtils.targetAnimals = !EntityUtils.targetAnimals;
                            displayName = "Animals";
                            color = EntityUtils.targetAnimals ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                        }
                    }
                });

                getElements().add(new ButtonElement("Invisible") {

                    @Override
                    public void createButton(String displayName) {
                        color = EntityUtils.targetInvisible ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        super.createButton(displayName);
                    }

                    @Override
                    public String getDisplayName() {
                        displayName = "Invisible";
                        color = EntityUtils.targetInvisible ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        return super.getDisplayName();
                    }

                    @Override
                    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
                        if(mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible()) {
                            EntityUtils.targetInvisible = !EntityUtils.targetInvisible;
                            displayName = "Invisible";
                            color = EntityUtils.targetInvisible ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                        }
                    }
                });

                getElements().add(new ButtonElement("Dead") {

                    @Override
                    public void createButton(String displayName) {
                        color = EntityUtils.targetDead ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        super.createButton(displayName);
                    }

                    @Override
                    public String getDisplayName() {
                        displayName = "Dead";
                        color = EntityUtils.targetDead ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                        return super.getDisplayName();
                    }

                    @Override
                    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
                        if(mouseButton == 0 && isHovering(mouseX, mouseY) && isVisible()) {
                            EntityUtils.targetDead = !EntityUtils.targetDead;
                            displayName = "Dead";
                            color = EntityUtils.targetDead ? ClickGUI.generateColor().getRGB() : Integer.MAX_VALUE;
                            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
                        }
                    }
                });
            }
        });
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if(Mouse.isButtonDown(0) && mouseX >= 5 && mouseX <= 50 && mouseY <= height - 5 && mouseY >= height - 50)
            mc.displayGuiScreen(new GuiHudDesigner());

        final double scale = ((ClickGUI) Objects.requireNonNull(ModuleManager.getModule(ClickGUI.class))).scaleValue.get();

        mouseX /= scale;
        mouseY /= scale;

        this.mouseX = mouseX;
        this.mouseY = mouseY;

        drawDefaultBackground();

        RenderUtils.drawFilledCircle(25, height - 25, 16, new Color(37, 126, 255));
        RenderUtils.drawImage(hudIcon, 12, height - 35, 23, 23);

        GlStateManager.scale(scale, scale, scale);

        for(final Panel panel : panels) {
            panel.updateFade(RenderUtils.deltaTime);
            panel.drawScreen(mouseX, mouseY, partialTicks);
        }

        for(final Panel panel : panels) {
            for(final Element element : panel.getElements()) {
                if(element instanceof ModuleElement) {
                    final ModuleElement moduleElement = (ModuleElement) element;

                    if(mouseX != 0 && mouseY != 0 && moduleElement.isHovering(mouseX, mouseY) && moduleElement.isVisible() && element.getY() <= panel.getY() + panel.getFade())
                        style.drawDescription(mouseX, mouseY, moduleElement.getModule().getDescription());
                }
            }
        }

        if(Mouse.hasWheel()) {
            int wheel = Mouse.getDWheel();

            for(int i = panels.size() - 1; i >= 0; i--)
                if(panels.get(i).handleScroll(mouseX, mouseY, wheel))
                    break;
        }

        GlStateManager.disableLighting();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.scale(1, 1, 1);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        final double scale = ((ClickGUI) Objects.requireNonNull(ModuleManager.getModule(ClickGUI.class))).scaleValue.get();

        mouseX /= scale;
        mouseY /= scale;

        for(final Panel panel : panels) {
            panel.mouseClicked(mouseX, mouseY, mouseButton);

            panel.drag = false;

            if(mouseButton == 0 && panel.isHovering(mouseX, mouseY))
                clickedPanel = panel;
        }

        if(clickedPanel != null) {
            clickedPanel.x2 = clickedPanel.x - mouseX;
            clickedPanel.y2 = clickedPanel.y - mouseY;
            clickedPanel.drag = true;

            panels.remove(clickedPanel);
            panels.add(clickedPanel);
            clickedPanel = null;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        final double scale = ((ClickGUI) Objects.requireNonNull(ModuleManager.getModule(ClickGUI.class))).scaleValue.get();

        mouseX /= scale;
        mouseY /= scale;

        for(Panel panel : panels) {
            panel.mouseReleased(mouseX, mouseY, state);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void updateScreen() {
        for(final Panel panel : panels) {
            for(final Element element : panel.getElements()) {
                if(element instanceof ButtonElement) {
                    final ButtonElement buttonElement = (ButtonElement) element;

                    if(buttonElement.isHovering(mouseX, mouseY)) {
                        if(buttonElement.hoverTime < 7)
                            buttonElement.hoverTime++;
                    }else if(buttonElement.hoverTime > 0)
                        buttonElement.hoverTime--;
                }

                if(element instanceof ModuleElement) {
                    if(((ModuleElement) element).getModule().getState()) {
                        if(((ModuleElement) element).slowlyFade < 255)
                            ((ModuleElement) element).slowlyFade += 20;
                    }else if(((ModuleElement) element).slowlyFade > 0)
                        ((ModuleElement) element).slowlyFade -= 20;

                    if(((ModuleElement) element).slowlyFade > 255)
                        ((ModuleElement) element).slowlyFade = 255;

                    if(((ModuleElement) element).slowlyFade < 0)
                        ((ModuleElement) element).slowlyFade = 0;
                }
            }
        }
        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.clickGuiConfig);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
