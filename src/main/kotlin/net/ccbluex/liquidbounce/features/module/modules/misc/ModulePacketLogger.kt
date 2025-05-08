/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.common.*
import net.minecraft.network.packet.c2s.config.ReadyC2SPacket
import net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket
import net.minecraft.network.packet.c2s.login.EnterConfigurationC2SPacket
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket
import net.minecraft.network.packet.c2s.login.LoginKeyC2SPacket
import net.minecraft.network.packet.c2s.login.LoginQueryResponseC2SPacket
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.c2s.query.QueryPingC2SPacket
import net.minecraft.network.packet.c2s.query.QueryRequestC2SPacket
import net.minecraft.network.packet.s2c.common.*
import net.minecraft.network.packet.s2c.config.*
import net.minecraft.network.packet.s2c.login.*
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.network.packet.s2c.query.PingResultS2CPacket
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.max

private typealias PacketClass = Class<out Packet<*>>

/**
 * Note: Keep up to date with the minecraft version
 */
private val CLIENT_TO_SERVER: Set<PacketClass> = setOf(
    // Common
    ClientOptionsC2SPacket::class.java,
    CommonPongC2SPacket::class.java,
    CookieResponseC2SPacket::class.java,
    CustomPayloadC2SPacket::class.java,
    KeepAliveC2SPacket::class.java,
    ResourcePackStatusC2SPacket::class.java,

    // Config
    ReadyC2SPacket::class.java,
    SelectKnownPacksC2SPacket::class.java,

    // Handshake
    HandshakeC2SPacket::class.java,

    // Login
    EnterConfigurationC2SPacket::class.java,
    LoginHelloC2SPacket::class.java,
    LoginKeyC2SPacket::class.java,
    LoginQueryResponseC2SPacket::class.java,

    // Play
    AcknowledgeChunksC2SPacket::class.java,
    AcknowledgeReconfigurationC2SPacket::class.java,
    AdvancementTabC2SPacket::class.java,
    BoatPaddleStateC2SPacket::class.java,
    BookUpdateC2SPacket::class.java,
    BundleItemSelectedC2SPacket::class.java,
    ButtonClickC2SPacket::class.java,
    ChatCommandSignedC2SPacket::class.java,
    ChatMessageC2SPacket::class.java,
    ClickSlotC2SPacket::class.java,
    ClientCommandC2SPacket::class.java,
    ClientStatusC2SPacket::class.java,
    ClientTickEndC2SPacket::class.java,
    CloseHandledScreenC2SPacket::class.java,
    CommandExecutionC2SPacket::class.java,
    CraftRequestC2SPacket::class.java,
    CreativeInventoryActionC2SPacket::class.java,
    DebugSampleSubscriptionC2SPacket::class.java,
    HandSwingC2SPacket::class.java,
    JigsawGeneratingC2SPacket::class.java,
    MessageAcknowledgmentC2SPacket::class.java,
    PickItemFromBlockC2SPacket::class.java,
    PickItemFromEntityC2SPacket::class.java,
    PlayerActionC2SPacket::class.java,
    PlayerInputC2SPacket::class.java,
    PlayerInteractBlockC2SPacket::class.java,
    PlayerInteractEntityC2SPacket::class.java,
    PlayerInteractItemC2SPacket::class.java,
    PlayerLoadedC2SPacket::class.java,
    PlayerMoveC2SPacket::class.java,
    PlayerMoveC2SPacket.Full::class.java,
    PlayerMoveC2SPacket.LookAndOnGround::class.java,
    PlayerMoveC2SPacket.OnGroundOnly::class.java,
    PlayerMoveC2SPacket.PositionAndOnGround::class.java,
    PlayerSessionC2SPacket::class.java,
    QueryBlockNbtC2SPacket::class.java,
    QueryEntityNbtC2SPacket::class.java,
    RecipeBookDataC2SPacket::class.java,
    RecipeCategoryOptionsC2SPacket::class.java,
    RenameItemC2SPacket::class.java,
    RequestCommandCompletionsC2SPacket::class.java,
    SelectMerchantTradeC2SPacket::class.java,
    SlotChangedStateC2SPacket::class.java,
    SpectatorTeleportC2SPacket::class.java,
    TeleportConfirmC2SPacket::class.java,
    UpdateBeaconC2SPacket::class.java,
    UpdateCommandBlockC2SPacket::class.java,
    UpdateCommandBlockMinecartC2SPacket::class.java,
    UpdateDifficultyC2SPacket::class.java,
    UpdateDifficultyLockC2SPacket::class.java,
    UpdateJigsawC2SPacket::class.java,
    UpdatePlayerAbilitiesC2SPacket::class.java,
    UpdateSelectedSlotC2SPacket::class.java,
    UpdateSignC2SPacket::class.java,
    UpdateStructureBlockC2SPacket::class.java,
    VehicleMoveC2SPacket::class.java,

    // Query
    QueryPingC2SPacket::class.java,
    QueryRequestC2SPacket::class.java
)

