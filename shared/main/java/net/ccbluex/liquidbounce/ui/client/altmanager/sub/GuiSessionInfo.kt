/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.WorkerUtils.workers
import net.ccbluex.liquidbounce.utils.login.UserUtils.getUsername
import net.ccbluex.liquidbounce.utils.login.UserUtils.isValidTokenStatus
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

class GuiSessionInfo(private val prevGui: IGuiScreen, private val defaultSessionId: String?) : WrappedGuiScreen()
{
	private lateinit var sessionIdField: IGuiTextField
	private lateinit var decodeButton: IGuiButton
	private lateinit var clipboardButton: IGuiButton
	private lateinit var loginButton: IGuiButton

	private var status = "\u00A77Idle..."

	/**
	 * Token Algorithm
	 */
	private var algorithm: String? = null

	/**
	 * Token Type
	 */
	private var type: String? = null

	/**
	 * Token Subject
	 */
	private var subject: String? = null

	/**
	 * Access token
	 */
	private var accesstoken: String? = null
	private var accesstokenInvalidMessage: String? = null
	private var accesstokenChecked = false

	/**
	 * Session UUID
	 */
	private var uuid: String? = null

	/**
	 * Session Nickname
	 */
	private var nickname: String? = null
	private var nicknameChecked = false

	/**
	 * Token issuer
	 */
	private var issuer: String? = null

	/**
	 * Token expired at
	 */
	private var expiredAt: String? = null

	/**
	 * Token issued at
	 */
	private var issuedAt: String? = null

	/**
	 * Token verifier signature
	 */
	private var verifySig: String? = null

	override fun initGui()
	{
		val width = representedScreen.width
		val height = representedScreen.height

		Keyboard.enableRepeatEvents(true)

		val middleScreen = width shr 1
		val buttonX = middleScreen - 100
		val buttonY = height - 54

		representedScreen.buttonList.add(classProvider.createGuiButton(1, buttonX, buttonY - 48, "Decode").also { decodeButton = it })
		representedScreen.buttonList.add(classProvider.createGuiButton(2, buttonX, buttonY - 24, "Clipboard").also { clipboardButton = it })
		representedScreen.buttonList.add(classProvider.createGuiButton(3, buttonX, buttonY - 72, "Login").also { loginButton = it })
		representedScreen.buttonList.add(classProvider.createGuiButton(0, buttonX, buttonY, "Back"))

		sessionIdField = classProvider.createGuiTextField(2, Fonts.font40, middleScreen - 300, 60, 600, 20).apply {
			isFocused = true
			maxStringLength = Int.MAX_VALUE
			text = if (defaultSessionId != null && defaultSessionId.isNotEmpty()) defaultSessionId else mc.session.token
		}

		loginButton.enabled = false
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		val width = representedScreen.width
		val height = representedScreen.height

		representedScreen.drawBackground(0)

		drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)

		val middleScreen = (width shr 1).toFloat()

		Fonts.font40.drawCenteredString("Decode session token", middleScreen, 34f, 0xffffff)

		if (status.isNotEmpty()) Fonts.font35.drawCenteredString(status, middleScreen, 95f, 0xffffff)

		// Display session information if present.

		// Header
		if (algorithm != null) Fonts.font35.drawCenteredString("Algorithm: $algorithm", middleScreen, 115f, 0xffffff)
		if (type != null) Fonts.font35.drawCenteredString("Type: $type", middleScreen, 135f, 0xffffff)

		// Payload
		if (subject != null) Fonts.font35.drawCenteredString("Subject: $subject", middleScreen, 155f, 0xffffff)
		if (accesstoken != null) Fonts.font35.drawCenteredString(if (accesstokenChecked) (if (accesstokenInvalidMessage == null) "\u00A7a" else "\u00A7c") + "Access Token: " + accesstoken + " \u00A78(" + (accesstokenInvalidMessage?.let { "\u00A7c$it" } ?: "\u00A7aValid") + "\u00A78)" else "\u00A78Access Token: $accesstoken \u00A78(Checking...)", middleScreen, 175f, Color.GREEN.rgb)
		if (uuid != null) Fonts.font35.drawCenteredString(if (nickname != null) "\u00A7aUUID: $uuid \u00A78(\u00A7a$nickname\u00A78)" else (if (nicknameChecked) "\u00A7c" else "\u00A78") + "UUID: " + uuid + " \u00A78(Unknown)", middleScreen, 195f, Color.GREEN.rgb)
		if (issuer != null) Fonts.font35.drawCenteredString("Issuer: $issuer", middleScreen, 215f, 0xffffff)
		if (issuedAt != null) Fonts.font35.drawCenteredString("Issued at: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH).format(Date(issuedAt!!.toLong() * 1000L)), middleScreen, 235f, 0xffffff)
		if (expiredAt != null) Fonts.font35.drawCenteredString("Expiration: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH).format(Date(expiredAt!!.toLong() * 1000L)), middleScreen, 255f, 0xffffff)

		// Verify Signature
		if (verifySig != null) Fonts.font35.drawCenteredString("Verify Signature: $verifySig", middleScreen, 295f, 0xffffff)

		sessionIdField.drawTextBox()

		if (sessionIdField.text.isEmpty() && !sessionIdField.isFocused) Fonts.font40.drawCenteredString("\u00A77Session ID", middleScreen, 66f, 0xffffff)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	@Throws(IOException::class)
	override fun actionPerformed(button: IGuiButton)
	{
		if (!button.enabled) return
		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui)

			1 ->
			{
				val token = sessionIdField.text

				processToken(if (token.startsWith("token:")) token.split(":", ignoreCase = true, limit = 3).toTypedArray()[1] else token)
			}

			2 -> try
			{
				val clipboardData = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
				val jwt = if (clipboardData.startsWith("token:")) clipboardData.split(":", ignoreCase = true, limit = 3).toTypedArray()[1] else clipboardData

				if (processToken(jwt)) sessionIdField.text = jwt
			}
			catch (e: UnsupportedFlavorException)
			{
				status = "\u00A7cClipboard flavor unsupported!"
				logger.error("Failed to read data from clipboard.", e)
			}

			3 ->
			{
				val jwtToken: String
				val token2 = sessionIdField.text

				jwtToken = if (token2.startsWith("token:")) token2.split(":", ignoreCase = true, limit = 3).toTypedArray()[1] else token2

				val sl = GuiSessionLogin(representedScreen)

				mc.displayGuiScreen(sl.representedScreen)
				sl.processToken(jwtToken)
			}
		}
		super.actionPerformed(button)
	}

