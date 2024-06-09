package net.ccbluex.liquidbounce.interfaces;

/**
 * Additions to {@link net.minecraft.client.gui.hud.ChatHudLine}.
 */
public interface ChatHudLineAddition {

    /**
     * Sets the count of the message.
     * This indicates how many times this massage has already been sent in
     * {@link net.ccbluex.liquidbounce.features.module.modules.misc.ModuleBetterChat}.
     */
    void liquid_bounce$setCount(int count);

    /**
     * Gets the count stored in this line.
     */
    @SuppressWarnings("unused")
    int liquid_bounce$getCount();

}
