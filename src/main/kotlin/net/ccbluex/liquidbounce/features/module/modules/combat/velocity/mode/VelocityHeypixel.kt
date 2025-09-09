package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode


import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.entity.Entity
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket
import net.minecraft.util.Hand

internal object VelocityHeypixel : VelocityMode("Heypixel") {
    private val attackCount by int("AttackCount", 6, 0..20)
    private val chance by int("Chance", 100, 0..100, "%")
    private val jumpReset by boolean("JumpReset", true)
    private val ignoreBackTracking by boolean("IgnoreBackTracking",true)
    private object Cooldown : ToggleableConfigurable(this, "Cooldown", true) {
        val maxAttackCount by int("MaxAttackCount", 30, 0..100)
        val cooldownTicks by int("CooldownTicks", 20, 0..100, "ticks")
        val byHighCPSWarning by boolean("ByHighCPSWarning", true)
        }

        init {
            tree(Cooldown)
        }

        private var canReduce = false
        private var target: Entity? = null
        private var totalAttackCount = 0
        private var cooldownTicks = 0
        private var jump = false


    override val running: Boolean
        get() = super.running &&
            (!ignoreBackTracking || !ModuleBacktrack.isLagging())

    private fun reset() {
            canReduce = false
            target = null
        }

        private fun findTarget(): Entity? {
            if (ModuleKillAura.running && ModuleKillAura.targetTracker.target != null) {
                return ModuleKillAura.targetTracker.target
            }

            return raytraceEntity(
                ModuleKillAura.range.toDouble(),
                RotationManager.serverRotation
            ) { !it.isRemoved && it.shouldBeAttacked() }?.entity
        }

        private fun cooldown() {
            totalAttackCount = 0
            cooldownTicks = Cooldown.cooldownTicks
        }

        @Suppress("unused","ComplexCondition")
        private val tickHandler = tickHandler {
            if (!CombatManager.isInCombat) totalAttackCount = 0

            if (cooldownTicks > 0) {
                cooldownTicks--
            }

            if (player.hurtTime == 0) {
                reset()
            }

            if (canReduce) {
                if (target != null
                    && player.isAlive
                    && !player.isSpectator
                    && !player.abilities.flying
                    && !player.usingItem
                ) {
                    player.setVelocity(
                        player.velocity.x * 0.07776,
                        player.velocity.y,
                        player.velocity.z * 0.07776
                    )
                }
                reset()
            }
        }

        @Suppress("unused")
        private val packetEventHandler = handler<PacketEvent> { event ->
            val packet = event.packet


            if (event.packet is EntityVelocityUpdateS2CPacket
                && packet.entityId == player.id
                && (1..100).random() <= chance
            ) {
                target = findTarget() ?: return@handler

                if (cooldownTicks == 0) {
                    val sprinting = player.isSprinting

                    if (!sprinting) {
                        network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly
                                (player.isOnGround, player.horizontalCollision))
                        network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SPRINTING))
                    }

                    if (Cooldown.enabled && totalAttackCount + attackCount > Cooldown.maxAttackCount) {
                        cooldown()
                    } else {
                        var attacked = true
                        reset()

                        for (i in 1..attackCount) {
                            val entityHitResult = raytraceEntity(
                                ModuleKillAura.range.toDouble(),
                                RotationManager.serverRotation
                            ) { it == target }

                            if (entityHitResult == null) {
                                attacked = false
                                break
                            }

                            network.sendPacket(PlayerInteractEntityC2SPacket.attack(target, false))
                            network.sendPacket(HandSwingC2SPacket(Hand.MAIN_HAND))
                            totalAttackCount++
                        }

                        this.canReduce = attacked
                    }

                    if (!sprinting) {
                        network.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING))
                    }
                }

                if (jumpReset) jump = true
            }

            if (packet is GameMessageS2CPacket
                && Cooldown.enabled
                && Cooldown.byHighCPSWarning
                && packet.content.string == "警告 您当前的点击行为疑似作弊, 请立即停止作弊行为或者降低操作频率!"
            ) {
                cooldown()
            }
        }

        @Suppress("unused")
        private val movementInputEventHandler = handler<MovementInputEvent> { event ->
            if (jumpReset && jump) {
                if (!InventoryManager.isInventoryOpen
                    && mc.currentScreen !is GenericContainerScreen
                    && player.isOnGround
                ) {
                    event.jump = true
                }
                jump = false
            }
        }

        override fun enable() {
            reset()
            totalAttackCount = 0
            cooldownTicks = 0
            jump = false
        }

    }
