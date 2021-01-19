package net.mcleaks;

public class RedeemResponse
{

	private final String username;
	private final String token;

	RedeemResponse(final String username, final String token)
	{
		this.username = username;
		this.token = token;
	}

	public String getUsername()
	{
		return username;
	}

	public String getToken()
	{
		return token;
	}
}
