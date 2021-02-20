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

		buttonsList.add(classProvider.createGuiButton(1, buttonX, (representedScreen.height shr 2) + 35, "Enabled ${stateString(AntiModDisable.enabled)}").also { enabledButton = it })
		buttonsList.add(classProvider.createGuiButton(2, buttonX, buttonY + 25, "$BLOCK_FML${stateString(AntiModDisable.enabled && AntiModDisable.blockFMLPackets)}").also { fmlButton = it })
		buttonsList.add(classProvider.createGuiButton(3, buttonX, buttonY + 50, "$BLOCK_FML_PROXY_PACKET${stateString(AntiModDisable.enabled && AntiModDisable.blockFMLProxyPackets)}").also { fmlProxyPacket = it })
		buttonsList.add(classProvider.createGuiButton(4, buttonX, buttonY + 75, "$SPOOF_BRAND_PAYLOAD_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockClientBrandRetrieverPackets)}").also { clientBrandPayloadPacket = it })
		buttonsList.add(classProvider.createGuiButton(5, buttonX, buttonY + 100, "$BLOCK_WDL_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockWDLPayloads)}").also { wdlPayloadButton = it })
		buttonsList.add(classProvider.createGuiButton(6, buttonX, buttonY + 125, "$BLOCK_5ZIG_PACKETS ${stateString(AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads)}").also { `5zigPayloadButton` = it })
		buttonsList.add(classProvider.createGuiButton(7, buttonX, buttonY + 150, "$BLOCK_BETTERSPRINTING_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads)}").also { betterSprintingButton = it })
		buttonsList.add(classProvider.createGuiButton(8, buttonX, buttonY + 175, "$BLOCK_VAPE_SABOTAGES${stateString(AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages)}").also { vapeButton = it })
		buttonsList.add(classProvider.createGuiButton(9, buttonX, buttonY + 200, "$BLOCK_DIPERMISSIONS_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads)}").also { dipermsButton = it })
		buttonsList.add(classProvider.createGuiButton(10, buttonX, buttonY + 225, "$BLOCK_PERMISSIONS_REPL_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockPermissionsReplPayloads)}").also { permsreplButton = it })
		buttonsList.add(classProvider.createGuiButton(11, buttonX, buttonY + 250, "$BLOCK_SCHEMATICA_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads)}").also { schematicaButton = it })
		buttonsList.add(classProvider.createGuiButton(999, buttonX, buttonY + 275, "$PRINT_DEBUG_MESSAGES${stateString(AntiModDisable.enabled && AntiModDisable.debug)}").also { debugmode = it })

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
				enabledButton.displayString = "Enabled ${stateString(AntiModDisable.enabled)}"
				updateAll()
			}

			2 ->
			{
				AntiModDisable.blockFMLPackets = !AntiModDisable.blockFMLPackets
				fmlButton.displayString = "$BLOCK_FML${stateString(AntiModDisable.blockFMLPackets)}"
			}

			3 ->
			{
				AntiModDisable.blockFMLProxyPackets = !AntiModDisable.blockFMLProxyPackets
				fmlProxyPacket.displayString = "$BLOCK_FML_PROXY_PACKET${stateString(AntiModDisable.blockFMLProxyPackets)}"
			}

			4 ->
			{
				AntiModDisable.blockClientBrandRetrieverPackets = !AntiModDisable.blockClientBrandRetrieverPackets
				clientBrandPayloadPacket.displayString = "$SPOOF_BRAND_PAYLOAD_PACKETS${stateString(AntiModDisable.blockClientBrandRetrieverPackets)}"
			}

			5 ->
			{
				AntiModDisable.blockWDLPayloads = !AntiModDisable.blockWDLPayloads
				wdlPayloadButton.displayString = "$BLOCK_WDL_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockWDLPayloads)}"
			}

			6 ->
			{
				AntiModDisable.block5zigsmodPayloads = !AntiModDisable.block5zigsmodPayloads
				`5zigPayloadButton`.displayString = "$BLOCK_5ZIG_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads)}"
			}

			7 ->
			{
				AntiModDisable.blockBetterSprintingPayloads = !AntiModDisable.blockBetterSprintingPayloads
				betterSprintingButton.displayString = "$BLOCK_BETTERSPRINTING_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads)}"
			}

			8 ->
			{
				AntiModDisable.blockCrackedVapeSabotages = !AntiModDisable.blockCrackedVapeSabotages
				vapeButton.displayString = "$BLOCK_VAPE_SABOTAGES${stateString(AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages)}"
			}

			9 ->
			{
				AntiModDisable.blockDIPermissionsPayloads = !AntiModDisable.blockDIPermissionsPayloads
				dipermsButton.displayString = "$BLOCK_DIPERMISSIONS_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads)}"
			}

			10 ->
			{
				AntiModDisable.blockPermissionsReplPayloads = !AntiModDisable.blockPermissionsReplPayloads
				permsreplButton.displayString = "$BLOCK_PERMISSIONS_REPL_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockPermissionsReplPayloads)}"
			}

			11 ->
			{
				AntiModDisable.blockSchematicaPayloads = !AntiModDisable.blockSchematicaPayloads
				schematicaButton.displayString = "$BLOCK_SCHEMATICA_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads)}"
			}

			999 ->
			{
				AntiModDisable.debug = !AntiModDisable.debug
				debugmode.displayString = "$PRINT_DEBUG_MESSAGES${stateString(AntiModDisable.enabled && AntiModDisable.debug)}"
			}

			0 -> mc.displayGuiScreen(prevGui)
		}

		if (buttonID != 0) saveConfig(LiquidBounce.fileManager.valuesConfig) // Save the changed value
	}

	private fun updateAll()
	{
		fmlButton.displayString = "$BLOCK_FML${stateString(AntiModDisable.enabled && AntiModDisable.blockFMLPackets)}"
		fmlProxyPacket.displayString = "$BLOCK_FML_PROXY_PACKET${stateString(AntiModDisable.enabled && AntiModDisable.blockFMLProxyPackets)}"
		clientBrandPayloadPacket.displayString = "$SPOOF_BRAND_PAYLOAD_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockClientBrandRetrieverPackets)}"
		wdlPayloadButton.displayString = "$BLOCK_WDL_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockWDLPayloads)}"
		`5zigPayloadButton`.displayString = "$BLOCK_5ZIG_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads)}"
		betterSprintingButton.displayString = "$BLOCK_BETTERSPRINTING_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads)}"
		vapeButton.displayString = "$BLOCK_VAPE_SABOTAGES${stateString(AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages)}"
		dipermsButton.displayString = "$BLOCK_DIPERMISSIONS_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads)}"
		permsreplButton.displayString = "$BLOCK_PERMISSIONS_REPL_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockPermissionsReplPayloads)}"
		schematicaButton.displayString = "$BLOCK_SCHEMATICA_PACKETS${stateString(AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads)}"
		debugmode.displayString = "$PRINT_DEBUG_MESSAGES${stateString(AntiModDisable.enabled && AntiModDisable.debug)}"
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

		private fun stateString(state: Boolean): String = if (state) "\u00A7a(On)" else "\u00A7c(Off)"
	}
}
