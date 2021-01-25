/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField;
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.WorkerUtils;
import net.ccbluex.liquidbounce.utils.login.UserUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;

import org.apache.http.StatusLine;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Keyboard;

public class GuiSessionInfo extends WrappedGuiScreen
{

	private final IGuiScreen prevGui;

	private IGuiTextField sessionIdField;
	private IGuiButton decodeButton;
	private IGuiButton clipboardButton;
	private IGuiButton loginButton;

	private final String defaultSessionId;
	private String status = "\u00A77Idle...";

	@Nullable
	private String algorithm;
	@Nullable
	private String type;
	@Nullable
	private String subject;
	@Nullable
	private String accesstoken;
	private String accesstokenInvalidMessage;
	private boolean accesstokenChecked;
	@Nullable
	private String uuid;
	@Nullable
	private String nickname;
	private boolean nicknameChecked;
	@Nullable
	private String issuer;
	@Nullable
	private String expiredAt;
	@Nullable
	private String issuedAt;
	private String verifySig;

	public GuiSessionInfo(final IGuiScreen gui, final String defaultSessionId)
	{
		prevGui = gui;
		this.defaultSessionId = defaultSessionId;
	}

	@Override
	public final void initGui()
	{
		final int width = representedScreen.getWidth();
		final int height = representedScreen.getHeight();

		Keyboard.enableRepeatEvents(true);
		representedScreen.getButtonList().add(decodeButton = classProvider.createGuiButton(1, width / 2 - 100, height - 54 - 48, "Decode"));
		representedScreen.getButtonList().add(clipboardButton = classProvider.createGuiButton(2, width / 2 - 100, height - 54 - 24, "Clipboard"));
		representedScreen.getButtonList().add(loginButton = classProvider.createGuiButton(3, width / 2 - 100, height - 54 - 72, "Login"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(0, width / 2 - 100, height - 54, "Back"));
		sessionIdField = classProvider.createGuiTextField(2, Fonts.font40, width / 2 - 300, 60, 600, 20);
		sessionIdField.setFocused(true);
		sessionIdField.setMaxStringLength(Integer.MAX_VALUE);
		sessionIdField.setText(defaultSessionId != null && !defaultSessionId.isEmpty() ? defaultSessionId : mc.getSession().getToken());
		loginButton.setEnabled(false);
	}

	@Override
	public final void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		final int width = representedScreen.getWidth();
		final int height = representedScreen.getHeight();

		representedScreen.drawBackground(0);
		RenderUtils.drawRect(30, 30, width - 30, height - 30, Integer.MIN_VALUE);

		Fonts.font40.drawCenteredString("Decode session token", width / 2, 34, 0xffffff);

		if (status != null && !status.isEmpty())
			Fonts.font35.drawCenteredString(status, width / 2, 95, 0xffffff);

		// Display session information if present.

		// Header
		if (algorithm != null)
			Fonts.font35.drawCenteredString("Algorithm: " + algorithm, width / 2, 115, 0xffffff);

		if (type != null)
			Fonts.font35.drawCenteredString("Type: " + type, width / 2, 135, 0xffffff);

		// Payload
		if (subject != null)
			Fonts.font35.drawCenteredString("Subject: " + subject, width / 2, 155, 0xffffff);

		if (accesstoken != null)
			if (accesstokenChecked)
				Fonts.font35.drawCenteredString((accesstokenInvalidMessage == null ? "\u00A7a" : "\u00A7c") + "Access Token: " + accesstoken + " \u00A78(" + Optional.ofNullable(accesstokenInvalidMessage).map(invalidMessage -> "\u00A7c" + invalidMessage).orElse("\u00A7aValid") + "\u00A78)", width / 2, 175, Color.GREEN.getRGB());
			else
				Fonts.font35.drawCenteredString("\u00A78Access Token: " + accesstoken + " \u00A78(Checking...)", width / 2, 175, Color.GREEN.getRGB());

		if (uuid != null)
			if (nickname != null)
				Fonts.font35.drawCenteredString("\u00A7aUUID: " + uuid + " \u00A78(\u00A7a" + nickname + "\u00A78)", width / 2, 195, Color.GREEN.getRGB());
			else
				Fonts.font35.drawCenteredString((nicknameChecked ? "\u00A7c" : "\u00A78") + "UUID: " + uuid + " \u00A78(Unknown)", width / 2, 195, Color.GREEN.getRGB());

		if (issuer != null)
			Fonts.font35.drawCenteredString("Issuer: " + issuer, width / 2, 215, 0xffffff);

		if (issuedAt != null)
			Fonts.font35.drawCenteredString("Issued at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(Long.parseLong(issuedAt) * 1000L)), width / 2, 235, 0xffffff);

		if (expiredAt != null)
			Fonts.font35.drawCenteredString("Expiration: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(Long.parseLong(expiredAt) * 1000L)), width / 2, 255, 0xffffff);

		// Verify Signature
		if (verifySig != null)
			Fonts.font35.drawCenteredString("Verify Signature: " + verifySig, width / 2, 295, 0xffffff);

		sessionIdField.drawTextBox();

		if (sessionIdField.getText().isEmpty() && !sessionIdField.isFocused())
			Fonts.font40.drawCenteredString("\u00A77Session ID", width / 2, 66, 0xffffff);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public final void actionPerformed(final IGuiButton button) throws IOException
	{
		if (!button.getEnabled())
			return;

		switch (button.getId())
		{
			case 0:
				mc.displayGuiScreen(prevGui);
				break;
			case 1:
				final String token = sessionIdField.getText();
				if (token.startsWith("token:"))
					processToken(token.split(":", 3)[1]);
				else
					processToken(token);
				break;
			case 2:
				try
				{
					final String clipboardData = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
					final String jwt;
					if (clipboardData.startsWith("token:"))
						jwt = clipboardData.split(":", 3)[1];
					else
						jwt = clipboardData;
					if (processToken(jwt))
						sessionIdField.setText(jwt);
				}
				catch (final UnsupportedFlavorException e)
				{
					status = "\u00A7cClipboard flavor unsupported!";
					ClientUtils.getLogger().error("Failed to read data from clipboard.", e);
				}
				break;
			case 3:
				final String jwtToken;
				final String token2 = sessionIdField.getText();
				if (token2.startsWith("token:"))
					jwtToken = token2.split(":", 3)[1];
				else
					jwtToken = token2;
				final GuiSessionLogin sl = new GuiSessionLogin(this.representedScreen);
				mc.displayGuiScreen(sl.representedScreen);
				sl.processToken(jwtToken);
				break;
		}

		super.actionPerformed(button);
	}

	@Override
	public final void keyTyped(final char typedChar, final int keyCode) throws IOException
	{
		switch (keyCode)
		{
			case Keyboard.KEY_ESCAPE:
				mc.displayGuiScreen(prevGui);
				return;
			case Keyboard.KEY_RETURN:
				actionPerformed(decodeButton);
				return;
		}

		if (sessionIdField.isFocused())
			sessionIdField.textboxKeyTyped(typedChar, keyCode);

		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public final void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException
	{
		sessionIdField.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public final void updateScreen()
	{
		sessionIdField.updateCursorCounter();
		super.updateScreen();
	}

	@Override
	public final void onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false);
		super.onGuiClosed();
	}

	private boolean processToken(final String token)
	{
		boolean trouble = false;
		final String[] tokenPieces = token.split("\\.", 3);
		if (tokenPieces.length < 3)
		{
			status = "\u00A7cSession token is invalid! (pieces: " + tokenPieces.length + ")";
			return false;
		}

		String header = null;
		String payload = null;
		try
		{
			header = new String(Base64.getDecoder().decode(tokenPieces[0]));
			payload = new String(Base64.getDecoder().decode(tokenPieces[1]));
			verifySig = tokenPieces[2];
		}
		catch (final Exception e)
		{
			status = "\u00A7cSession token is invalid! (" + e + ")";
			trouble = true;
		}

		if (header != null)
		{
			algorithm = null;

			JsonObject headerJson = null;
			try
			{
				headerJson = new JsonParser().parse(header).getAsJsonObject();
			}
			catch (final Exception e)
			{
				status = "\u00A7cFailed to parse header from session token!";
				trouble = true;
			}

			if (headerJson != null)
			{
				if (headerJson.has("alg"))
					algorithm = headerJson.get("alg").getAsString();
				else
				{
					status = "\u00A7cFailed to parse algorithm member from header! This cannot be happened!";
					trouble = true;
				}

				if (headerJson.has("typ"))
					type = headerJson.get("typ").getAsString();
			}
		}

		if (payload != null)
		{
			subject = null;
			accesstoken = null;
			accesstokenInvalidMessage = null;
			accesstokenChecked = false;
			uuid = null;
			nickname = null;
			nicknameChecked = false;
			issuer = null;
			expiredAt = null;
			issuedAt = null;

			JsonObject payloadJson = null;
			try
			{
				payloadJson = new JsonParser().parse(payload).getAsJsonObject();
			}
			catch (final Exception e)
			{
				status = "\u00A7cFailed to parse payload from session token!";
				trouble = true;
			}
			if (payloadJson != null)
			{
				if (payloadJson.has("sub"))
					subject = payloadJson.get("sub").getAsString();
				else
				{
					status = "\u00A7cFailed to parse subject member from payload!";
					trouble = true;
				}

				// Validate Token
				if (payloadJson.has("yggt"))
				{
					accesstoken = payloadJson.get("yggt").getAsString();
					WorkerUtils.getWorkers().submit(() ->
					{
						final StatusLine status = UserUtils.INSTANCE.isValidTokenStatus(accesstoken);
						final boolean tokenValid = status.getStatusCode() == 204;

						if (!tokenValid)
							accesstokenInvalidMessage = "Invalid token; HTTP " + status.getStatusCode() + Optional.ofNullable(status.getReasonPhrase()).map(reasonPhrase -> " :: " + reasonPhrase).orElse("");

						loginButton.setEnabled(tokenValid);
						accesstokenChecked = true;
					});
				}
				else
				{
					status = "\u00A7cFailed to parse access token member(yggt) from payload!";
					trouble = true;
				}

				// Validate UUID
				if (payloadJson.has("spr"))
				{
					uuid = payloadJson.get("spr").getAsString();
					WorkerUtils.getWorkers().submit(() ->
					{
						nickname = UserUtils.INSTANCE.getUsername(uuid);
						nicknameChecked = true;
					});
				}
				else
				{
					status = "\u00A7cFailed to parse uuid member(spr) from payload!";
					trouble = true;
				}

				if (payloadJson.has("iss"))
					issuer = payloadJson.get("iss").getAsString();

				if (payloadJson.has("exp"))
					expiredAt = payloadJson.get("exp").getAsString();

				if (payloadJson.has("iat"))
					issuedAt = payloadJson.get("iat").getAsString();
				else
				{
					status = "\u00A7cFailed to parse issued at member(iat) from payload! This cannot be happened!";
					trouble = true;
				}
			}
		}

		if (!trouble)
			status = "\u00A7aSession token successfully decoded.";
		return !trouble;
	}
}
