package net.ccbluex.liquidbounce.ui.client.hud.element.elements;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.ui.client.hud.GuiHudDesigner;
import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.notifications.FadeState;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.notifications.Notification;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ElementInfo(name = "Notifications")
public class Notifications extends Element {

    private final Notification exampleNotification = new Notification("Example Notification");

    @Override
    public void drawElement() {
        if(LiquidBounce.CLIENT.hud.getNotifications().size() > 0)
            LiquidBounce.CLIENT.hud.getNotifications().get(0).draw(this);

        if (mc.currentScreen instanceof GuiHudDesigner) {
            if(!LiquidBounce.CLIENT.hud.getNotifications().contains(exampleNotification))
                LiquidBounce.CLIENT.hud.addNotification(exampleNotification);

            exampleNotification.fadeState = FadeState.STAY;
            exampleNotification.x = exampleNotification.textLength + 8;

            final int[] location = getLocationFromFacing();

            RenderUtils.drawBorderedRect(location[0] - 95, location[1] - 20, location[0], location[1], 3, Integer.MIN_VALUE, 0);
        }
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

        return mouseX <= location[0] && mouseY <= location[1] && mouseX >= location[0] - 95 && mouseY >= location[1] - 20;
    }
}