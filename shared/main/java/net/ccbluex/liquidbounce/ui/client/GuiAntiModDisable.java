/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client;

import java.io.IOException;
import java.util.Collection;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen;
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen;
import net.ccbluex.liquidbounce.features.special.AntiModDisable;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.ui.font.Fonts;

import org.lwjgl.input.Keyboard;

public class GuiAntiModDisable extends WrappedGuiScreen
{
	public static final String BLOCK_FML = "Block FML ";
	public static final String BLOCK_FMLPROXY_PACKET = "Block FMLProxyPackets ";
	public static final String SPOOF_MC_BRAND_PAYLOAD_PACKETS = "Spoof FML ClientBrandRetriever Payloads ";
	public static final String BLOCK_WORLD_DOWNLOADER_PAYLOAD_PACKETS = "Block WorldDownloader Payloads ";
	public static final String BLOCK_THE_5_ZIG_S_MOD_PAYLOAD_PACKETS = "Block The 5zig's mod Payloads ";
	public static final String BLOCK_BETTER_SPRINTING_PAYLOAD_PACKETS = "Block Better Sprinting mod Payloads ";
	public static final String BLOCK_CRACKED_VAPE_SABOTAGES = "Block Cracked Vape Sabotages ";
	public static final String BLOCK_DIPERMISSIONS_PAYLOAD_PACKETS = "Block DIPermissions Payloads ";
	public static final String BLOCK_PERMISSIONS_REPL_PAYLOAD_PACKETS = "Block PermissionsRepl Payloads ";
	public static final String BLOCK_SCHEMATICA_PAYLOAD_PACKETS = "Block Schematica Payloads ";
	public static final String PRINT_BLOCKED_SPOOFED_PACKETS = "Print Blocked/Spoofed Packets On The Chat ";

	private final IGuiScreen prevGui;

	private IGuiButton enabledButton;
	private IGuiButton fmlButton;
	private IGuiButton fmlProxyPacket;
	private IGuiButton clientBrandPayloadPacket;
	private IGuiButton wdlPayloadButton;
	private IGuiButton _5zigPayloadButton;
	private IGuiButton debugmode;
	private IGuiButton betterSprintingButton;
	private IGuiButton vapeButton;
	private IGuiButton dipermsButton;
	private IGuiButton permsreplButton;
	private IGuiButton schematicaButton;

	public GuiAntiModDisable(final IGuiScreen prevGui)
	{
		this.prevGui = prevGui;
	}

