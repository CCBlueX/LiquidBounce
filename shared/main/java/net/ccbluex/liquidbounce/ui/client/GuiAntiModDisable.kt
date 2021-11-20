/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.features.special.AntiModDisable
import net.ccbluex.liquidbounce.file.FileManager.Companion.saveConfig
import net.ccbluex.liquidbounce.ui.font.Fonts
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiAntiModDisable(private val prevGui: IGuiScreen) : WrappedGuiScreen()
{
	private lateinit var enabledButton: IGuiButton
	private lateinit var fmlButton: IGuiButton
	private lateinit var fmlProxyPacket: IGuiButton
	private lateinit var clientBrandPayloadPacket: IGuiButton
	private lateinit var wdlPayloadButton: IGuiButton
	private lateinit var `5zigPayloadButton`: IGuiButton
	private lateinit var debugmode: IGuiButton
	private lateinit var betterSprintingButton: IGuiButton
	private lateinit var vapeButton: IGuiButton
	private lateinit var dipermsButton: IGuiButton
	private lateinit var permsreplButton: IGuiButton
	private lateinit var schematicaButton: IGuiButton
	override fun initGui()
	{
		val buttonsList: MutableCollection<IGuiButton> = representedScreen.buttonList
		val width = representedScreen.width
		val height = representedScreen.height

		val buttonX = (width shr 1) - 100
		val buttonY = (height shr 2) + 50

		buttonsList.add(classProvider.createGuiButton(1, buttonX, (representedScreen.height shr 2) + 35, "Enabled ${getStateString(AntiModDisable.enabled)}").also { enabledButton = it })

		val addButton = { id: Int, index: Int, text: String, callback: (IGuiButton) -> Unit ->
			buttonsList.add(classProvider.createGuiButton(id, buttonX, buttonY + 25 * index + 25, text).also(callback))
		}

		addButton(2, 0, "$BLOCK_FML${getStateString(AntiModDisable.enabled && AntiModDisable.blockFMLPackets)}") { fmlButton = it }
		addButton(3, 1, "$BLOCK_FML_PROXY_PACKET${getStateString(AntiModDisable.enabled && AntiModDisable.blockFMLProxyPackets)}") { fmlProxyPacket = it }
		addButton(4, 2, "$SPOOF_BRAND_PAYLOAD_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockClientBrandRetrieverPackets)}") { clientBrandPayloadPacket = it }
		addButton(5, 3, "$BLOCK_WDL_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockWDLPayloads)}") { wdlPayloadButton = it }
		addButton(6, 4, "$BLOCK_5ZIG_PACKETS ${getStateString(AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads)}") { `5zigPayloadButton` = it }
		addButton(7, 5, "$BLOCK_BETTERSPRINTING_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads)}") { betterSprintingButton = it }
		addButton(8, 6, "$BLOCK_VAPE_SABOTAGES${getStateString(AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages)}") { vapeButton = it }
		addButton(9, 7, "$BLOCK_DIPERMISSIONS_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads)}") { dipermsButton = it }
		addButton(10, 8, "$BLOCK_PERMISSIONS_REPL_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockReplicatedPermissionsPayloads)}") { permsreplButton = it }
		addButton(11, 9, "$BLOCK_SCHEMATICA_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads)}") { schematicaButton = it }
		addButton(999, 10, "$PRINT_DEBUG_MESSAGES${getStateString(AntiModDisable.enabled && AntiModDisable.debug)}") { debugmode = it }

		buttonsList.add(classProvider.createGuiButton(0, buttonX, (height shr 2) + 55 + 300 + 5, "Back"))
	}

	override fun actionPerformed(button: IGuiButton)
	{
		val buttonID = button.id

		when (buttonID)
		{
			1 ->
			{
				AntiModDisable.enabled = !AntiModDisable.enabled
				enabledButton.displayString = "Enabled ${getStateString(AntiModDisable.enabled)}"
				updateAll()
			}

			2 ->
			{
				AntiModDisable.blockFMLPackets = !AntiModDisable.blockFMLPackets
				fmlButton.displayString = "$BLOCK_FML${getStateString(AntiModDisable.blockFMLPackets)}"
			}

			3 ->
			{
				AntiModDisable.blockFMLProxyPackets = !AntiModDisable.blockFMLProxyPackets
				fmlProxyPacket.displayString = "$BLOCK_FML_PROXY_PACKET${getStateString(AntiModDisable.blockFMLProxyPackets)}"
			}

			4 ->
			{
				AntiModDisable.blockClientBrandRetrieverPackets = !AntiModDisable.blockClientBrandRetrieverPackets
				clientBrandPayloadPacket.displayString = "$SPOOF_BRAND_PAYLOAD_PACKETS${getStateString(AntiModDisable.blockClientBrandRetrieverPackets)}"
			}

			5 ->
			{
				AntiModDisable.blockWDLPayloads = !AntiModDisable.blockWDLPayloads
				wdlPayloadButton.displayString = "$BLOCK_WDL_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockWDLPayloads)}"
			}

			6 ->
			{
				AntiModDisable.block5zigsmodPayloads = !AntiModDisable.block5zigsmodPayloads
				`5zigPayloadButton`.displayString = "$BLOCK_5ZIG_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads)}"
			}

			7 ->
			{
				AntiModDisable.blockBetterSprintingPayloads = !AntiModDisable.blockBetterSprintingPayloads
				betterSprintingButton.displayString = "$BLOCK_BETTERSPRINTING_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads)}"
			}

			8 ->
			{
				AntiModDisable.blockCrackedVapeSabotages = !AntiModDisable.blockCrackedVapeSabotages
				vapeButton.displayString = "$BLOCK_VAPE_SABOTAGES${getStateString(AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages)}"
			}

			9 ->
			{
				AntiModDisable.blockDIPermissionsPayloads = !AntiModDisable.blockDIPermissionsPayloads
				dipermsButton.displayString = "$BLOCK_DIPERMISSIONS_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads)}"
			}

			10 ->
			{
				AntiModDisable.blockReplicatedPermissionsPayloads = !AntiModDisable.blockReplicatedPermissionsPayloads
				permsreplButton.displayString = "$BLOCK_PERMISSIONS_REPL_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockReplicatedPermissionsPayloads)}"
			}

			11 ->
			{
				AntiModDisable.blockSchematicaPayloads = !AntiModDisable.blockSchematicaPayloads
				schematicaButton.displayString = "$BLOCK_SCHEMATICA_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads)}"
			}

			999 ->
			{
				AntiModDisable.debug = !AntiModDisable.debug
				debugmode.displayString = "$PRINT_DEBUG_MESSAGES${getStateString(AntiModDisable.enabled && AntiModDisable.debug)}"
			}

			0 -> mc.displayGuiScreen(prevGui)
		}

		if (buttonID != 0) saveConfig(LiquidBounce.fileManager.valuesConfig) // Save the changed value
	}

	private fun updateAll()
	{
		fmlButton.displayString = "$BLOCK_FML${getStateString(AntiModDisable.enabled && AntiModDisable.blockFMLPackets)}"
		fmlProxyPacket.displayString = "$BLOCK_FML_PROXY_PACKET${getStateString(AntiModDisable.enabled && AntiModDisable.blockFMLProxyPackets)}"
		clientBrandPayloadPacket.displayString = "$SPOOF_BRAND_PAYLOAD_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockClientBrandRetrieverPackets)}"
		wdlPayloadButton.displayString = "$BLOCK_WDL_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockWDLPayloads)}"
		`5zigPayloadButton`.displayString = "$BLOCK_5ZIG_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads)}"
		betterSprintingButton.displayString = "$BLOCK_BETTERSPRINTING_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads)}"
		vapeButton.displayString = "$BLOCK_VAPE_SABOTAGES${getStateString(AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages)}"
		dipermsButton.displayString = "$BLOCK_DIPERMISSIONS_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads)}"
		permsreplButton.displayString = "$BLOCK_PERMISSIONS_REPL_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockReplicatedPermissionsPayloads)}"
		schematicaButton.displayString = "$BLOCK_SCHEMATICA_PACKETS${getStateString(AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads)}"
		debugmode.displayString = "$PRINT_DEBUG_MESSAGES${getStateString(AntiModDisable.enabled && AntiModDisable.debug)}"
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)
		Fonts.fontBold180.drawCenteredString("AntiModDisable", (representedScreen.width shr 1).toFloat(), representedScreen.height * 0.1f + 5.0f, 4673984, true)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	@Throws(IOException::class)
	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		if (keyCode == Keyboard.KEY_ESCAPE)
		{
			mc.displayGuiScreen(prevGui)
			return
		}
		super.keyTyped(typedChar, keyCode)
	}

	companion object
	{
		// Button string constants
		const val BLOCK_FML = "Block FML "
		const val BLOCK_FML_PROXY_PACKET = "Block FMLProxyPackets "
		const val SPOOF_BRAND_PAYLOAD_PACKETS = "Spoof FML ClientBrandRetriever Payloads "
		const val BLOCK_WDL_PACKETS = "Block WorldDownloader Payloads "
		const val BLOCK_5ZIG_PACKETS = "Block The 5zig's mod Payloads "
		const val BLOCK_BETTERSPRINTING_PACKETS = "Block Better Sprinting mod Payloads "
		const val BLOCK_VAPE_SABOTAGES = "Block Cracked Vape Sabotages "
		const val BLOCK_DIPERMISSIONS_PACKETS = "Block DIPermissions Payloads "
		const val BLOCK_PERMISSIONS_REPL_PACKETS = "Block PermissionsRepl Payloads "
		const val BLOCK_SCHEMATICA_PACKETS = "Block Schematica Payloads "
		const val PRINT_DEBUG_MESSAGES = "Print Blocked/Spoofed Packets On The Chat "

		@JvmStatic
		private fun getStateString(state: Boolean): String = if (state) "\u00A7a(On)" else "\u00A7c(Off)"
	}
}
