package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.text.Text;

/**
 * Additions to {@link net.minecraft.client.gui.hud.ChatHud}.
 */
public interface ChatHudAddition {

    @SuppressWarnings("unused")
    void liquid_bounce$addMessage(Text message, String id, int count);

    @SuppressWarnings("unused")
    void liquid_bounce$removeMessage(String id);

}
