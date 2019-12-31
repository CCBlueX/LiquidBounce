package net.ccbluex.liquidbounce.file;

import java.io.File;
import java.io.IOException;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public abstract class FileConfig {

    private final File file;

    /**
     * Constructor of config
     *
     * @param file of config
     */
    public FileConfig(final File file) {
        this.file = file;
    }

    /**
     * Load config from file
     *
     * @throws IOException
     */
    protected abstract void loadConfig() throws IOException;

    /**
     * Save config to file
     *
     * @throws IOException
     */
    protected abstract void saveConfig() throws IOException;

    /**
     * Create config
     *
     * @throws IOException
     */
    public void createConfig() throws IOException {
        file.createNewFile();
    }

    /**
     * @return config file exist
     */
    public boolean hasConfig() {
        return file.exists();
    }

    /**
     * @return file of config
     */
    public File getFile() {
        return file;
    }
}