	@Throws(IOException::class)
	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		when (keyCode)
		{
			Keyboard.KEY_ESCAPE ->
			{
				mc.displayGuiScreen(prevGui)
				return
			}

			Keyboard.KEY_RETURN ->
			{
				actionPerformed(decodeButton)
				return
			}
		}
		if (sessionIdField.isFocused) sessionIdField.textboxKeyTyped(typedChar, keyCode)
		super.keyTyped(typedChar, keyCode)
	}

	@Throws(IOException::class)
	override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
	{
		sessionIdField.mouseClicked(mouseX, mouseY, mouseButton)

		super.mouseClicked(mouseX, mouseY, mouseButton)
	}

	override fun updateScreen()
	{
		sessionIdField.updateCursorCounter()
		super.updateScreen()
	}

	override fun onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false)
		super.onGuiClosed()
	}

	private fun processToken(token: String): Boolean
	{
		val tokenPieces = token.split("\\.", ignoreCase = true, limit = 3).toTypedArray()
		if (tokenPieces.size < 3)
		{
			status = "\u00A7cSession token is invalid! (pieces: " + tokenPieces.size + ")"
			return false
		}

		var header: String? = null
		var payload: String? = null
		var success = true

		try
		{
			header = String(Base64.getDecoder().decode(tokenPieces[0]), StandardCharsets.UTF_8)
			payload = String(Base64.getDecoder().decode(tokenPieces[1]), StandardCharsets.UTF_8)
			verifySig = tokenPieces[2]
		}
		catch (e: Exception)
		{
			status = "\u00A7cSession token is invalid! ($e)"
			success = false
		}
		if (header != null)
		{
			algorithm = null
			var headerJson: JsonObject? = null
			try
			{
				headerJson = JsonParser().parse(header).asJsonObject
			}
			catch (e: Exception)
			{
				status = "\u00A7cFailed to parse header from session token!"
				success = false
			}
			if (headerJson != null)
			{
				if (headerJson.has("alg")) algorithm = headerJson["alg"].asString
				else
				{
					status = "\u00A7cFailed to parse algorithm member from header! This cannot be happened!"
					success = false
				}
				if (headerJson.has("typ")) type = headerJson["typ"].asString
			}
		}
		if (payload != null)
		{
			subject = null
			accesstoken = null
			accesstokenInvalidMessage = null
			accesstokenChecked = false
			uuid = null
			nickname = null
			nicknameChecked = false
			issuer = null
			expiredAt = null
			issuedAt = null

			var payloadJson: JsonObject? = null
			try
			{
				payloadJson = JsonParser().parse(payload).asJsonObject
			}
			catch (e: Exception)
			{
				status = "\u00A7cFailed to parse payload from session token!"
				success = false
			}

			if (payloadJson != null)
			{
				if (payloadJson.has("sub")) subject = payloadJson["sub"].asString
				else
				{
					status = "\u00A7cFailed to parse subject member from payload!"
					success = false
				}

				// Validate Token
				if (payloadJson.has("yggt"))
				{
					val accesstoken = payloadJson["yggt"].asString.also { accesstoken = it }
					workers.submit {
						val status = isValidTokenStatus(accesstoken)
						val tokenValid = status.statusCode == 204

						if (!tokenValid) accesstokenInvalidMessage = "Invalid token; HTTP " + status.statusCode + (status.reasonPhrase?.let { reasonPhrase: String -> " :: $reasonPhrase" } ?: "")

						loginButton.enabled = tokenValid
						accesstokenChecked = true
					}
				}
				else
				{
					status = "\u00A7cFailed to parse access token member(yggt) from payload!"
					success = false
				}

				// Validate UUID
				if (payloadJson.has("spr"))
				{
					val uuid = payloadJson["spr"].asString.also { uuid = it }
					workers.submit {
						nickname = getUsername(uuid)
						nicknameChecked = true
					}
				}
				else
				{
					status = "\u00A7cFailed to parse uuid member(spr) from payload!"
					success = false
				}
				if (payloadJson.has("iss")) issuer = payloadJson["iss"].asString
				if (payloadJson.has("exp")) expiredAt = payloadJson["exp"].asString
				if (payloadJson.has("iat")) issuedAt = payloadJson["iat"].asString
				else
				{
					status = "\u00A7cFailed to parse issued at member(iat) from payload! This cannot be happened!"
					success = false
				}
			}
		}
		if (success) status = "\u00A7aSession token successfully decoded."
		return success
	}
}
