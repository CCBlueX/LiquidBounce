package net.ccbluex.liquidbounce.features.module;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.Listenable;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.notifications.Notification;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.ccbluex.liquidbounce.utils.render.ChatColor;
import net.ccbluex.liquidbounce.value.Value;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
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
public class Module extends MinecraftInstance implements Listenable {

    protected String name = getClass().getAnnotation(ModuleInfo.class).name();
    protected String description = getClass().getAnnotation(ModuleInfo.class).description();
    protected ModuleCategory category = getClass().getAnnotation(ModuleInfo.class).category();
    protected boolean state;
    private int keyBind = getClass().getAnnotation(ModuleInfo.class).keyBind();
    private final boolean canEnable = getClass().getAnnotation(ModuleInfo.class).canEnable();

    // HUD
    public final float hue = (float) Math.random();
    public float slide;

    public String getName() {
        return name;
    }

    public String getTagName() {
        return getName() + (getTag() == null ? "" : " §7" + getTag());
    }

    public String getColorlessTagName() {
        return getName() + (getTag() == null ? "" : " " + ChatColor.stripColor(getTag()));
    }

    public String getDescription() {
        return description;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public boolean getState() {
        return state;
    }

    public int getKeyBind() {
        return keyBind;
    }

    public void setKeyBind(final int keyBind) {
        this.keyBind = keyBind;
        LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.modulesConfig);
    }

    public String getTag() {
        return null;
    }

    public void setState(final boolean state) {
        try {
            if(getState() == state)
                return;

            onToggle(state);

            if(state) {
                onEnable();
                if(canEnable)
                    this.state = true;
            }else{
                onDisable();
                this.state = false;
            }

            LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.modulesConfig);
        }catch(final Exception e) {
            e.printStackTrace();
        }
    }

    public void toggle() {
        setState(!getState());
    }

    public void onToggle(final boolean state) {
        if(!LiquidBounce.CLIENT.isStarting && getState() != state) {
            mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("random.click"), 1.0F));
            LiquidBounce.CLIENT.hud.addNotification(new Notification((state ? "Enabled " : "Disabled ") + getName()));
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onStarted() {
    }

    public boolean showArray() {
        return true;
    }

    // Value

    public Value getValue(final String valueName) {
        for(final Field field : getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                final Object o = field.get(this);

                if(o instanceof Value) {
                    final Value value = (Value) o;

                    if(value.getName().equalsIgnoreCase(valueName))
                        return value;
                }
            }catch(IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public List<Value> getValues() {
        final List<Value> values = new ArrayList<>();

        for(final Field field : getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                final Object o = field.get(this);

                if(o instanceof Value)
                    values.add((Value) o);
            }catch(IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return values;
    }

    @Override
    public boolean handleEvents() {
        return getState();
    }
}
