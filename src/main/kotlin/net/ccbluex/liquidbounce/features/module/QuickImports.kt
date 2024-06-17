package net.ccbluex.liquidbounce.features.module

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld

/**
 * Collection of the most used variables
 * to make the code more readable.
 *
 * However, we do not check for nulls here, because
 * we are sure that the client is in-game, if not
 * fiddling with the handler code.
 */
interface QuickImports {
    val mc: MinecraftClient
        get() = net.ccbluex.liquidbounce.utils.client.mc
    val player: ClientPlayerEntity
        get() = mc.player!!
    val world: ClientWorld
        get() = mc.world!!
    val network: ClientPlayNetworkHandler
        get() = mc.networkHandler!!
    val interaction: ClientPlayerInteractionManager
        get() = mc.interactionManager!!
}
