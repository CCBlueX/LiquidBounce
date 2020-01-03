package net.ccbluex.liquidbounce.file;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kotlin.Pair;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.command.Command;
import net.ccbluex.liquidbounce.features.command.shortcuts.Shortcut;
import net.ccbluex.liquidbounce.file.configs.*;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Field;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
public class FileManager extends MinecraftInstance {

    public final File dir = new File(mc.mcDataDir, LiquidBounce.CLIENT_NAME + "-1.8");
    public final File fontsDir = new File(dir, "fonts");
    public final File settingsDir = new File(dir, "settings");
    public final File shortcutsDir = new File(dir, "shortcuts");

    public final FileConfig modulesConfig = new ModulesConfig(new File(dir, "modules.json"));
    public final FileConfig valuesConfig = new ValuesConfig(new File(dir, "values.json"));
    public final FileConfig clickGuiConfig = new ClickGuiConfig(new File(dir, "clickgui.json"));
    public final AccountsConfig accountsConfig = new AccountsConfig(new File(dir, "accounts.json"));
    public final FriendsConfig friendsConfig = new FriendsConfig(new File(dir, "friends.json"));
    public final FileConfig xrayConfig = new XRayConfig(new File(dir, "xray-blocks.json"));
    public final FileConfig hudConfig = new HudConfig(new File(dir, "hud.json"));

    public final File backgroundFile = new File(dir, "userbackground.png");

    public boolean firstStart =  false;

    public static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Constructor of file manager
     * Setup everything important
     */
    public FileManager() {
        setupFolder();
        loadBackground();
    }

    /**
     * Setup folder
     */
    public void setupFolder() {
        if(!dir.exists()) {
            dir.mkdir();
            firstStart = true;
        }

        if(!fontsDir.exists())
            fontsDir.mkdir();

        if(!settingsDir.exists())
            settingsDir.mkdir();

        if (!shortcutsDir.exists())
            shortcutsDir.mkdir();
    }

    /**
     * Load all configs in file manager
     */
    public void loadAllConfigs() {
        for(final Field field : getClass().getDeclaredFields()) {
            if(field.getType() == FileConfig.class) {
                try {
                    if(!field.isAccessible())
                        field.setAccessible(true);

                    final FileConfig fileConfig = (FileConfig) field.get(this);
                    loadConfig(fileConfig);
                }catch(final IllegalAccessException e) {
                    ClientUtils.getLogger().error("Failed to load config file of field " + field.getName() + ".", e);
                }
            }
        }
    }

    /**
     * Load all shortcuts.
     */
    public void loadShortcuts() {
        File[] shortcutFiles = shortcutsDir.listFiles((FilenameFilter) new WildcardFileFilter("*.lbsh", IOCase.INSENSITIVE));

        if (shortcutFiles != null) {
            for (File shortcutFile : shortcutFiles) {
                String filename = shortcutFile.getName();

                String shortcutName = filename.substring(0, filename.lastIndexOf('.'));

                StringBuilder shortcutData = new StringBuilder();

                char[] readBuf = new char[1024];
                int n;

                try (BufferedReader reader = new BufferedReader(new FileReader(shortcutFile))) {
                    while ((n = reader.read(readBuf)) > 0)
                        shortcutData.append(readBuf, 0, n);

                    LiquidBounce.CLIENT.commandManager.registerShortcut(shortcutName, shortcutData.toString());
                } catch (IOException | IllegalArgumentException e) {
                    ClientUtils.getLogger().error("Unable to load a shortcut!", e);
                }
            }
        } else {
            ClientUtils.getLogger().error("Unable to get list of shortcut files.");
        }
    }

    /**
     * Load a list of configs
     *
     * @param configs list
     */
    public void loadConfigs(final FileConfig... configs) {
        for(final FileConfig fileConfig : configs)
            loadConfig(fileConfig);
    }

    /**
     * Load one config
     *
     * @param config to load
     */
    public void loadConfig(final FileConfig config) {
        if(!config.hasConfig()) {
            ClientUtils.getLogger().info("[FileManager] Skipped loading config: " + config.getFile().getName() + ".");

            saveConfig(config, true);
            return;
        }

        try {
            config.loadConfig();
            ClientUtils.getLogger().info("[FileManager] Loaded config: " + config.getFile().getName() + ".");
        }catch(final Throwable t) {
            ClientUtils.getLogger().error("[FileManager] Failed to load config file: " + config.getFile().getName() + ".", t);
        }
    }

    /**
     * Save all configs in file manager
     */
    public void saveAllConfigs() {
        for(final Field field : getClass().getDeclaredFields()) {
            if(field.getType() == FileConfig.class) {
                try {
                    if(!field.isAccessible())
                        field.setAccessible(true);

                    final FileConfig fileConfig = (FileConfig) field.get(this);
                    saveConfig(fileConfig);
                }catch(final IllegalAccessException e) {
                    ClientUtils.getLogger().error("[FileManager] Failed to save config file of field " +
                            field.getName() + ".", e);
                }
            }
        }
    }

    /**
     * Save all shortcuts.
     */
    public void saveShortcuts() {
        for (Command command : LiquidBounce.CLIENT.commandManager.getCommands()) {
            if (command instanceof Shortcut) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(shortcutsDir, command.getCommand() + ".lbsh")))) {
                    for (Pair<Command, String[]> entry : ((Shortcut) command).getScript())
                        writer.write(StringUtils.join(entry.getSecond(), ' ') + ";\n");
                } catch (IOException e) {
                    ClientUtils.getLogger().error("Unable to save shortcut!", e);
                }
            }
        }
    }

    /**
     * Save a list of configs
     *
     * @param configs list
     */
    public void saveConfigs(final FileConfig... configs) {
        for(final FileConfig fileConfig : configs)
            saveConfig(fileConfig);
    }

    /**
     * Save one config
     *
     * @param config to save
     */
    public void saveConfig(final FileConfig config) {
        saveConfig(config, false);
    }

    /**
     * Save one config
     *
     * @param config         to save
     * @param ignoreStarting check starting
     */
    private void saveConfig(final FileConfig config, final boolean ignoreStarting) {
        if(!ignoreStarting && LiquidBounce.CLIENT.isStarting)
            return;

        try {
            if(!config.hasConfig())
                config.createConfig();

            config.saveConfig();
            ClientUtils.getLogger().info("[FileManager] Saved config: " + config.getFile().getName() + ".");
        }catch(final Throwable t) {
            ClientUtils.getLogger().error("[FileManager] Failed to save config file: " +
                    config.getFile().getName() + ".", t);
        }
    }

    /**
     * Load background for background
     */
    public void loadBackground() {
        if(backgroundFile.exists()) {
            try {
                final BufferedImage bufferedImage = ImageIO.read(new FileInputStream(backgroundFile));

                if(bufferedImage == null)
                    return;

                LiquidBounce.CLIENT.background = new ResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + "/background.png");
                mc.getTextureManager().loadTexture(LiquidBounce.CLIENT.background, new DynamicTexture(bufferedImage));
                ClientUtils.getLogger().info("[FileManager] Loaded background.");
            }catch(final Exception e) {
                ClientUtils.getLogger().error("[FileManager] Failed to load background.", e);
            }
        }
    }
}
