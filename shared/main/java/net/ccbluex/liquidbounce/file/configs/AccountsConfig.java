/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs;

import com.google.gson.*;
import net.ccbluex.liquidbounce.file.FileConfig;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AccountsConfig extends FileConfig {
    private final List<MinecraftAccount> accounts = new ArrayList<>();

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
        clearAccounts();
        try {
            final JsonElement jsonElement = new JsonParser().parse(new BufferedReader(new FileReader(getFile())));

            if (jsonElement instanceof JsonNull)
                return;

            for (final JsonElement accountElement : jsonElement.getAsJsonArray()) {
                JsonObject accountObject = accountElement.getAsJsonObject();
                JsonElement name = accountObject.get("name");
                JsonElement password = accountObject.get("password");
                JsonElement inGameName = accountObject.get("inGameName");

                if (inGameName == null || inGameName.isJsonNull())
                    addAccount(name.getAsString(), password.getAsString());
                else if (inGameName.isJsonNull() && password.isJsonNull())
                    addAccount(name.getAsString());
                else
                    addAccount(name.getAsString(), accountObject.get("password").getAsString(), accountObject.get("inGameName").getAsString());
            }

        } catch (JsonSyntaxException | IllegalStateException ex) {
            //When the JSON Parse fail, the client try to load and update the old config
            ClientUtils.getLogger().info("[FileManager] Try to load old Accounts config...");
            final List<String> accountList = new Gson().fromJson(new BufferedReader(new FileReader(getFile())), List.class);

            if (accountList == null)
                return;

            accounts.clear();

            for (final String account : accountList) {
                final String[] information = account.split(":");

                if (information.length >= 3)
                    accounts.add(new MinecraftAccount(information[0], information[1], information[2]));
                else if (information.length == 2)
                    accounts.add(new MinecraftAccount(information[0], information[1]));
                else
                    accounts.add(new MinecraftAccount(information[0]));
            }
            ClientUtils.getLogger().info("[FileManager] Loaded old Accounts config...");

            //Save the accounts into a new valid JSON file
            saveConfig();
            ClientUtils.getLogger().info("[FileManager] Saved Accounts to new config...");
        }

    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Override
    protected void saveConfig() throws IOException {
        final JsonArray jsonArray = new JsonArray();

        for (final MinecraftAccount minecraftAccount : accounts) {
            JsonObject friendObject = new JsonObject();
            friendObject.addProperty("name", minecraftAccount.getName());
            friendObject.addProperty("password", minecraftAccount.getPassword());
            friendObject.addProperty("inGameName", minecraftAccount.getAccountName());
            jsonArray.add(friendObject);
        }

        final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
        printWriter.println(FileManager.PRETTY_GSON.toJson(jsonArray));
        printWriter.close();
    }

    /**
     * Add cracked account to config
     *
     * @param name of account
     */
    public void addAccount(final String name) {
        if (accountExists(name))
            return;

        accounts.add(new MinecraftAccount(name));
    }

    /**
     * Add account to config
     *
     * @param name     of account
     * @param password of password
     */
    public void addAccount(final String name, final String password) {
        if (accountExists(name))
            return;

        accounts.add(new MinecraftAccount(name, password));
    }

    /**
     * Add account to config
     *
     * @param name     of account
     * @param password of account
     */
    public void addAccount(final String name, final String password, final String inGameName) {
        if (accountExists(name))
            return;

        accounts.add(new MinecraftAccount(name, password, inGameName));
    }

    /**
     * Remove account from config
     *
     * @param selectedSlot of the account
     */
    public void removeAccount(final int selectedSlot) {
        accounts.remove(selectedSlot);
    }


    /**
     * Removed an account from the config
     *
     * @param account the account
     */
    public void removeAccount(MinecraftAccount account) {
        accounts.remove(account);
    }

    /**
     * Check if the account is already added
     *
     * @param name of account
     * @return if the account exists
     */
    public boolean accountExists(final String name) {
        for (final MinecraftAccount minecraftAccount : accounts)
            if (minecraftAccount.getName().equals(name))
                return true;
        return false;
    }

    /**
     * Clear all minecraft accounts from alt array
     */
    public void clearAccounts() {
        accounts.clear();
    }

    /**
     * Get Alts
     *
     * @return list of Alt Accounts
     */
    public List<MinecraftAccount> getAccounts() {
        return accounts;
    }
}
