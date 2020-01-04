package net.ccbluex.liquidbounce.ui.client.hud;

import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.notifications.Notification;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public class HUD extends MinecraftInstance {

    private final List<Element> elements = new ArrayList<>();

    private final List<Notification> notifications = new ArrayList<>();

    public void render() {
        for(final Element element : elements) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(element.getScale(), element.getScale(), element.getScale());
            try {
                element.drawElement();
            }catch(final Throwable t) {
                t.printStackTrace();
            }
            GlStateManager.popMatrix();
        }
    }

    public void update() {
        for (final Element element : elements) {
            element.updateElement();
        }
    }

    public void handleMouseClick(final int mouseX, final int mouseY, final int button) {
        for(final Element element : elements)
            element.handleMouseClick((int) (mouseX / element.getScale()), (int) (mouseY / element.getScale()), button);

        for(int i = elements.size() - 1; i >= 0; i--) {
            final Element element = elements.get(i);

            if(element.isMouseOverElement((int) (mouseX / element.getScale()), (int) (mouseY / element.getScale())) && button == 0) {
                element.drag = true;
                elements.remove(element);
                elements.add(element);
                break;
            }
        }
    }

    public void handleMouseReleased() {
        for(final Element element : elements)
            element.drag = false;
    }

    public void handleMouseMove(final int mouseX, final int mouseY) {
        for(final Element element : elements) {
            final int scaledX = (int) (mouseX / element.getScale());
            final int scaledY = (int) (mouseY / element.getScale());

            if (element.drag && mc.currentScreen instanceof GuiHudDesigner) {
                switch(element.getFacing().getHorizontal()) {
                    case LEFT:
                    case MIDDLE:
                        element.setX(element.getX() + (scaledX - element.prevMouseX));
                        break;
                    case RIGHT:
                        element.setX(element.getX() - (scaledX - element.prevMouseX));
                        break;
                }

                switch(element.getFacing().getVertical()) {
                    case UP:
                    case MIDDLE:
                        element.setY(element.getY() + (scaledY - element.prevMouseY));
                        break;
                    case DOWN:
                        element.setY(element.getY() - (scaledY - element.prevMouseY));
                        break;
                }
            }

            element.prevMouseX = scaledX;
            element.prevMouseY = scaledY;
        }
    }

    public void handleKey(final char c, int keyCode) {
        elements.forEach(element -> element.handleKey(c, keyCode));
    }

    public void addElement(final Element element) {
        elements.add(element);
    }

    public void removeElement(final Element element) {
        element.destroyElement();

        elements.remove(element);
    }

    public List<Element> getElements() {
        return elements;
    }

    public void addNotification(final Notification notification) {
        notifications.add(notification);
    }

    public void removeNotification(final Notification notification) {
        notifications.remove(notification);
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void clearElements() {
        elements.forEach(Element :: destroyElement);
        elements.clear();
    }
}