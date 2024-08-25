/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.ui.client.GuiMainMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerAddress
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.network.NetHandlerLoginClient
import net.minecraft.network.EnumConnectionState
import net.minecraft.network.NetworkManager
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.login.client.C00PacketLoginStart
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.spongepowered.asm.mixin.Unique
import java.net.InetAddress


@SideOnly(Side.CLIENT)
object ServerUtils : MinecraftInstance() {
    var serverData: ServerData? = null

    @JvmOverloads
    fun connectToLastServer(noGLContext: Boolean = false) {
        if (serverData == null) return

        if (noGLContext) {
            Thread {
                // Code ported from GuiConnecting.connect
                // Used in AutoAccount's ReconnectDelay.
                // You cannot do this in the normal way because of required OpenGL context in current thread.
                // When you delay a call, it gets run in a new TimerThread.

                val serverAddress = ServerAddress.fromString(serverData!!.serverIP)
                mc.world = null
                mc.setServerData(serverData)

                val inetAddress = InetAddress.getByName(serverAddress.ip)
                val networkManager = NetworkManager.createNetworkManagerAndConnect(
                    inetAddress,
                    serverAddress.port,
                    mc.gameSettings.isUsingNativeTransport
                )
                networkManager.netHandler = NetHandlerLoginClient(networkManager, mc, GuiMainMenu())

                networkManager.sendPacket(
                    HandshakeC2SPacket(47, serverAddress.ip, serverAddress.port, EnumConnectionState.LOGIN, true)
                )

                networkManager.sendPacket(
                    C00PacketLoginStart(mc.session.profile)
                )
            }.start()
        } else mc.displayGuiScreen(GuiConnecting(GuiMultiplayer(GuiMainMenu()), mc, serverData))
    }

    /**
     * Hides sensitive information from LiquidProxy addresses.
     */
    fun hideSensitiveInformation(address: String): String {
        return if (address.contains(".liquidbounce.net")) {
            "<redacted>.liquidbounce.net"
        } else if (address.contains(".liquidproxy.net")) {
            "<redacted>.liquidproxy.net"
        } else {
            address
        }
    }

    val remoteIp: String
        get() {
            var serverIp = "Singleplayer"

            // This can throw NPE during LB startup, if an element has server ip in it
            if (mc.world?.isRemote == true) {
                val serverData = mc.currentServerData
                if (serverData != null) serverIp = serverData.serverIP
            }

            return serverIp
        }
}