/**
 * Note: Keep up to date with the minecraft version
 */
private val SERVER_TO_CLIENT: Set<PacketClass> = setOf(
    // Common
    CommonPingS2CPacket::class.java,
    CookieRequestS2CPacket::class.java,
    CustomPayloadS2CPacket::class.java,
    CustomReportDetailsS2CPacket::class.java,
    DisconnectS2CPacket::class.java,
    KeepAliveS2CPacket::class.java,
    ResourcePackRemoveS2CPacket::class.java,
    ResourcePackSendS2CPacket::class.java,
    ServerLinksS2CPacket::class.java,
    ServerTransferS2CPacket::class.java,
    StoreCookieS2CPacket::class.java,
    SynchronizeTagsS2CPacket::class.java,

    // Config
    DynamicRegistriesS2CPacket::class.java,
    FeaturesS2CPacket::class.java,
    ReadyS2CPacket::class.java,
    ResetChatS2CPacket::class.java,
    SelectKnownPacksS2CPacket::class.java,

    // Login
    LoginCompressionS2CPacket::class.java,
    LoginDisconnectS2CPacket::class.java,
    LoginHelloS2CPacket::class.java,
    LoginQueryRequestS2CPacket::class.java,
    LoginSuccessS2CPacket::class.java,

    // Play
    AdvancementUpdateS2CPacket::class.java,
    BlockBreakingProgressS2CPacket::class.java,
    BlockEntityUpdateS2CPacket::class.java,
    BlockEventS2CPacket::class.java,
    BlockUpdateS2CPacket::class.java,
    BossBarS2CPacket::class.java,
    BundleDelimiterS2CPacket::class.java,
    BundleS2CPacket::class.java,
    ChatMessageS2CPacket::class.java,
    ChatSuggestionsS2CPacket::class.java,
    ChunkBiomeDataS2CPacket::class.java,
    ChunkDataS2CPacket::class.java,
    ChunkDeltaUpdateS2CPacket::class.java,
    ChunkLoadDistanceS2CPacket::class.java,
    ChunkRenderDistanceCenterS2CPacket::class.java,
    ChunkSentS2CPacket::class.java,
    ClearTitleS2CPacket::class.java,
    CloseScreenS2CPacket::class.java,
    CommandSuggestionsS2CPacket::class.java,
    CommandTreeS2CPacket::class.java,
    CooldownUpdateS2CPacket::class.java,
    CraftFailedResponseS2CPacket::class.java,
    DamageTiltS2CPacket::class.java,
    DeathMessageS2CPacket::class.java,
    DebugSampleS2CPacket::class.java,
    DifficultyS2CPacket::class.java,
    EndCombatS2CPacket::class.java,
    EnterCombatS2CPacket::class.java,
    EnterReconfigurationS2CPacket::class.java,
    EntitiesDestroyS2CPacket::class.java,
    EntityAnimationS2CPacket::class.java,
    EntityAttachS2CPacket::class.java,
    EntityAttributesS2CPacket::class.java,
    EntityDamageS2CPacket::class.java,
    EntityEquipmentUpdateS2CPacket::class.java,
    EntityPassengersSetS2CPacket::class.java,
    EntityPositionS2CPacket::class.java,
    EntityPositionSyncS2CPacket::class.java,
    EntityS2CPacket::class.java,
    EntityS2CPacket.MoveRelative::class.java,
    EntityS2CPacket.Rotate::class.java,
    EntityS2CPacket.RotateAndMoveRelative::class.java,
    EntitySetHeadYawS2CPacket::class.java,
    EntitySpawnS2CPacket::class.java,
    EntityStatusEffectS2CPacket::class.java,
    EntityStatusS2CPacket::class.java,
    EntityTrackerUpdateS2CPacket::class.java,
    EntityVelocityUpdateS2CPacket::class.java,
    ExperienceBarUpdateS2CPacket::class.java,
    ExperienceOrbSpawnS2CPacket::class.java,
    ExplosionS2CPacket::class.java,
    GameJoinS2CPacket::class.java,
    GameMessageS2CPacket::class.java,
    GameStateChangeS2CPacket::class.java,
    HealthUpdateS2CPacket::class.java,
    InventoryS2CPacket::class.java,
    ItemPickupAnimationS2CPacket::class.java,
    LightUpdateS2CPacket::class.java,
    LookAtS2CPacket::class.java,
    MapUpdateS2CPacket::class.java,
    MoveMinecartAlongTrackS2CPacket::class.java,
    NbtQueryResponseS2CPacket::class.java,
    OpenHorseScreenS2CPacket::class.java,
    OpenScreenS2CPacket::class.java,
    OpenWrittenBookS2CPacket::class.java,
    OverlayMessageS2CPacket::class.java,
    ParticleS2CPacket::class.java,
    PlayerAbilitiesS2CPacket::class.java,
    PlayerActionResponseS2CPacket::class.java,
    PlayerListHeaderS2CPacket::class.java,
    PlayerListS2CPacket::class.java,
    PlayerPositionLookS2CPacket::class.java,
    PlayerRemoveS2CPacket::class.java,
    PlayerRespawnS2CPacket::class.java,
    PlayerRotationS2CPacket::class.java,
    PlayerSpawnPositionS2CPacket::class.java,
    PlaySoundFromEntityS2CPacket::class.java,
    PlaySoundS2CPacket::class.java,
    ProfilelessChatMessageS2CPacket::class.java,
    ProjectilePowerS2CPacket::class.java,
    RecipeBookAddS2CPacket::class.java,
    RecipeBookRemoveS2CPacket::class.java,
    RecipeBookSettingsS2CPacket::class.java,
    RemoveEntityStatusEffectS2CPacket::class.java,
    RemoveMessageS2CPacket::class.java,
    ScoreboardDisplayS2CPacket::class.java,
    ScoreboardObjectiveUpdateS2CPacket::class.java,
    ScoreboardScoreResetS2CPacket::class.java,
    ScoreboardScoreUpdateS2CPacket::class.java,
    ScreenHandlerPropertyUpdateS2CPacket::class.java,
    ScreenHandlerSlotUpdateS2CPacket::class.java,
    SelectAdvancementTabS2CPacket::class.java,
    ServerMetadataS2CPacket::class.java,
    SetCameraEntityS2CPacket::class.java,
    SetCursorItemS2CPacket::class.java,
    SetPlayerInventoryS2CPacket::class.java,
    SetTradeOffersS2CPacket::class.java,
    SignEditorOpenS2CPacket::class.java,
    SimulationDistanceS2CPacket::class.java,
    StartChunkSendS2CPacket::class.java,
    StatisticsS2CPacket::class.java,
    StopSoundS2CPacket::class.java,
    SubtitleS2CPacket::class.java,
    SynchronizeRecipesS2CPacket::class.java,
    TeamS2CPacket::class.java,
    TickStepS2CPacket::class.java,
    TitleFadeS2CPacket::class.java,
    TitleS2CPacket::class.java,
    UnloadChunkS2CPacket::class.java,
    UpdateSelectedSlotS2CPacket::class.java,
    UpdateTickRateS2CPacket::class.java,
    VehicleMoveS2CPacket::class.java,
    WorldBorderCenterChangedS2CPacket::class.java,
    WorldBorderInitializeS2CPacket::class.java,
    WorldBorderInterpolateSizeS2CPacket::class.java,
    WorldBorderSizeChangedS2CPacket::class.java,
    WorldBorderWarningBlocksChangedS2CPacket::class.java,
    WorldBorderWarningTimeChangedS2CPacket::class.java,
    WorldEventS2CPacket::class.java,
    WorldTimeUpdateS2CPacket::class.java,

    // Query
    PingResultS2CPacket::class.java,
    QueryResponseS2CPacket::class.java
)

