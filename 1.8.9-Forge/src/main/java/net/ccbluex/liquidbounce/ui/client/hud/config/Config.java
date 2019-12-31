package net.ccbluex.liquidbounce.ui.client.hud.config;

import com.google.gson.*;
import net.ccbluex.liquidbounce.ui.client.hud.DefaultHUD;
import net.ccbluex.liquidbounce.ui.client.hud.HUD;
import net.ccbluex.liquidbounce.ui.client.hud.element.Element;
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo;
import net.ccbluex.liquidbounce.ui.client.hud.element.Facing;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.*;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.value.Value;
import net.minecraft.client.gui.FontRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public class Config {

    private JsonArray jsonArray = new JsonArray();

    private static final Class<? extends Element>[] ELEMENTS = new Class[]{
            Armor.class,
            Arraylist.class,
            Effects.class,
            Image.class,
            Model.class,
            Notifications.class,
            TabGUI.class,
            Text.class
    };

    public Config(final String config) {
        jsonArray = new Gson().fromJson(config, JsonArray.class);
    }

    public Config(final HUD hud) {
        for(final Element element : hud.getElements()) {
            final JsonObject elementObject = new JsonObject();

            elementObject.addProperty("Type", element.getName());
            elementObject.addProperty("X", element.getX());
            elementObject.addProperty("Y", element.getY());
            elementObject.addProperty("Scale", element.getScale());
            elementObject.addProperty("HorizontalFacing", element.getFacing().getHorizontal().getName());
            elementObject.addProperty("VerticalFacing", element.getFacing().getVertical().getName());

            for(final Field field : element.getClass().getDeclaredFields()) {
                try {
                    field.setAccessible(true);

                    final Object o = field.get(element);

                    if(o instanceof Value) {
                        final Value value = (Value) o;

                        elementObject.add(value.getName(), value.toJson());
                    }

                    if(o instanceof FontRenderer) {
                        final JsonObject fontObject = new JsonObject();
                        final Object[] fontDetails = Fonts.getFontDetails((FontRenderer) o);

                        fontObject.addProperty("fontName", (String) fontDetails[0]);
                        fontObject.addProperty("fontSize", (int) fontDetails[1]);

                        elementObject.add("font", fontObject);
                    }
                }catch(final IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            jsonArray.add(elementObject);
        }
    }

    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(jsonArray);
    }

    public HUD toHUD() {
        final HUD hud = new HUD();

        try {
            for(final JsonElement jsonElement : jsonArray) {
                try {
                    if(jsonElement == null || jsonElement instanceof JsonNull || !jsonElement.isJsonObject())
                        continue;

                    final JsonObject jsonObject = jsonElement.getAsJsonObject();

                    if(!jsonObject.has("Type"))
                        continue;

                    final String type = jsonObject.get("Type").getAsString();

                    for (final Class<? extends Element> c : ELEMENTS) {
                        final String classType = c.getAnnotation(ElementInfo.class).name();

                        if(classType.equals(type)) {
                            final Element element = c.newInstance();

                            element.setX(jsonObject.get("X").getAsInt());
                            element.setY(jsonObject.get("Y").getAsInt());
                            element.setScale(jsonObject.get("Scale").getAsFloat());
                            element.setFacing(new Facing(Facing.Horizontal.getByName(jsonObject.get("HorizontalFacing").getAsString()), Facing.Vertical.getByName(jsonObject.get("VerticalFacing").getAsString())));

                            for(final Field field : element.getClass().getDeclaredFields()) {
                                try {
                                    field.setAccessible(true);

                                    final Object o = field.get(element);

                                    if(o instanceof Value) {
                                        final Value value = (Value) o;

                                        if(jsonObject.has(value.getName()))
                                            value.fromJson(jsonObject.get(value.getName()));
                                    }

                                    if(o instanceof FontRenderer) {
                                        if(jsonObject.has("font")) {
                                            final JsonObject fontObject = jsonObject.get("font").getAsJsonObject();

                                            field.set(element, Fonts.getFontRenderer(fontObject.get("fontName").getAsString(), fontObject.get("fontSize").getAsInt()));
                                        }
                                    }
                                }catch(final Exception e) {
                                    ClientUtils.getLogger().error("Error while loading value of custom hud element from config.", e);
                                }
                            }

                            hud.addElement(element);
                            break;
                        }
                    }
                }catch(final Exception e) {
                    ClientUtils.getLogger().error("Error while loading custom hud element from config.", e);
                }
            }
        }catch(final Exception e) {
            ClientUtils.getLogger().error("Error while loading custom hud config.", e);

            return new DefaultHUD();
        }

        return hud;
    }
}