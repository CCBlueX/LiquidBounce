package net.ccbluex.liquidbounce.ui.client.hud;

import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.ui.client.hud.element.Facing;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.*;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public class DefaultHUD extends HUD {

    public DefaultHUD() {
        addElement(
                new Text()
                        .setText("%clientName%")
                        .setShadow(true)
                        .setRainbow(false)
                        .setColor(new Color(0, 111, 255))
                        .setFontRenderer(Fonts.font40)
                        .setX(2)
                        .setY(2)
                        .setScale(2)
                        .setFacing(new Facing(Facing.Horizontal.LEFT, Facing.Vertical.UP))
        );

        addElement(
                new TabGUI()
                        .setFontRenderer(Fonts.font35)
                        .setRainbow(false)
                        .setColor(new Color(0, 148, 255, 140))
                        .setX(5)
                        .setY(25)
                        .setFacing(new Facing(Facing.Horizontal.LEFT, Facing.Vertical.UP))
        );

        addElement(
                new Arraylist()
                        .setFontRenderer(Fonts.font40)
                        .setX(1)
                        .setY(2)
                        .setFacing(new Facing(Facing.Horizontal.RIGHT, Facing.Vertical.UP))
        );

        addElement(
                new Armor()
                        .setX(8)
                        .setY(57)
                        .setFacing(new Facing(Facing.Horizontal.MIDDLE, Facing.Vertical.DOWN))
        );

        addElement(new Notifications()
                .setX(0)
                .setY(30)
                .setFacing(new Facing(Facing.Horizontal.RIGHT, Facing.Vertical.DOWN))
        );

        addElement(
                new Effects()
                        .setFontRenderer(Fonts.font35)
                        .setShadow(true)
                        .setX(2)
                        .setY(10)
                        .setFacing(new Facing(Facing.Horizontal.RIGHT, Facing.Vertical.DOWN))
        );
    }
}