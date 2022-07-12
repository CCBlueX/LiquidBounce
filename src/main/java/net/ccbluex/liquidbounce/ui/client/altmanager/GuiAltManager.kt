/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.thealtening.AltService
import com.thealtening.AltService.EnumAltService
import me.liuli.elixir.account.CrackedAccount
import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.account.MojangAccount
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.file.FileManager.Companion.saveConfig
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.*
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.MinecraftInstance.Companion.mc
import net.ccbluex.liquidbounce.utils.ServerUtils.connectToLastServer
import net.ccbluex.liquidbounce.utils.login.TheAlteningAccount
import net.ccbluex.liquidbounce.utils.login.UserUtils.isValidTokenOffline
import net.ccbluex.liquidbounce.utils.login.WrappedMinecraftAccount
import net.ccbluex.liquidbounce.utils.login.unwrapped
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.openFileChooser
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import kotlin.concurrent.thread

class GuiAltManager(private val prevGui: GuiScreen?) : GuiScreen()
{
    @JvmField
    var status = "\u00A77Idle..."

    lateinit var loginButton: GuiButton
    lateinit var randomButton: GuiButton
    lateinit var altsList: GuiAltsList

    private lateinit var searchField: GuiTextField

    override fun initGui()
    {
        val textFieldWidth = (width shr 3).coerceAtLeast(70)

        searchField = GuiTextField(2, Fonts.font40, width - textFieldWidth - 10, 10, textFieldWidth, 20)
        searchField.maxStringLength = Int.MAX_VALUE

        altsList = GuiAltsList(this)
        altsList.registerScrollButtons(7, 8)

        // Find the current logged-on account and automatically select it
        val index = LiquidBounce.fileManager.accountsConfig.accounts.indexOfFirst { it.name == mc.session.username } // TODO: Perform more accurate checks
        altsList.elementClicked(index, false, 0, 0)
        altsList.scrollBy(index * altsList.slotHeight)

        val buttonScreenRightPos = width - 80
        val buttonList = buttonList

        buttonList.add(GuiButton(0, buttonScreenRightPos, height - 65, 70, 20, "Back"))

        buttonList.add(GuiButton(1, buttonScreenRightPos, 46, 70, 20, "Add"))
        buttonList.add(GuiButton(2, buttonScreenRightPos, 70, 70, 20, "Remove"))
        buttonList.add(GuiButton(8, buttonScreenRightPos, 94, 70, 20, "Copy"))
        buttonList.add(GuiButton(7, buttonScreenRightPos, 118, 70, 20, "Import"))
        buttonList.add(GuiButton(12, buttonScreenRightPos, 142, 70, 20, "Export"))
        buttonList.add(GuiButton(13, buttonScreenRightPos, 166, 70, 20, "Banned Servers"))
        buttonList.add(GuiButton(14, buttonScreenRightPos, 190, 70, 20, "Copy Current Session Into Clipboard"))
        buttonList.add(GuiButton(15, buttonScreenRightPos, 214, 70, 20, "Current Session Info"))


        buttonList.add(GuiButton(3, 5, 46, 90, 20, "Login").also { loginButton = it })
        buttonList.add(GuiButton(4, 5, 70, 90, 20, "Random").also { randomButton = it })
        buttonList.add(GuiButton(6, 5, 94, 90, 20, "Direct Login"))
        buttonList.add(GuiButton(88, 5, 118, 90, 20, "Change Name"))
        buttonList.add(GuiButton(10, 5, 147, 90, 20, "Session Login"))
        buttonList.add(GuiButton(11, 5, 171, 90, 20, "Cape"))
        buttonList.add(GuiButton(99, 5, 195, 90, 20, "Reconnect to last server"))

        if (activeGenerators.getOrDefault("thealtening", true)) buttonList.add(GuiButton(9, 5, 255, 90, 20, "TheAltening"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        val width = width

        drawBackground(0)
        altsList.drawScreen(mouseX, mouseY, partialTicks)

        Fonts.font40.drawCenteredString("AltManager", (width shr 1).toFloat(), 6f, 0xffffff)
        Fonts.font35.drawCenteredString(if (searchField.text.isEmpty()) "${LiquidBounce.fileManager.accountsConfig.accounts.size} Alts" else "${altsList.accounts.size} Search Results", (width shr 1).toFloat(), 18f, 0xffffff)
        Fonts.font35.drawCenteredString(status, (width shr 1).toFloat(), 32f, 0xffffff)

        val altServiceTypeText = when
        {
            altService.currentService == EnumAltService.THEALTENING -> "\u00A7aTheAltening"
            isValidTokenOffline(mc.session.token) -> "\u00A76Mojang"
            else -> "\u00A78Cracked"
        }

        Fonts.font35.drawStringWithShadow("\u00A77User: \u00A7a${mc.session.username}", 6f, 6f, 0xffffff)
        Fonts.font35.drawStringWithShadow("\u00A77Type: \u00A7a$altServiceTypeText", 6f, 15f, 0xffffff)

        searchField.drawTextBox()
        if (searchField.text.isEmpty() && !searchField.isFocused) Fonts.font40.drawStringWithShadow("\u00A77Search...", searchField.xPosition + 4f, 17f, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    override fun actionPerformed(button: GuiButton)
    {
        if (!button.enabled) return

        when (button.id)
        {
            0 -> mc.displayGuiScreen(prevGui)

            1 -> mc.displayGuiScreen(GuiLoginIntoAccount(this))

            2 ->
            {
                status = if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.size)
                {
                    LiquidBounce.fileManager.accountsConfig.removeAccount(altsList.accounts[altsList.selectedSlot])
                    saveConfig(LiquidBounce.fileManager.accountsConfig)
                    "\u00A7aThe account has been removed."
                }
                else
                {
                    "\u00A7cSelect an account."
                }
            }

            3 ->
            {
                status = altsList.selectedAccount?.let {
                    loginButton.enabled = false
                    randomButton.enabled = false

                    login(it, {
                        status = "\u00A7aLogged into ${mc.session.username}."
                    }, { exception ->
                        status = "\u00A7cLogin failed due to '${exception.message}'."
                    }, {
                        loginButton.enabled = true
                        randomButton.enabled = true
                    })

                    "\u00A7aLogging in..."
                } ?: "\u00A7cSelect an account."
            }

            4 ->
            {
                status = altsList.accounts.randomOrNull()?.let {
                    loginButton.enabled = false
                    randomButton.enabled = false

                    login(it, {
                        status = "\u00A7aLogged into ${mc.session.username}."
                    }, { exception ->
                        status = "\u00A7cLogin failed due to '${exception.message}'."
                    }, {
                        loginButton.enabled = true
                        randomButton.enabled = true
                    })

                    "\u00A7aLogging in..."
                } ?: "\u00A7cYou do not have any accounts."
            }

            6 -> mc.displayGuiScreen(GuiLoginIntoAccount(this, true))

            7 ->
            {
                val file = openFileChooser() ?: return
                file.readLines().forEach {
                    val accountData = it.split(":", ignoreCase = true, limit = 2)
                    if (accountData.size > 1)
                    {
                        // Most likely mojang account
                        LiquidBounce.fileManager.accountsConfig.addMojangAccount(accountData[0], accountData[1])
                    }
                    else if (accountData[0].length < 16)
                    {
                        // Might be cracked account
                        LiquidBounce.fileManager.accountsConfig.addCrackedAccount(accountData[0])
                    }
                    // Or, skip account
                }
                saveConfig(LiquidBounce.fileManager.accountsConfig)
                status = "\u00A7aThe accounts were imported successfully."
            }

            8 ->
            {
                val currentAccount = altsList.selectedAccount

                if (currentAccount == null)
                {
                    status = "\u00A7cSelect an account."
                    return
                }

                // Format data for other tools
                val formattedData = when (currentAccount)
                {
                    is MojangAccount -> "${currentAccount.email}:${currentAccount.password}" // EMAIL:PASSWORD
                    is TheAlteningAccount -> "${currentAccount.token}:" // TOKEN:
                    is MicrosoftAccount -> "${currentAccount.name}:${currentAccount.session.token}" // NAME:SESSION
                    else -> currentAccount.name
                }

                // Copy to clipboard
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(formattedData), null);
                status = "\u00A7aCopied account into your clipboard."
            }

            88 -> mc.displayGuiScreen(GuiChangeName(this))

            9 -> mc.displayGuiScreen(GuiTheAltening(this))

            10 -> mc.displayGuiScreen(GuiSessionLogin(this))

            11 -> mc.displayGuiScreen(GuiDonatorCape(this))

            12 ->
            {
                if (LiquidBounce.fileManager.accountsConfig.accounts.isEmpty())
                {
                    status = "§cYou do not have any accounts to export."
                    return
                }

                val file = MiscUtils.saveFileChooser()
                if (file == null || file.isDirectory)
                {
                    return
                }

                try
                {
                    if (!file.exists()) file.createNewFile()
                    val accounts = LiquidBounce.fileManager.accountsConfig.accounts.joinToString(separator = "\n") { account ->
                        when (val unwrapped = account.unwrapped)
                        {
                            is MojangAccount -> "${unwrapped.email}:${unwrapped.password}" // EMAIL:PASSWORD
                            is TheAlteningAccount -> "${unwrapped.token}:" // TOKEN:
                            is MicrosoftAccount -> "${account.name}:${account.session.token}" // NAME:SESSION
                            else -> account.name
                        }
                    }
                    file.writeText(accounts)

                    status = "§aExported successfully!"
                }
                catch (e: Exception)
                {
                    status = "§cUnable to export due to error: ${e.message}"
                }
            }

            99 -> connectToLastServer()

            13 -> if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.size) mc.displayGuiScreen(GuiBannedServers(this, LiquidBounce.fileManager.accountsConfig.accounts[altsList.selectedSlot]))
            else status = "\u00A7cSelect an account."

            14 ->
            {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(mc.session.sessionID), null)
                status = "\u00A7aCopied current session id into your clipboard."
            }

            15 -> mc.displayGuiScreen(GuiSessionInfo(this, null))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        val list = altsList

        val search = searchField
        if (search.isFocused) search.textboxKeyTyped(typedChar, keyCode)

        val height = height
        val selected = list.selectedSlot

        when (keyCode)
        {
            Keyboard.KEY_ESCAPE ->
            {
                mc.displayGuiScreen(prevGui)
                return
            }

            Keyboard.KEY_UP -> list.elementClicked((selected - 1).coerceAtLeast(0), false, 0, 0)
            Keyboard.KEY_DOWN -> list.elementClicked((selected + 1).coerceAtMost(list.size - 1), false, 0, 0)
            Keyboard.KEY_RETURN -> list.elementClicked(selected, true, 0, 0)
            Keyboard.KEY_NEXT -> list.scrollBy(height - 100)

            Keyboard.KEY_PRIOR ->
            {
                list.scrollBy(-height + 100)
                return
            }
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun handleMouseInput()
    {
        super.handleMouseInput()
        altsList.handleMouseInput()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    {
        searchField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen()
    {
        searchField.updateCursorCounter()
    }

    inner class GuiAltsList internal constructor(prevGui: GuiScreen) : GuiSlot(mc, prevGui.width, prevGui.height, 40, prevGui.height - 40, 30)
    {
        val accounts: List<WrappedMinecraftAccount>
            get()
            {
                var search = searchField.text
                if (search == null || search.isEmpty()) return LiquidBounce.fileManager.accountsConfig.accounts

                // Apply search filter
                search = search.lowercase(Locale.getDefault())

                return LiquidBounce.fileManager.accountsConfig.accounts.filter { it.name.contains(search, ignoreCase = true) || (it.represented is MojangAccount && it.represented.email.contains(search, ignoreCase = true)) }
            }

        var selectedSlot = 0
            get()
            {
                if (field > accounts.size) field = -1
                return field
            }
            internal set

        val selectedAccount: MinecraftAccount?
            get() = if (selectedSlot >= 0 && selectedSlot < accounts.size) accounts[selectedSlot] else null

        override fun isSelected(id: Int): Boolean = selectedSlot == id

        public override fun getSize(): Int = accounts.size

        public override fun elementClicked(slotIndex: Int, doubleClick: Boolean, var3: Int, var4: Int)
        {
            selectedSlot = slotIndex

            if (doubleClick)
            {
                status = altsList.selectedAccount?.let {
                    loginButton.enabled = false
                    randomButton.enabled = false

                    login(it, {
                        status = "\u00A7aLogged into ${mc.session.username}."
                    }, { exception ->
                        status = "\u00A7cLogin failed due to '${exception.message}'."
                    }, {
                        loginButton.enabled = true
                        randomButton.enabled = true
                    })

                    "\u00A7aLogging in..."
                } ?: "\u00A7cSelect an account."
            }
        }

        override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, mouseXIn: Int, mouseYIn: Int)
        {
            val width = width
            val middleScreen = (width shr 1).toFloat()

            val account = accounts[id]

            // Draw account name
            Fonts.font40.drawCenteredString(if (account.represented is MojangAccount && account.name.isEmpty()) account.represented.email else account.name, middleScreen, y + 2f, -1, true)

            val serviceTypeColor = when
            {
                account.represented is CrackedAccount -> -8355712 // Cracked
                account.represented is MojangAccount && account.name.isEmpty() -> -4144960 // Unchecked mojang
                !account.isAvailable -> -65536 // Unavailable
                else -> -16711936 // Premium
            }

            Fonts.font40.drawCenteredString(account.type, middleScreen, y + 10f, serviceTypeColor, true)

            if (account.bannedServers.isNotEmpty()) Fonts.font35.drawCenteredString("Banned on " + account.serializeBannedServers(), middleScreen, y + 20f, -65536, true)
        }

        override fun drawBackground()
        {
        }
    }

    companion object
    {
        @JvmField
        val altService = AltService()

        private val activeGenerators = mutableMapOf<String, Boolean>()

        fun loadActiveGenerators()
        {
            try
            {
                // Read versions json from cloud
                val jsonElement = JsonParser().parse(HttpUtils.get(LiquidBounce.CLIENT_CLOUD + "/generators.json"))

                // Check json is valid object
                if (jsonElement.isJsonObject)
                {
                    // Get json object of element
                    val jsonObject = jsonElement.asJsonObject
                    jsonObject.entrySet().forEach(Consumer { (key, value): Map.Entry<String, JsonElement> ->
                        activeGenerators[key] = value.asBoolean
                    })
                }
            }
            catch (throwable: Throwable)
            {
                // Print throwable to console
                logger.error("Failed to load enabled generators.", throwable)
            }
        }

        fun login(minecraftAccount: MinecraftAccount, success: () -> Unit, error: (Exception) -> Unit, done: () -> Unit) = thread(name = "LoginTask") {
            val targetAltService = if (minecraftAccount.unwrapped is TheAlteningAccount) EnumAltService.THEALTENING else EnumAltService.MOJANG
            if (altService.currentService != targetAltService)
            {
                try
                {
                    altService.switchService(targetAltService)
                }
                catch (e: NoSuchFieldException)
                {
                    error(e)
                    logger.error("Something went wrong while trying to switch alt service.", e)
                }
                catch (e: IllegalAccessException)
                {
                    error(e)
                    logger.error("Something went wrong while trying to switch alt service.", e)
                }
            }

            try
            {
                minecraftAccount.update()
                Minecraft.getMinecraft().session = Session(minecraftAccount.session.username, minecraftAccount.session.uuid, minecraftAccount.session.token, "mojang")
                LiquidBounce.eventManager.callEvent(SessionEvent())
                success()
            }
            catch (exception: Exception)
            {
                error(exception)
            }
            done()
        }

        @JvmStatic
        fun canEnableMarkBannedButton(): Boolean
        {
            val username = mc.session.username
            return LiquidBounce.fileManager.accountsConfig.accounts.any { it.name.equals(username, ignoreCase = true) }
        }

        @JvmStatic
        fun canMarkBannedCurrent(serverIp: String?): Boolean
        {
            val username = mc.session.username
            return serverIp == null || LiquidBounce.fileManager.accountsConfig.accounts.firstOrNull { it.name.equals(username, ignoreCase = true) }?.let { !it.bannedServers.contains(serverIp) } ?: true
        }

        @JvmStatic
        fun toggleMarkBanned(serverIp: String)
        {
            val username = mc.session.username
            LiquidBounce.fileManager.accountsConfig.accounts.filter { it.name.equals(username, ignoreCase = true) }.forEach { account ->
                val canMarkAsBanned = canMarkBannedCurrent(serverIp)

                if (canMarkAsBanned) account.bannedServers.add(serverIp) else account.bannedServers.remove(serverIp)

                saveConfig(LiquidBounce.fileManager.accountsConfig)

                logger.info("Marked account {} {} on {}", account.name, if (canMarkAsBanned) "banned" else "un-banned", serverIp)
            }
        }
    }
}
