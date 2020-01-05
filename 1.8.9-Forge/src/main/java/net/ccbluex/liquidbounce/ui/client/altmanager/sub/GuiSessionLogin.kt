package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.Session
import org.apache.http.HttpHeaders
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.apache.http.util.EntityUtils
import org.json.JSONArray
import org.json.JSONObject
import org.lwjgl.input.Keyboard
import java.util.*
import kotlin.concurrent.thread


class GuiSessionLogin(private val prevGui: GuiAltManager) : GuiScreen() {

    // Buttons
    private lateinit var loginButton: GuiButton

    // User Input Fields
    private lateinit var sessionTokenField: GuiTextField

    // Status
    private var status = ""


    /**
     * Initialize Session Login GUI
     */
    override fun initGui() {
        // Enable keyboard repeat events
        Keyboard.enableRepeatEvents(true)

        // Add buttons to screen


        loginButton = GuiButton(1, width / 2 - 100, height / 4 + 96, "Login")
        buttonList.add(loginButton)


        buttonList.add(GuiButton(0, width / 2 - 100, height / 4 + 120, "Back"))

        // Add fields to screen
        sessionTokenField = GuiTextField(666, Fonts.font40, width / 2 - 100, 80, 200, 20)
        sessionTokenField.isFocused = true
        sessionTokenField.maxStringLength = Integer.MAX_VALUE
        sessionTokenField

        // Call sub method
        super.initGui()
    }

    /**
     * Draw screen
     */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Draw background to screen
        drawBackground(0)
        Gui.drawRect(30, 30, width - 30, height - 30, Integer.MIN_VALUE)

        // Draw title and status
        drawCenteredString(Fonts.font35, "Session Login", width / 2, 36, 0xffffff)
        drawCenteredString(Fonts.font35, status, width / 2, height / 4 + 80, 0xffffff)

        // Draw fields
        sessionTokenField.drawTextBox()

        drawCenteredString(Fonts.font40, "§7Session Token:", width / 2 - 65, 66, 0xffffff)


        // Call sub method
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    /**
     * Handle button actions
     */
    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return

        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> {
                loginButton.enabled = false

                thread {
                    status = "§aParsing session"
                    val data = try {
                        sessionTokenField.text.split(".")[1]
                    } catch (e: Exception) {
                        loginButton.enabled = true
                        status = "§cFailed parsing the token"
                        return@thread
                    }
                    val dataDecoded = try {
                        Base64.getDecoder().decode(data)
                    } catch (e: Exception) {
                        loginButton.enabled = true
                        status = "§cFailed to decode the accesstoken"
                        return@thread
                    }

                    val dataDecodedStr = String(dataDecoded, Charsets.UTF_8)
                    val jsonElement: JsonElement = JsonParser().parse(dataDecodedStr)
                    val jsonObject = jsonElement.asJsonObject
                    val uuid = jsonObject.get("spr").asString
                    val accessToken = jsonObject.get("yggt").asString


                    status = "§aValidating session"
                    val httpClient = HttpClients.createDefault()
                    val headers = arrayOf(
                            BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                    )


                    // Check if the accesstoken is valid
                    val validateRequest = HttpPost("https://authserver.mojang.com/validate")
                    validateRequest.setHeaders(headers)
                    val body = StringEntity("{\"accessToken\": \"${accessToken}\"}")
                    validateRequest.entity = body
                    val validateResponse = httpClient.execute(validateRequest)
                    if (validateResponse.statusLine.statusCode != 204) {
                        loginButton.enabled = true
                        status = "§cSession is invalid"
                        return@thread
                    }


                    status = "§aGetting the username"
                    // Get latest minecraft username

                    val nameRequest = HttpGet("https://api.mojang.com/user/profiles/${uuid}/names")
                    val nameResponse = httpClient.execute(nameRequest)
                    if (nameResponse.statusLine.statusCode != 200) {
                        loginButton.enabled = true
                        status = "§cFailed to get usernames of the account"
                        return@thread
                    }

                    val usernamesJson = try {
                        JSONArray(EntityUtils.toString(nameResponse.entity))
                    } catch (e: Exception) {
                        loginButton.enabled = true
                        status = "§cFailed to parse usernames"
                        return@thread
                    }

                    val username = try {
                        JSONObject(usernamesJson.get(usernamesJson.length() - 1).toString()).getString("name")
                    } catch (e: Exception) {
                        loginButton.enabled = true
                        status = "§cFailed to get the current username"
                        return@thread
                    }

                    // Login into the session
                    mc.session = Session(username, uuid, accessToken, "mojang")
                    status = "§aLogged in"
                    prevGui.status = "§cYour name is now §f§l${username}§c."
                    LiquidBounce.CLIENT.eventManager.callEvent(SessionEvent())
                    loginButton.enabled = true
                }
            }
        }

        // Call sub method
        super.actionPerformed(button)
    }

    /**
     * Handle key typed
     */
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // Check if user want to escape from screen
        if (Keyboard.KEY_ESCAPE == keyCode) {
            // Send back to prev screen
            mc.displayGuiScreen(prevGui)

            // Quit
            return
        }

        // Check if field is focused, then call key typed
        if (sessionTokenField.isFocused) sessionTokenField.textboxKeyTyped(typedChar, keyCode)

        // Call sub method
        super.keyTyped(typedChar, keyCode)
    }

    /**
     * Handle mouse clicked
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // Call mouse clicked to field
        sessionTokenField.mouseClicked(mouseX, mouseY, mouseButton)

        // Call sub method
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Handle screen update
     */
    override fun updateScreen() {
        sessionTokenField.updateCursorCounter()
        super.updateScreen()
    }

    /**
     * Handle gui closed
     */
    override fun onGuiClosed() {
        // Disable keyboard repeat events
        Keyboard.enableRepeatEvents(false)

        // Call sub method
        super.onGuiClosed()
    }
}