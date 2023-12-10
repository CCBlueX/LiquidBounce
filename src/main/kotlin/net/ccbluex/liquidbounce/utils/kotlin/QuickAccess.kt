package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.network.ClientPlayerInteractionManager
import net.minecraft.client.world.ClientWorld

object QuickAccess {
    val player: ClientPlayerEntity
        inline get() = mc.player!!
    val world: ClientWorld
        inline get() = mc.world!!
    val network: ClientPlayNetworkHandler
        inline get() = mc.networkHandler!!
    val interaction: ClientPlayerInteractionManager
        inline get() = mc.interactionManager!!
}