/**
 * Module PacketLogger
 *
 * Prints all packets and their fields.
 *
 * @author ccetl, 1zun4, sqlerrorthing
 */
object ModulePacketLogger : ClientModule("PacketLogger", Category.MISC) {
    init {
        doNotIncludeAlways()

        tree(IncomingTransferOrigin)
        tree(OutgoingTransferOrigin)
    }

    object IncomingTransferOrigin : PacketBound("Incoming", TransferOrigin.INCOMING, SERVER_TO_CLIENT, false)
    object OutgoingTransferOrigin : PacketBound("Outgoing", TransferOrigin.OUTGOING, CLIENT_TO_SERVER, true)
}

sealed class PacketBound(
    name: String,
    private val origin: TransferOrigin,
    packets: Set<PacketClass>,
    enabled: Boolean
) : ToggleableConfigurable(ModulePacketLogger, name, enabled) {
    private val classNames = packets.associateWithTo(ConcurrentHashMap()) { it.getPacketName() }
    private val fieldNames = ConcurrentHashMap<Field, String>()

    private val selectedPackets by multiStringChoice(
        "Packets",
        choices = classNames.values
            .map { it.removePacketSuffix() }
            .sorted()
            .toSet()
    )
    private val filter by enumChoice("Filter", Filter.BLACKLIST)

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = READ_FINAL_STATE) { event ->
        processPacket(event.packet, event.origin, event.isCancelled)
    }

    @Suppress("ReturnCount")
    fun processPacket(packet: Packet<*>, origin: TransferOrigin = this.origin, isCancelled: Boolean = false) {
        if (origin != this.origin) {
            return
        }

        val name = packet::class.java.getCachedPacketName()

        if (!filter(name.removePacketSuffix(), selectedPackets)) {
            return
        }

        buildLog(packet, name, isCancelled).also { log ->
            chat(log, metadata = MessageMetadata(prefix = false))
        }
    }

    private fun buildLog(packet: Packet<*>, packetName: String, cancelled: Boolean): MutableText {
        return Text.empty().formatted(Formatting.WHITE).apply {
            append(ModulePacketLogger.message(if (origin == TransferOrigin.INCOMING) "receive" else "send"))
            append(" $packetName")

            if (cancelled) {
                append(" (".asText().formatted(Formatting.RED))
                append(ModulePacketLogger.message("cancelled").formatted(Formatting.RED))
                append(")".asText().formatted(Formatting.RED))
            }

            appendFields(packet::class.java, packet)
        }
    }

    private fun MutableText.appendFields(clazz: PacketClass, packet: Packet<*>) {
        var start = true

        var currentClass: Class<*>? = clazz

        while (currentClass.isNotRoot()) {
            currentClass.declaredFields.forEach { field ->
                if (Modifier.isStatic(field.modifiers)) {
                    return@forEach
                }

                field.isAccessible = true

                if (start) {
                    append(":")
                    start = false
                }

                append("\n")

                val name = field.remappedFieldName(currentClass.name)
                append("-$name: ".asText().formatted(Formatting.GRAY))
                append(field.getValue(packet).asText().formatted(Formatting.GRAY))
            }

            currentClass = currentClass.superclass
        }
    }

    private fun Field.remappedFieldName(className: String): String {
        return fieldNames.computeIfAbsent(this) {
            EnvironmentRemapper.remapField(className, this.name)
        }
    }

    private fun Field.getValue(instance: Any): String {
        return runCatching {
            get(instance)?.toString()
        }.getOrDefault("null") ?: "null"
    }

    private fun PacketClass.getCachedPacketName(): String {
        return classNames.computeIfAbsent(this) {
            this.getPacketName()
        }
    }

    private fun String.removePacketSuffix(): String {
        return this
            .removeSuffix("S2CPacket")
            .removeSuffix("C2SPacket")

            .replace("S2CPacket.", "")
            .replace("C2SPacket.", "")
    }
}

private fun PacketClass.getPacketName(): String {
    val classNames = ArrayDeque<CharSequence>()
    classNames.add(this.getClassName())

    var superclass: Class<*>? = superclass

    while (superclass.isNotRoot()) {
        classNames.addFirst(superclass.getClassName())
        superclass = superclass.superclass
    }

    return classNames.joinToString(".")
}

private fun Class<*>.getClassName(): CharSequence {
    val remapClassName = EnvironmentRemapper.remapClass(this)
    val lastDotIndex = remapClassName.lastIndexOf('.')
    val lastDollarIndex = remapClassName.lastIndexOf('$')
    return remapClassName.subSequence(max(lastDotIndex, lastDollarIndex) + 1, remapClassName.length)
}

@OptIn(ExperimentalContracts::class)
private fun Class<*>?.isNotRoot(): Boolean {
    contract {
        returns(true) implies (this@isNotRoot != null)
    }
    return !(this == null || this === Record::class.java || this.superclass == null)
}
