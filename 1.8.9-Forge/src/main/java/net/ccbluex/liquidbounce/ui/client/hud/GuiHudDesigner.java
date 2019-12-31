package net.ccbluex.liquidbounce.ui.client.hud;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.ui.client.hud.element.EditorPanel;
import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.Facing;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.*;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.misc.MiscUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.io.IOException;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class GuiHudDesigner extends GuiScreen {

    private GuiButton createButton;
    private GuiButton resetButton;
    private GuiButton deleteButton;

    private boolean isCreating;

    private GuiButton textButton;
    private GuiButton tabGuiButton;
    private GuiButton arrayButton;
    private GuiButton modelButton;
    private GuiButton armorButton;
    private GuiButton effectsButton;
    private GuiButton notificationsButton;
    private GuiButton imageButton;
    private GuiButton closeButton;

    public Element selectedElement;

    private EditorPanel editorPanel = new EditorPanel(this, 2, 2);

    private boolean buttonAction;

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        editorPanel = new EditorPanel(this, width / 2, height / 2);

        buttonList.add(deleteButton = new GuiButton(1, (width / 2) - 95, 2, 60, 20, "Delete"));
        buttonList.add(resetButton = new GuiButton(12, (width / 2) - 30, 2, 60, 20, "Reset"));
        buttonList.add(createButton = new GuiButton(2, (width / 2) + 35, 2, 60, 20, "Create"));

        isCreating = false;

        textButton = new GuiButton(3, width / 2 - 100, height / 4 + 48, "Text");
        tabGuiButton = new GuiButton(4, width / 2 - 100, height / 4 + 48 + 25, "TabGUI");
        arrayButton = new GuiButton(5, width / 2 - 100, height / 4 + 48 + 50, "Array");
        modelButton = new GuiButton(7, width / 2 - 100, height / 4 + 48 + 75, "Model");
        armorButton = new GuiButton(8, width / 2 - 100, height / 4 + 48 + 100, "Armor");
        effectsButton = new GuiButton(9, width / 2 - 100, height / 4 + 48 + 125, "Effects");
        notificationsButton = new GuiButton(10, width / 2 - 100, height / 4 + 48 + 150, "Notifications");
        imageButton = new GuiButton(11, width / 2 - 100, height / 4 + 48 + 175, "Image");
        closeButton = new GuiButton(6, width / 2 - 100, height / 4 + 48 + 200, "Close");
        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        arrayButton.enabled = true;
        for(final Element element : LiquidBounce.CLIENT.hud.getElements()) {
            if(element instanceof Arraylist) {
                arrayButton.enabled = false;
                break;
            }
        }

        Gui.drawRect(width / 2 - 100, 0, width / 2 + 100, 25, Integer.MIN_VALUE);

        LiquidBounce.CLIENT.hud.render();
        LiquidBounce.CLIENT.hud.handleMouseMove(mouseX, mouseY);

        if(!LiquidBounce.CLIENT.hud.getElements().contains(selectedElement))
            selectedElement = null;

        final int wheel = Mouse.getDWheel();

        editorPanel.drawPanel(mouseX, mouseY, wheel);

        if(isCreating) {
            super.drawScreen(mouseX, mouseY, partialTicks);

            drawGradientRect(0, 0, this.width, this.height, -1072689136, -804253680);

            textButton.drawButton(mc, mouseX, mouseY);
            tabGuiButton.drawButton(mc, mouseX, mouseY);
            arrayButton.drawButton(mc, mouseX, mouseY);
            closeButton.drawButton(mc, mouseX, mouseY);
            modelButton.drawButton(mc, mouseX, mouseY);
            armorButton.drawButton(mc, mouseX, mouseY);
            effectsButton.drawButton(mc, mouseX, mouseY);
            notificationsButton.drawButton(mc, mouseX, mouseY);
            imageButton.drawButton(mc, mouseX, mouseY);
            return;
        }

        for(final Element element : LiquidBounce.CLIENT.hud.getElements()) {
            if(element.isMouseOverElement((int) (mouseX / element.getScale()), (int) (mouseY / element.getScale()))) {
                element.setScale(element.getScale() + (wheel > 0 ? 0.05F : wheel < 0 ? -0.05F : 0));
                break;
            }
        }

        deleteButton.enabled = selectedElement != null;

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if(buttonAction) {
            buttonAction = false;
            return;
        }

        if(isCreating)
            return;

        LiquidBounce.CLIENT.hud.handleMouseClick(mouseX, mouseY, mouseButton);

        if (!(mouseX >= editorPanel.getX() && mouseX <= editorPanel.getX() + editorPanel.getWidth() && mouseY >= editorPanel.getY() && mouseY <= editorPanel.getY() + (Math.min(editorPanel.getRealHeight(), 200))))
            selectedElement = null;

        for(final Element element : LiquidBounce.CLIENT.hud.getElements()) {
            if(mouseButton == 0 && element.isMouseOverElement((int) (mouseX / element.getScale()), (int) (mouseY / element.getScale()))) {
                selectedElement = element;
                break;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);

        if(isCreating)
            return;

        LiquidBounce.CLIENT.hud.handleMouseReleased();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.hudConfig);
        super.onGuiClosed();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        buttonAction = true;

        switch(button.id) {
            case 1:
                if(selectedElement != null)
                    LiquidBounce.CLIENT.hud.removeElement(selectedElement);
                break;
            case 2:
                buttonList.add(textButton);
                buttonList.add(tabGuiButton);
                buttonList.add(arrayButton);
                buttonList.add(modelButton);
                buttonList.add(armorButton);
                buttonList.add(closeButton);
                buttonList.add(effectsButton);
                buttonList.add(notificationsButton);
                buttonList.add(imageButton);

                createButton.enabled = false;
                resetButton.enabled = false;
                deleteButton.enabled = false;

                isCreating = true;
                break;
            case 3:
                LiquidBounce.CLIENT.hud.addElement(new Text().setText("").setShadow(true).setRainbow(false).setFontRenderer(Fonts.font40).setX(2).setY(2).setScale(1).setFacing(new Facing(Facing.Horizontal.LEFT, Facing.Vertical.UP)));
                break;
            case 4:
                LiquidBounce.CLIENT.hud.addElement(new TabGUI().setFontRenderer(Fonts.font35).setX(2).setY(25).setFacing(new Facing(Facing.Horizontal.LEFT, Facing.Vertical.UP)));
                break;
            case 5:
                LiquidBounce.CLIENT.hud.addElement(new Arraylist().setFontRenderer(Fonts.font40).setX(0).setY(2).setFacing(new Facing(Facing.Horizontal.RIGHT, Facing.Vertical.UP)));
                break;
            case 6:
                buttonList.remove(textButton);
                buttonList.remove(tabGuiButton);
                buttonList.remove(arrayButton);
                buttonList.remove(modelButton);
                buttonList.remove(armorButton);
                buttonList.remove(closeButton);
                buttonList.remove(effectsButton);
                buttonList.remove(notificationsButton);
                buttonList.remove(imageButton);

                createButton.enabled = true;
                resetButton.enabled = true;
                deleteButton.enabled = true;

                isCreating = false;
                break;
            case 7:
                LiquidBounce.CLIENT.hud.addElement(new Model().setX(40).setY(100).setFacing(new Facing(Facing.Horizontal.LEFT, Facing.Vertical.UP)));
                break;
            case 8:
                LiquidBounce.CLIENT.hud.addElement(new Armor().setX(8).setY(55).setFacing(new Facing(Facing.Horizontal.MIDDLE, Facing.Vertical.DOWN)));
                break;
            case 9:
                LiquidBounce.CLIENT.hud.addElement(new Effects().setFontRenderer(Fonts.font35).setShadow(true).setX(2).setY(10).setFacing(new Facing(Facing.Horizontal.RIGHT, Facing.Vertical.DOWN)));
                break;
            case 10:
                LiquidBounce.CLIENT.hud.addElement(new Notifications().setX(0).setY(40).setFacing(new Facing(Facing.Horizontal.RIGHT, Facing.Vertical.DOWN)));
                break;
            case 11:
                final File file = MiscUtils.openFileChooser();

                if (file == null) return;

                if (!file.exists()) {
                    MiscUtils.showErrorPopup("Error", "The file does not exist.");
                    return;
                }

                if (file.isDirectory()) {
                    MiscUtils.showErrorPopup("Error", "The file is a directory.");
                    return;
                }

                LiquidBounce.CLIENT.hud.addElement(new Image().setImage(file).setX(2).setY(2).setFacing(new Facing(Facing.Horizontal.LEFT, Facing.Vertical.UP)));
                break;
            case 12:
                selectedElement = null;
                LiquidBounce.CLIENT.hud.clearElements();
                LiquidBounce.CLIENT.hud = new DefaultHUD();
                selectedElement = null;
                break;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(isCreating) {
            super.keyTyped(typedChar, keyCode);
            return;
        }

        if(Keyboard.KEY_DELETE == keyCode && selectedElement != null)
            LiquidBounce.CLIENT.hud.removeElement(selectedElement);

        if(Keyboard.KEY_ESCAPE != keyCode)
            LiquidBounce.CLIENT.hud.handleKey(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }
}