	@Override
	public void initGui()
	{
		final Collection<IGuiButton> buttonsList = representedScreen.getButtonList();
		final int width = representedScreen.getWidth();
		final int height = representedScreen.getHeight();

		buttonsList.add(enabledButton = classProvider.createGuiButton(1, width / 2 - 100, representedScreen.getHeight() / 4 + 35, "Enabled " + (AntiModDisable.enabled ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(fmlButton = classProvider.createGuiButton(2, width / 2 - 100, height / 4 + 50 + 25, BLOCK_FML + (AntiModDisable.enabled && AntiModDisable.blockFMLPackets ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(fmlProxyPacket = classProvider.createGuiButton(3, width / 2 - 100, height / 4 + 50 + 50, BLOCK_FMLPROXY_PACKET + (AntiModDisable.enabled && AntiModDisable.blockFMLProxyPackets ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(clientBrandPayloadPacket = classProvider.createGuiButton(4, width / 2 - 100, height / 4 + 50 + 75, SPOOF_MC_BRAND_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockClientBrandRetrieverPackets ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(wdlPayloadButton = classProvider.createGuiButton(5, width / 2 - 100, height / 4 + 50 + 100, BLOCK_WORLD_DOWNLOADER_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockWDLPayloads ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(_5zigPayloadButton = classProvider.createGuiButton(6, width / 2 - 100, height / 4 + 50 + 125, "Block The 5zig's mod Payload Packets " + (AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(betterSprintingButton = classProvider.createGuiButton(7, width / 2 - 100, height / 4 + 50 + 150, BLOCK_BETTER_SPRINTING_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(vapeButton = classProvider.createGuiButton(8, width / 2 - 100, height / 4 + 50 + 175, BLOCK_CRACKED_VAPE_SABOTAGES + (AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(dipermsButton = classProvider.createGuiButton(9, width / 2 - 100, height / 4 + 50 + 200, BLOCK_DIPERMISSIONS_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(permsreplButton = classProvider.createGuiButton(10, width / 2 - 100, height / 4 + 50 + 225, BLOCK_PERMISSIONS_REPL_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockPermissionsReplPayloads ? "\u00A7a(On)" : "\u00A7c(Off)")));
		buttonsList.add(schematicaButton = classProvider.createGuiButton(11, width / 2 - 100, height / 4 + 50 + 250, BLOCK_SCHEMATICA_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads ? "\u00A7a(On)" : "\u00A7c(Off)")));

		buttonsList.add(debugmode = classProvider.createGuiButton(999, width / 2 - 100, height / 4 + 50 + 275, PRINT_BLOCKED_SPOOFED_PACKETS + (AntiModDisable.enabled && AntiModDisable.debug ? "\u00A7a(On)" : "\u00A7c(Off)")));

		buttonsList.add(classProvider.createGuiButton(0, width / 2 - 100, height / 4 + 55 + 300 + 5, "Back"));
	}

	@Override
	public void actionPerformed(final IGuiButton button)
	{
		switch (button.getId())
		{
			case 1:
				AntiModDisable.enabled = !AntiModDisable.enabled;
				enabledButton.setDisplayString("Enabled (" + (AntiModDisable.enabled ? "On" : "Off") + ")");
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				updateAll();
				break;
			case 2:
				AntiModDisable.blockFMLPackets = !AntiModDisable.blockFMLPackets;
				fmlButton.setDisplayString("Block FML (" + (AntiModDisable.blockFMLPackets ? "On" : "Off") + ")");
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 3:
				AntiModDisable.blockFMLProxyPackets = !AntiModDisable.blockFMLProxyPackets;
				fmlProxyPacket.setDisplayString("Block FMLProxyPackets (" + (AntiModDisable.blockFMLProxyPackets ? "On" : "Off") + ")");
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 4:
				AntiModDisable.blockClientBrandRetrieverPackets = !AntiModDisable.blockClientBrandRetrieverPackets;
				clientBrandPayloadPacket.setDisplayString("Block FML ClientBrandRetriever Packets (" + (AntiModDisable.blockClientBrandRetrieverPackets ? "On" : "Off") + ")");
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 5:
				AntiModDisable.blockWDLPayloads = !AntiModDisable.blockWDLPayloads;
				wdlPayloadButton.setDisplayString(BLOCK_WORLD_DOWNLOADER_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockWDLPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 6:
				AntiModDisable.block5zigsmodPayloads = !AntiModDisable.block5zigsmodPayloads;
				_5zigPayloadButton.setDisplayString(BLOCK_THE_5_ZIG_S_MOD_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 7:
				AntiModDisable.blockBetterSprintingPayloads = !AntiModDisable.blockBetterSprintingPayloads;
				betterSprintingButton.setDisplayString(BLOCK_BETTER_SPRINTING_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 8:
				AntiModDisable.blockCrackedVapeSabotages = !AntiModDisable.blockCrackedVapeSabotages;
				vapeButton.setDisplayString(BLOCK_CRACKED_VAPE_SABOTAGES + (AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages ? "\u00A7a(On)" : "\u00A7c(Off)"));
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 9:
				AntiModDisable.blockDIPermissionsPayloads = !AntiModDisable.blockDIPermissionsPayloads;
				dipermsButton.setDisplayString(BLOCK_DIPERMISSIONS_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 10:
				AntiModDisable.blockPermissionsReplPayloads = !AntiModDisable.blockPermissionsReplPayloads;
				permsreplButton.setDisplayString(BLOCK_PERMISSIONS_REPL_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockPermissionsReplPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 11:
				AntiModDisable.blockSchematicaPayloads = !AntiModDisable.blockSchematicaPayloads;
				schematicaButton.setDisplayString(BLOCK_SCHEMATICA_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 999:
				AntiModDisable.debug = !AntiModDisable.debug;
				debugmode.setDisplayString(PRINT_BLOCKED_SPOOFED_PACKETS + (AntiModDisable.enabled && AntiModDisable.debug ? "\u00A7a(On)" : "\u00A7c(Off)"));
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
			case 0:
				mc.displayGuiScreen(prevGui);
				break;
		}
	}

	private void updateAll()
	{
		fmlButton.setDisplayString(BLOCK_FML + (AntiModDisable.enabled && AntiModDisable.blockFMLPackets ? "\u00A7a(On)" : "\u00A7c(Off)"));
		fmlProxyPacket.setDisplayString(BLOCK_FMLPROXY_PACKET + (AntiModDisable.enabled && AntiModDisable.blockFMLProxyPackets ? "\u00A7a(On)" : "\u00A7c(Off)"));
		clientBrandPayloadPacket.setDisplayString(SPOOF_MC_BRAND_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockClientBrandRetrieverPackets ? "\u00A7a(On)" : "\u00A7c(Off)"));
		wdlPayloadButton.setDisplayString(BLOCK_WORLD_DOWNLOADER_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockWDLPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
		_5zigPayloadButton.setDisplayString(BLOCK_THE_5_ZIG_S_MOD_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.block5zigsmodPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
		betterSprintingButton.setDisplayString(BLOCK_BETTER_SPRINTING_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockBetterSprintingPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
		vapeButton.setDisplayString(BLOCK_CRACKED_VAPE_SABOTAGES + (AntiModDisable.enabled && AntiModDisable.blockCrackedVapeSabotages ? "\u00A7a(On)" : "\u00A7c(Off)"));
		dipermsButton.setDisplayString(BLOCK_DIPERMISSIONS_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockDIPermissionsPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
		permsreplButton.setDisplayString(BLOCK_PERMISSIONS_REPL_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockPermissionsReplPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
		schematicaButton.setDisplayString(BLOCK_SCHEMATICA_PAYLOAD_PACKETS + (AntiModDisable.enabled && AntiModDisable.blockSchematicaPayloads ? "\u00A7a(On)" : "\u00A7c(Off)"));
		debugmode.setDisplayString(PRINT_BLOCKED_SPOOFED_PACKETS + (AntiModDisable.enabled && AntiModDisable.debug ? "\u00A7a(On)" : "\u00A7c(Off)"));
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		representedScreen.drawBackground(0);
		Fonts.fontBold180.drawCenteredString("AntiModDisable", (int) (representedScreen.getWidth() / 2.0F), (int) (representedScreen.getHeight() / 10.0F + 5.0F), 4673984, true);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void keyTyped(final char typedChar, final int keyCode) throws IOException
	{
		if (keyCode == Keyboard.KEY_ESCAPE)
		{
			mc.displayGuiScreen(prevGui);
			return;
		}

		super.keyTyped(typedChar, keyCode);
	}
}
