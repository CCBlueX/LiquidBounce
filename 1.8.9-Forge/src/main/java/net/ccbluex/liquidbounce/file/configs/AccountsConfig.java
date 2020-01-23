/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs;

import com.google.gson.Gson;
import net.ccbluex.liquidbounce.file.FileConfig;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AccountsConfig extends FileConfig {

    public final List<MinecraftAccount> altManagerMinecraftAccounts = new ArrayList<>();

    /**
     * Constructor of config
     *
     * @param file of config
     */
    public AccountsConfig(final File file) {
        super(file);
    }

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Override
    protected void loadConfig() throws IOException {
        final List<String> accountList = new Gson().fromJson(new BufferedReader(new FileReader(getFile())), List.class);

        if(accountList == null)
            return;

        altManagerMinecraftAccounts.clear();

        for(final String account : accountList) {
            final String[] information = account.split(":");

            if(information.length >= 3)
                altManagerMinecraftAccounts.add(new MinecraftAccount(information[0], information[1], information[2]));
            else if(information.length == 2)
                altManagerMinecraftAccounts.add(new MinecraftAccount(information[0], information[1]));
            else
                altManagerMinecraftAccounts.add(new MinecraftAccount(information[0]));
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Override
    protected void saveConfig() throws IOException {
        final List<String> accountList = new ArrayList<>();

        for(final MinecraftAccount minecraftAccount : altManagerMinecraftAccounts)
            accountList.add(minecraftAccount.getName() + ":" + (minecraftAccount.getPassword() == null ? "" : minecraftAccount.getPassword()) + ":" + (minecraftAccount.getAccountName() == null ? "" : minecraftAccount.getAccountName()));

        final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
        printWriter.println(FileManager.PRETTY_GSON.toJson(accountList));
        printWriter.close();
    }
}
