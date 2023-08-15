/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.thealtening.AltService
import me.liuli.elixir.account.CrackedAccount
import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.account.MojangAccount
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.file.FileManager.accountsConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiDonatorCape
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiLoginIntoAccount
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiSessionLogin
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.login.UserUtils.isValidTokenOffline
import net.ccbluex.liquidbounce.utils.misc.HttpUtils.get
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.randomAccount
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.*
import java.util.function.Consumer
import kotlin.concurrent.thread


class GuiAltManager(private val prevGui: GuiScreen) : GuiScreen() {

    var status = "§7Idle..."

    private lateinit var loginButton: GuiButton
    private lateinit var randomAltButton: GuiButton
    private lateinit var randomNameButton: GuiButton
    private lateinit var addButton: GuiButton
    private lateinit var removeButton: GuiButton
    private lateinit var copyButton: GuiButton
    private lateinit var altsList: GuiList
    private lateinit var searchField: GuiTextField

    override fun initGui() {
        val textFieldWidth = (width / 8).coerceAtLeast(70)
        searchField = GuiTextField(2, Fonts.font40, width - textFieldWidth - 10, 10, textFieldWidth, 20)
        searchField.maxStringLength = Int.MAX_VALUE

        altsList = GuiList(this)
        altsList.run {
            registerScrollButtons(7, 8)

            val mightBeTheCurrentAccount = accountsConfig.accounts.indexOfFirst { it.name == mc.session.username }
            elementClicked(mightBeTheCurrentAccount, false, 0, 0)

            scrollBy(mightBeTheCurrentAccount * altsList.getSlotHeight())
        }


        // Setup buttons

        val startPositionY = 22
        buttonList.run {
            add(GuiButton(1, width - 80, startPositionY + 24, 70, 20, "Add").also { addButton = it })
            add(GuiButton(2, width - 80, startPositionY + 24 * 2, 70, 20, "Remove").also { removeButton = it })
            add(GuiButton(7, width - 80, startPositionY + 24 * 3, 70, 20, "Import"))
            add(GuiButton(12, width - 80, startPositionY + 24 * 4, 70, 20, "Export"))
            add(GuiButton(8, width - 80, startPositionY + 24 * 5, 70, 20, "Copy").also { copyButton = it })
            add(GuiButton(0, width - 80, height - 65, 70, 20, "Back"))
            add(GuiButton(3, 5, startPositionY + 24, 90, 20, "Login").also { loginButton = it })
            add(GuiButton(4, 5, startPositionY + 24 * 2, 90, 20, "Random Alt").also { randomAltButton = it })
            add(GuiButton(5, 5, startPositionY + 24 * 3, 90, 20, "Random Name").also { randomNameButton = it })
            add(GuiButton(6, 5, startPositionY + 24 * 4, 90, 20, "Direct Login"))
            add(GuiButton(10, 5, startPositionY + 24 * 5, 90, 20, "Session Login"))

            if (activeGenerators.getOrDefault("thealtening", true)) {
                add(GuiButton(9, 5, startPositionY + 24 * 6, 90, 20, "TheAltening"))
            }

            add(GuiButton(11, 5, startPositionY + 24 * 7, 90, 20, "Cape"))
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        altsList.drawScreen(mouseX, mouseY, partialTicks)
        Fonts.font40.drawCenteredString(translationMenu("altManager"), width / 2f, 6f, 0xffffff)
        Fonts.font35.drawCenteredString(
            if (searchField.text.isEmpty()) "${accountsConfig.accounts.size} Alts" else altsList.accounts.size.toString() + " Search Results",
            width / 2f,
            18f,
            0xffffff
        )
        Fonts.font35.drawCenteredString(status, width / 2f, 32f, 0xffffff)
        Fonts.font35.drawStringWithShadow(
            "§7User: §a${mc.getSession().username}", 6f, 6f, 0xffffff
        )
        Fonts.font35.drawStringWithShadow(
            "§7Type: §a${
                if (altService.currentService == AltService.EnumAltService.THEALTENING) "TheAltening" else if (isValidTokenOffline(
                        mc.getSession().token
                    )
                ) "Premium" else "Cracked"
            }", 6f, 15f, 0xffffff
        )
        searchField.drawTextBox()
        if (searchField.text.isEmpty() && !searchField.isFocused) Fonts.font40.drawStringWithShadow(
            "§7Search...", searchField.xPosition + 4f, 17f, 0xffffff
        )
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    public override fun actionPerformed(button: GuiButton) {
        // Not enabled buttons should be ignored
        if (!button.enabled) return

        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> mc.displayGuiScreen(GuiLoginIntoAccount(this))
            2 -> { // Delete button
                status = if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.size) {
                    accountsConfig.removeAccount(altsList.accounts[altsList.selectedSlot])
                    saveConfig(accountsConfig)
                    "§aThe account has been removed."
                } else {
                    "§cSelect an account."
                }
            }

            3 -> { // Login button
                status = altsList.selectedAccount?.let {
                    loginButton.enabled = false
                    randomAltButton.enabled = false
                    randomNameButton.enabled = false

                    login(it, {
                        status = "§aLogged into ${mc.session.username}."
                    }, { exception ->
                        status = "§cLogin failed due to '${exception.message}'."
                    }, {
                        loginButton.enabled = true
                        randomAltButton.enabled = true
                        randomNameButton.enabled = true
                    })

                    "§aLogging in..."
                } ?: "§cSelect an account."
            }

            4 -> { // Random alt button
                status = altsList.accounts.randomOrNull()?.let {
                    loginButton.enabled = false
                    randomAltButton.enabled = false
                    randomNameButton.enabled = false

                    login(it, {
                        status = "§aLogged into ${mc.session.username}."
                    }, { exception ->
                        status = "§cLogin failed due to '${exception.message}'."
                    }, {
                        loginButton.enabled = true
                        randomAltButton.enabled = true
                        randomNameButton.enabled = true
                    })

                    "§aLogging in..."
                } ?: "§cYou do not have any accounts."
            }

            5 -> { // Random name button
                status = "§aLogged into ${randomAccount().name}."
                altService.switchService(AltService.EnumAltService.MOJANG)
            }

            6 -> { // Direct login button
                mc.displayGuiScreen(GuiLoginIntoAccount(this, directLogin = true))
            }

            7 -> { // Import button
                val file = MiscUtils.openFileChooser() ?: return

                file.readLines().forEach {
                    val accountData = it.split(":".toRegex(), limit = 2)
                    if (accountData.size > 1) {
                        // Most likely a mojang account
                        accountsConfig.addMojangAccount(accountData[0], accountData[1])
                    } else if (accountData[0].length < 16) {
                        // Might be cracked account
                        accountsConfig.addCrackedAccount(accountData[0])
                    } // skip account
                }

                saveConfig(accountsConfig)
                status = "§aThe accounts were imported successfully."
            }

            12 -> { // Export button
                if (accountsConfig.accounts.isEmpty()) {
                    status = "§cYou do not have any accounts to export."
                    return
                }

                val file = MiscUtils.saveFileChooser()
                if (file == null || file.isDirectory) {
                    return
                }

                try {
                    if (!file.exists()) {
                        file.createNewFile()
                    }

                    val accounts = accountsConfig.accounts.joinToString(separator = "\n") { account ->
                        when (account) {
                            is MojangAccount -> "${account.email}:${account.password}" // EMAIL:PASSWORD
                            is MicrosoftAccount -> "${account.name}:${account.session.token}" // NAME:SESSION
                            else -> account.name
                        }
                    }
                    file.writeText(accounts)

                    status = "§aExported successfully!"
                } catch (e: Exception) {
                    status = "§cUnable to export due to error: ${e.message}"
                }
            }

            8 -> {
                val currentAccount = altsList.selectedAccount

                if (currentAccount == null) {
                    status = "§cSelect an account."
                    return
                }

                // Format data for other tools
                val formattedData = when (currentAccount) {
                    is MojangAccount -> "${currentAccount.email}:${currentAccount.password}" // EMAIL:PASSWORD
                    is MicrosoftAccount -> "${currentAccount.name}:${currentAccount.session.token}" // NAME:SESSION
                    else -> currentAccount.name
                }

                // Copy to clipboard
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(formattedData), null)
                status = "§aCopied account into your clipboard."
            }

