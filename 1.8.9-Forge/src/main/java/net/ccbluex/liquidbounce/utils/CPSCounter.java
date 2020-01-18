package net.ccbluex.liquidbounce.utils;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @author superblaubeere27
 * @game Minecraft
 */
public class CPSCounter {
    private static final int MAX_CPS = 50;
    private static final RollingArrayLongBuffer[] TIMESTAMP_BUFFERS = new RollingArrayLongBuffer[MouseButton.values().length];

    static {
        for (int i = 0; i < TIMESTAMP_BUFFERS.length; i++) {
            TIMESTAMP_BUFFERS[i] = new RollingArrayLongBuffer(MAX_CPS);
        }
    }

    /**
     * Registers a mouse button click
     *
     * @param button The clicked button
     */
    public static void registerClick(MouseButton button) {
        TIMESTAMP_BUFFERS[button.getIndex()].add(System.currentTimeMillis());
    }

    /**
     * Gets the count of clicks that have occurrence since the last 1000ms
     *
     * @param button The mouse button
     * @return The CPS
     */
    public static int getCPS(MouseButton button) {
        return TIMESTAMP_BUFFERS[button.getIndex()].getTimestampsSince(System.currentTimeMillis() - 1000L);
    }

    public enum MouseButton {
        LEFT(0), MIDDLE(1), RIGHT(2);

        private int index;

        MouseButton(int index) {
            this.index = index;
        }

        private int getIndex() {
            return index;
        }
    }

}
