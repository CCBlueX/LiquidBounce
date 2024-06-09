package net.ccbluex.liquidbounce.interfaces;

/**
 * Additions to {@link net.minecraft.client.gui.hud.ChatHudLine} and
 * {@link net.minecraft.client.gui.hud.ChatHudLine.Visible}.
 */
public interface ChatMessageAddition {

    /**
     * Sets the ID for the chat message.
     * The ID will be used for removing chat messages.
     */
    void liquid_bounce$setId(String id);

    /**
     * Gets the ID of the chat message.
     */
    String liquid_bounce$getId();

}
