package net.ccbluex.liquidbounce.interfaces;

public interface PlayerInventoryAdditions {
    /**
     * @return the actual selected slot without any modification by {@link net.ccbluex.liquidbounce.utils.client.SilentHotbar}
     */
    int liquid_bounce_getRealSelectedSlot();
}
