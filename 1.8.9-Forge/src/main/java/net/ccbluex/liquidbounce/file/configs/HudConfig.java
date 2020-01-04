package net.ccbluex.liquidbounce.file.configs;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.file.FileConfig;
import net.ccbluex.liquidbounce.ui.client.hud.Config;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class HudConfig extends FileConfig {

    /**
     * Constructor of config
     *
     * @param file of config
     */
    public HudConfig(final File file) {
        super(file);
    }

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Override
    protected void loadConfig() throws IOException {
        LiquidBounce.CLIENT.hud.clearElements();
        LiquidBounce.CLIENT.hud = new Config(FileUtils.readFileToString(getFile())).toHUD();
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Override
    protected void saveConfig() throws IOException {
        final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
        printWriter.println(new Config(LiquidBounce.CLIENT.hud).toJson());
        printWriter.close();
    }
}