            9 -> { // Altening Button
                mc.displayGuiScreen(GuiTheAltening(this))
            }

            10 -> { // Session Login Button
                mc.displayGuiScreen(GuiSessionLogin(this))
            }

            11 -> { // Donator Cape Button
                mc.displayGuiScreen(GuiDonatorCape(this))
            }
        }
    }

    public override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (searchField.isFocused) {
            searchField.textboxKeyTyped(typedChar, keyCode)
        }

        when (keyCode) {
            // Go back
            Keyboard.KEY_ESCAPE -> mc.displayGuiScreen(prevGui)

            // Go one up in account list
            Keyboard.KEY_UP -> altsList.selectedSlot -= 1

            // Go one down in account list
            Keyboard.KEY_DOWN -> altsList.selectedSlot += 1

            // Go up or down in account list
            Keyboard.KEY_TAB -> altsList.selectedSlot += if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) -1 else 1

            // Login into account
            Keyboard.KEY_RETURN -> altsList.elementClicked(altsList.selectedSlot, true, 0, 0)

            // Scroll account list
            Keyboard.KEY_NEXT -> altsList.scrollBy(height - 100)

            // Scroll account list
            Keyboard.KEY_PRIOR -> altsList.scrollBy(-height + 100)

            // Add account
            Keyboard.KEY_ADD -> actionPerformed(addButton)

            // Remove account
            Keyboard.KEY_DELETE, Keyboard.KEY_MINUS -> actionPerformed(removeButton)

            // Copy when CTRL+C gets pressed
            Keyboard.KEY_C -> {
                if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) actionPerformed(copyButton)
                else super.keyTyped(typedChar, keyCode)
            }

            else -> super.keyTyped(typedChar, keyCode)
        }
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        altsList.handleMouseInput()
    }

    public override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        searchField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen() = searchField.updateCursorCounter()

    private inner class GuiList constructor(prevGui: GuiScreen) :
        GuiSlot(mc, prevGui.width, prevGui.height, 40, prevGui.height - 40, 30) {

        val accounts: List<MinecraftAccount>
            get() {
                var search = searchField.text
                if (search == null || search.isEmpty()) {
                    return accountsConfig.accounts
                }
                search = search.lowercase(Locale.getDefault())

                return accountsConfig.accounts.filter {
                    it.name.contains(
                        search, ignoreCase = true
                    ) || (it is MojangAccount && it.email.contains(search, ignoreCase = true))
                }
            }

        var selectedSlot = 0
            set(value) {
                if (accounts.isEmpty()) return
                field = (value + accounts.size) % accounts.size
            }
            get() {
                return if (field >= accounts.size) -1
                else field
            }

        val selectedAccount
            get() = accounts.getOrNull(selectedSlot)

        override fun isSelected(id: Int) = selectedSlot == id

        public override fun getSize() = accounts.size

        public override fun elementClicked(clickedElement: Int, doubleClick: Boolean, var3: Int, var4: Int) {
            selectedSlot = clickedElement

            if (doubleClick) {
                status = altsList.selectedAccount?.let {
                    loginButton.enabled = false
                    randomAltButton.enabled = false
                    randomNameButton.enabled = false

                    login(it, {
                        status = "§aLogged into ${mc.session.username}."
                    }, { exception ->
                        status = "§cLogin failed due to '${exception.message}'."
                    }, {
                        loginButton.enabled = true
                        randomAltButton.enabled = true
                        randomNameButton.enabled = true
                    })

                    "§aLogging in..."
                } ?: "§cSelect an account."
            }
        }

        override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, var5: Int, var6: Int) {
            val minecraftAccount = accounts[id]
            val accountName = if (minecraftAccount is MojangAccount && minecraftAccount.name.isEmpty()) {
                minecraftAccount.email
            } else {
                minecraftAccount.name
            }

            Fonts.font40.drawCenteredString(accountName, width / 2f, y + 2f, Color.WHITE.rgb, true)
            Fonts.font40.drawCenteredString(
                if (minecraftAccount is CrackedAccount) "Cracked" else if (minecraftAccount is MicrosoftAccount) "Microsoft" else if (minecraftAccount is MojangAccount) "Mojang" else "Something else",
                width / 2f,
                y + 15f,
                if (minecraftAccount is CrackedAccount) Color.GRAY.rgb else Color(118, 255, 95).rgb,
                true
            )
        }

        override fun drawBackground() {}
    }

    companion object {

        val altService = AltService()
        private val activeGenerators = mutableMapOf<String, Boolean>()

        fun loadActiveGenerators() {
            try {
                // Read versions json from cloud
                val jsonElement = JsonParser().parse(get("$CLIENT_CLOUD/generators.json"))

                // Check json is valid object
                if (jsonElement.isJsonObject) {
                    // Get json object of element
                    val jsonObject = jsonElement.asJsonObject
                    jsonObject.entrySet().forEach(Consumer { (key, value): Map.Entry<String, JsonElement> ->
                        activeGenerators[key] = value.asBoolean
                    })
                }
            } catch (throwable: Throwable) {
                // Print throwable to console
                LOGGER.error("Failed to load enabled generators.", throwable)
            }
        }

        fun login(
            minecraftAccount: MinecraftAccount, success: () -> Unit, error: (Exception) -> Unit, done: () -> Unit
        ) = thread(name = "LoginTask") {
            if (altService.currentService != AltService.EnumAltService.MOJANG) {
                try {
                    altService.switchService(AltService.EnumAltService.MOJANG)
                } catch (e: NoSuchFieldException) {
                    error(e)
                    LOGGER.error("Something went wrong while trying to switch alt service.", e)
                } catch (e: IllegalAccessException) {
                    error(e)
                    LOGGER.error("Something went wrong while trying to switch alt service.", e)
                }
            }

            try {
                minecraftAccount.update()
                mc.session = Session(
                    minecraftAccount.session.username,
                    minecraftAccount.session.uuid,
                    minecraftAccount.session.token,
                    "mojang"
                )
                callEvent(SessionEvent())

                success()
            } catch (exception: Exception) {
                error(exception)
            }
            done()
        }
    }
}