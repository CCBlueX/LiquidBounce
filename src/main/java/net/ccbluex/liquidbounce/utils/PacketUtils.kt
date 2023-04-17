/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayClient
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.server.*

object PacketUtils : MinecraftInstance() {
    private val packets = ArrayList<Packet<INetHandlerPlayServer>>()

    fun handleSendPacket(packet: Packet<*>): Boolean {
        if (packets.contains(packet)) {
            packets.remove(packet)
            return true
        }
        return false
    }

    val S12PacketEntityVelocity.realMotionX: Float
        get() = motionX / 8000f

    val S12PacketEntityVelocity.realMotionY: Float
        get() = motionY / 8000f

    val S12PacketEntityVelocity.realMotionZ: Float
        get() = motionZ / 8000f

    @JvmStatic
    fun sendPacketNoEvent(packet: Packet<INetHandlerPlayServer>) {
        packets.add(packet)
        mc.netHandler.addToSendQueue(packet)
    }

    fun handlePacket(packet: Packet<INetHandlerPlayClient?>) {
        val netHandler = mc.netHandler

        if (packet is S00PacketKeepAlive) {
            netHandler.handleKeepAlive(packet)
        } else if (packet is S01PacketJoinGame) {
            netHandler.handleJoinGame(packet)
        } else if (packet is S02PacketChat) {
            netHandler.handleChat(packet)
        } else if (packet is S03PacketTimeUpdate) {
            netHandler.handleTimeUpdate(packet)
        } else if (packet is S04PacketEntityEquipment) {
            netHandler.handleEntityEquipment(packet)
        } else if (packet is S05PacketSpawnPosition) {
            netHandler.handleSpawnPosition(packet)
        } else if (packet is S06PacketUpdateHealth) {
            netHandler.handleUpdateHealth(packet)
        } else if (packet is S07PacketRespawn) {
            netHandler.handleRespawn(packet)
        } else if (packet is S08PacketPlayerPosLook) {
            netHandler.handlePlayerPosLook(packet)
        } else if (packet is S09PacketHeldItemChange) {
            netHandler.handleHeldItemChange(packet)
        } else if (packet is S10PacketSpawnPainting) {
            netHandler.handleSpawnPainting(packet)
        } else if (packet is S0APacketUseBed) {
            netHandler.handleUseBed(packet)
        } else if (packet is S0BPacketAnimation) {
            netHandler.handleAnimation(packet)
        } else if (packet is S0CPacketSpawnPlayer) {
            netHandler.handleSpawnPlayer(packet)
        } else if (packet is S0DPacketCollectItem) {
            netHandler.handleCollectItem(packet)
        } else if (packet is S0EPacketSpawnObject) {
            netHandler.handleSpawnObject(packet)
        } else if (packet is S0FPacketSpawnMob) {
            netHandler.handleSpawnMob(packet)
        } else if (packet is S11PacketSpawnExperienceOrb) {
            netHandler.handleSpawnExperienceOrb(packet)
        } else if (packet is S12PacketEntityVelocity) {
            netHandler.handleEntityVelocity(packet)
        } else if (packet is S13PacketDestroyEntities) {
            netHandler.handleDestroyEntities(packet)
        } else if (packet is S14PacketEntity) {
            netHandler.handleEntityMovement(packet)
        } else if (packet is S18PacketEntityTeleport) {
            netHandler.handleEntityTeleport(packet)
        } else if (packet is S19PacketEntityStatus) {
            netHandler.handleEntityStatus(packet)
        } else if (packet is S19PacketEntityHeadLook) {
            netHandler.handleEntityHeadLook(packet)
        } else if (packet is S1BPacketEntityAttach) {
            netHandler.handleEntityAttach(packet)
        } else if (packet is S1CPacketEntityMetadata) {
            netHandler.handleEntityMetadata(packet)
        } else if (packet is S1DPacketEntityEffect) {
            netHandler.handleEntityEffect(packet)
        } else if (packet is S1EPacketRemoveEntityEffect) {
            netHandler.handleRemoveEntityEffect(packet)
        } else if (packet is S1FPacketSetExperience) {
            netHandler.handleSetExperience(packet)
        } else if (packet is S20PacketEntityProperties) {
            netHandler.handleEntityProperties(packet)
        } else if (packet is S21PacketChunkData) {
            netHandler.handleChunkData(packet)
        } else if (packet is S22PacketMultiBlockChange) {
            netHandler.handleMultiBlockChange(packet)
        } else if (packet is S23PacketBlockChange) {
            netHandler.handleBlockChange(packet)
        } else if (packet is S24PacketBlockAction) {
            netHandler.handleBlockAction(packet)
        } else if (packet is S25PacketBlockBreakAnim) {
            netHandler.handleBlockBreakAnim(packet)
        } else if (packet is S26PacketMapChunkBulk) {
            netHandler.handleMapChunkBulk(packet)
        } else if (packet is S27PacketExplosion) {
            netHandler.handleExplosion(packet)
        } else if (packet is S28PacketEffect) {
            netHandler.handleEffect(packet)
        } else if (packet is S29PacketSoundEffect) {
            netHandler.handleSoundEffect(packet)
        } else if (packet is S2APacketParticles) {
            netHandler.handleParticles(packet)
        } else if (packet is S2BPacketChangeGameState) {
            netHandler.handleChangeGameState(packet)
        } else if (packet is S2CPacketSpawnGlobalEntity) {
            netHandler.handleSpawnGlobalEntity(packet)
        } else if (packet is S2DPacketOpenWindow) {
            netHandler.handleOpenWindow(packet)
        } else if (packet is S2EPacketCloseWindow) {
            netHandler.handleCloseWindow(packet)
        } else if (packet is S2FPacketSetSlot) {
            netHandler.handleSetSlot(packet)
        } else if (packet is S30PacketWindowItems) {
            netHandler.handleWindowItems(packet)
        } else if (packet is S31PacketWindowProperty) {
            netHandler.handleWindowProperty(packet)
        } else if (packet is S32PacketConfirmTransaction) {
            netHandler.handleConfirmTransaction(packet)
        } else if (packet is S33PacketUpdateSign) {
            netHandler.handleUpdateSign(packet)
        } else if (packet is S34PacketMaps) {
            netHandler.handleMaps(packet)
        } else if (packet is S35PacketUpdateTileEntity) {
            netHandler.handleUpdateTileEntity(packet)
        } else if (packet is S36PacketSignEditorOpen) {
            netHandler.handleSignEditorOpen(packet)
        } else if (packet is S37PacketStatistics) {
            netHandler.handleStatistics(packet)
        } else if (packet is S38PacketPlayerListItem) {
            netHandler.handlePlayerListItem(packet)
        } else if (packet is S39PacketPlayerAbilities) {
            netHandler.handlePlayerAbilities(packet)
        } else if (packet is S3APacketTabComplete) {
            netHandler.handleTabComplete(packet)
        } else if (packet is S3BPacketScoreboardObjective) {
            netHandler.handleScoreboardObjective(packet)
        } else if (packet is S3CPacketUpdateScore) {
            netHandler.handleUpdateScore(packet)
        } else if (packet is S3DPacketDisplayScoreboard) {
            netHandler.handleDisplayScoreboard(packet)
        } else if (packet is S3EPacketTeams) {
            netHandler.handleTeams(packet)
        } else if (packet is S3FPacketCustomPayload) {
            netHandler.handleCustomPayload(packet)
        } else if (packet is S40PacketDisconnect) {
            netHandler.handleDisconnect(packet)
        } else if (packet is S41PacketServerDifficulty) {
            netHandler.handleServerDifficulty(packet)
        } else if (packet is S42PacketCombatEvent) {
            netHandler.handleCombatEvent(packet)
        } else if (packet is S43PacketCamera) {
            netHandler.handleCamera(packet)
        } else if (packet is S44PacketWorldBorder) {
            netHandler.handleWorldBorder(packet)
        } else if (packet is S45PacketTitle) {
            netHandler.handleTitle(packet)
        } else if (packet is S46PacketSetCompressionLevel) {
            netHandler.handleSetCompressionLevel(packet)
        } else if (packet is S47PacketPlayerListHeaderFooter) {
            netHandler.handlePlayerListHeaderFooter(packet)
        } else if (packet is S48PacketResourcePackSend) {
            netHandler.handleResourcePack(packet)
        } else if (packet is S49PacketUpdateEntityNBT) {
            netHandler.handleEntityNBT(packet)
        } else {
            throw IllegalArgumentException("Unable to match packet type to handle: ${packet.javaClass}")
        }
    }

    fun getPacketType(packet: Packet<*>): PacketType {
        val className = packet.javaClass.simpleName
        if (className.startsWith("C", ignoreCase = true)) {
                return PacketType.CLIENTSIDE
        } else if (className.startsWith("S", ignoreCase = true)) {
                return PacketType.SERVERSIDE
        }

        return PacketType.UNKNOWN
    }

    enum class PacketType {
        SERVERSIDE,
        CLIENTSIDE,
        UNKNOWN
    }
}