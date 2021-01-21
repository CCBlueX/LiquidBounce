/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.login;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;

import org.jetbrains.annotations.Nullable;

public final class MinecraftAccount
{
	private AltServiceType serviceType;
	private final String name;
	private String password;
	private String inGameName;

	private List<String> bannedServers = new ArrayList<>();

	public MinecraftAccount(final AltServiceType serviceType, final String name)
	{
		this.serviceType = serviceType;
		this.name = name;
	}

	public MinecraftAccount(final AltServiceType serviceType, final String name, final List<String> bannedServers)
	{
		this.serviceType = serviceType;
		this.name = name;
		this.bannedServers = bannedServers;
	}

	public MinecraftAccount(final AltServiceType serviceType, final String name, final String password)
	{
		this.serviceType = serviceType;
		this.name = name;
		this.password = password;
	}

	public MinecraftAccount(final AltServiceType serviceType, final String name, final String password, final List<String> bannedServers)
	{
		this.serviceType = serviceType;
		this.name = name;
		this.password = password;
		this.bannedServers = bannedServers;
	}

	public MinecraftAccount(final AltServiceType serviceType, final String name, final String password, final String inGameName)
	{
		this.serviceType = serviceType;
		this.name = name;
		this.password = password;
		this.inGameName = inGameName;
	}

	public MinecraftAccount(final AltServiceType serviceType, final String name, final String password, final String inGameName, final List<String> bannedServers)
	{
		this.serviceType = serviceType;
		this.name = name;
		this.password = password;
		this.inGameName = inGameName;
		this.bannedServers = bannedServers;
	}

	public boolean isCracked()
	{
		return password == null || password.isEmpty();
	}

	public String getName()
	{
		return name;
	}

	public String getPassword()
	{
		return password;
	}

	public String getAccountName()
	{
		return inGameName;
	}

	public AltServiceType getServiceType()
	{
		return serviceType;
	}

	public void setServiceType(AltServiceType serviceType)
	{
		this.serviceType = serviceType;
	}

	public List<String> getBannedServers()
	{
		return bannedServers;
	}

	public void setBannedServers(final List<String> bannedServers)
	{
		this.bannedServers = bannedServers;
	}

	public void setAccountName(final String accountName)
	{
		inGameName = accountName;
	}

	public String serializeBannedServers()
	{
		return bannedServers.isEmpty() ? "" : Joiner.on(", ").join(bannedServers);
	}

	public static List<String> deserializeBannedServers(final String str)
	{
		final String[] split = str.split(";");
		return new ArrayList<>(Arrays.asList(split));
	}

	public enum AltServiceType
	{
		MOJANG("Mojang", null),
		MOJANG_INVALID("Mojang(Invalid)", MOJANG),
		MOJANG_MIGRATED("Mojang(Migrated)", MOJANG),

		MCLEAKS("MCLeaks", null),
		MCLEAKS_INVALID("MCLeaks(Invalid)", MCLEAKS),

		THEALTENING("TheAltening", null),
		THEALTENING_INVALID("TheAltening(Invalid)", THEALTENING);

		private final String id;
		private final AltServiceType parent;

		AltServiceType(final String id, final AltServiceType parent)
		{
			this.id = id;
			this.parent = parent;
		}

		@Nullable
		public static AltServiceType getById(final String id)
		{
			return Arrays.stream(values()).filter(altServiceType -> altServiceType.id.equalsIgnoreCase(id)).findFirst().orElse(null);
		}

		public final String getId()
		{
			return id;
		}

		public final boolean equals(final AltServiceType other)
		{
			return other != null && (this == other || parent != null && other == parent || this == other.parent);
		}
	}
}
