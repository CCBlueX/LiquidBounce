/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;

import net.ccbluex.liquidbounce.file.FileConfig;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount.AltServiceType;

public class AccountsConfig extends FileConfig
{
	private final List<MinecraftAccount> accounts = new ArrayList<>();

	/**
	 * Constructor of config
	 *
	 * @param file
	 *             of config
	 */
	public AccountsConfig(final File file)
	{
		super(file);
	}

	/**
	 * Load config from file
	 *
	 * @throws IOException
	 */
	@Override
	protected void loadConfig() throws IOException
	{
		clearAccounts();
		try
		{
			final JsonElement jsonElement = new JsonParser().parse(new BufferedReader(new FileReader(getFile())));

			if (jsonElement instanceof JsonNull)
				return;

			for (final JsonElement accountElement : jsonElement.getAsJsonArray())
			{
				final JsonObject accountObject = accountElement.getAsJsonObject();
				final JsonElement type = accountObject.get("type");
				final JsonElement name = accountObject.get("name");
				final JsonElement password = accountObject.get("password");
				final JsonElement inGameName = accountObject.get("inGameName");
				final JsonElement bannedServers = accountObject.get("bannedServers");

				final MinecraftAccount account;
				if ((inGameName == null || inGameName.isJsonNull()) && !(password == null || password.isJsonNull()))
					account = new MinecraftAccount(AltServiceType.getById(type.getAsString()), name.getAsString(), password.getAsString());
				else if (type == null || type.isJsonNull())
					account = new MinecraftAccount(AltServiceType.MOJANG, name.getAsString());
				else
					account = (inGameName == null || inGameName.isJsonNull()) && (password == null || password.isJsonNull()) ? new MinecraftAccount(AltServiceType.getById(type.getAsString()), name.getAsString()) : new MinecraftAccount(AltServiceType.getById(type.getAsString()), name.getAsString(), password.getAsString(), inGameName.getAsString());

				if (!bannedServers.isJsonNull())
				{
					final List<String> servers = new ArrayList<>();
					for (final JsonElement element : bannedServers.getAsJsonArray())
						servers.add(element.getAsString());
					account.setBannedServers(servers);
				}

				addAccount(account);
			}

		}
		catch (final JsonSyntaxException | IllegalStateException ex)
		{
			// When the JSON Parse fail, the client try to load and update the old config
			ClientUtils.getLogger().info("[FileManager] Try to load old Accounts config...");
			final List<String> accountList = new Gson().fromJson(new BufferedReader(new FileReader(getFile())), List.class);

			if (accountList == null)
				return;

			accounts.clear();

			for (final String account : accountList)
			{
				final String[] info = account.split("/", 2);
				if (info.length > 0)
				{
					final String[] information = info[0].split(":");

					MinecraftAccount acc = null;

					if (information.length == 1)
						acc = new MinecraftAccount(AltServiceType.MOJANG, information[0]);
					else if (information.length == 2)
						acc = new MinecraftAccount(AltServiceType.getById(information[0]), information[1]);
					else if (information.length == 3)
						acc = new MinecraftAccount(AltServiceType.getById(information[0]), information[1], information[2]);
					else if (information.length >= 4)
						acc = new MinecraftAccount(AltServiceType.getById(information[0]), information[1], information[2], information[3]);

					if (acc != null)
					{
						if (info.length > 1)
							acc.setBannedServers(MinecraftAccount.deserializeBannedServers(info[1]));
						accounts.add(acc);
					}
				}
			}
			ClientUtils.getLogger().info("[FileManager] Loaded old Accounts config...");

			// Save the accounts into a new valid JSON file
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
	protected void saveConfig() throws IOException
	{
		final JsonArray jsonArray = new JsonArray();

		for (final MinecraftAccount minecraftAccount : accounts)
		{
			final JsonObject accountObject = new JsonObject();
			accountObject.addProperty("type", minecraftAccount.getServiceType().getId());
			accountObject.addProperty("name", minecraftAccount.getName());
			accountObject.addProperty("password", minecraftAccount.getPassword());
			accountObject.addProperty("inGameName", minecraftAccount.getAccountName());
			if (!minecraftAccount.getBannedServers().isEmpty())
			{
				final JsonArray arr = new JsonArray();
				for (final String server : minecraftAccount.getBannedServers())
					arr.add(new JsonPrimitive(server));
				accountObject.add("bannedServers", arr);
			}
			jsonArray.add(accountObject);
		}

		final PrintWriter printWriter = new PrintWriter(new FileWriter(getFile()));
		printWriter.println(FileManager.PRETTY_GSON.toJson(jsonArray));
		printWriter.close();
	}

	/**
	 * Add cracked account to config
	 *
	 * @param account
	 *             The account
	 */
	public void addAccount(final MinecraftAccount account)
	{
		if (accountExists(account.getName()))
			return;

		accounts.add(account);
	}

	/**
	 * Remove account from config
	 *
	 * @param selectedSlot
	 *                     of the account
	 */
	public void removeAccount(final int selectedSlot)
	{
		accounts.remove(selectedSlot);
	}

	/**
	 * Removed an account from the config
	 *
	 * @param account
	 *                the account
	 */
	public void removeAccount(final MinecraftAccount account)
	{
		accounts.remove(account);
	}

	/**
	 * Check if the account is already added
	 *
	 * @param  name
	 *              of account
	 * @return      if the account exists
	 */
	public boolean accountExists(final String name)
	{
		for (final MinecraftAccount minecraftAccount : accounts)
			if (minecraftAccount.getName().equals(name))
				return true;
		return false;
	}

	/**
	 * Clear all minecraft accounts from alt array
	 */
	public void clearAccounts()
	{
		accounts.clear();
	}

	/**
	 * Get Alts
	 *
	 * @return list of Alt Accounts
	 */
	public List<MinecraftAccount> getAccounts()
	{
		return accounts;
	}
}
