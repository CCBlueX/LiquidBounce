package net.ccbluex.liquidbounce.ui.client.hud.element;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class Facing {

    private Horizontal horizontal;
    private Vertical vertical;

    public Facing(Horizontal horizontal, Vertical vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public Horizontal getHorizontal() {
        return horizontal;
    }

    public void setHorizontal(Horizontal horizontal) {
        this.horizontal = horizontal;
    }

    public Vertical getVertical() {
        return vertical;
    }

    public void setVertical(Vertical vertical) {
        this.vertical = vertical;
    }

    @Override
    public String toString() {
        return horizontal.getName() + "," + vertical.getName();
    }

    public enum Horizontal {

        LEFT("Left"), MIDDLE("Middle"), RIGHT("Right");

        private final String name;

        Horizontal(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Horizontal getByName(final String name) {
            for(final Horizontal horizontal : values())
                if(horizontal.getName().equals(name))
                    return horizontal;
            return null;
        }
    }

    public enum Vertical {

        UP("Up"), MIDDLE("Middle"), DOWN("Down");

        private final String name;

        Vertical(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Vertical getByName(final String name) {
            for(final Vertical vertical : values())
                if(vertical.getName().equals(name))
                    return vertical;
            return null;
        }
    }
}