/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.potion.PotionType
import net.ccbluex.liquidbounce.event.EventState.POST
import net.ccbluex.liquidbounce.event.EventState.PRE
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.misc.FallingPlayer
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

@ModuleInfo(name = "AutoPot", description = "Automatically throws healing potions.", category = ModuleCategory.COMBAT)
class AutoPot : Module() {

    private val healthValue = FloatValue("Health", 15F, 1F, 20F)
    private val delayValue = IntegerValue("Delay", 500, 500, 1000)

    private val openInventoryValue = BoolValue("OpenInv", false)
    private val simulateInventory = BoolValue("SimulateInventory", true)

    private val groundDistanceValue = FloatValue("GroundDistance", 2F, 0F, 5F)
    private val modeValue = ListValue("Mode", arrayOf("Normal", "Jump", "Port"), "Normal")

    private val msTimer = MSTimer()
    private var potion = -1

    @EventTarget
    fun onMotion(motionEvent: MotionEvent) {
        if (!msTimer.hasTimePassed(delayValue.get().toLong()) || mc.playerController.isInCreativeMode)
            return

        val thePlayer = mc.thePlayer ?: return

        when (motionEvent.eventState) {
            PRE -> {
                // Hotbar Potion
                val potionInHotbar = findPotion(36, 45)

                if (thePlayer.health <= healthValue.get() && potionInHotbar != -1) {
                    if (thePlayer.onGround) {
                        when (modeValue.get().toLowerCase()) {
                            "jump" -> thePlayer.jump()
                            "port" -> thePlayer.moveEntity(0.0, 0.42, 0.0)
                        }
                    }

                    // Prevent throwing potions into the void
                    val fallingPlayer = FallingPlayer(
                            thePlayer.posX,
                            thePlayer.posY,
                            thePlayer.posZ,
                            thePlayer.motionX,
                            thePlayer.motionY,
                            thePlayer.motionZ,
                            thePlayer.rotationYaw,
                            thePlayer.moveStrafing,
                            thePlayer.moveForward
                    )

                    val collisionBlock = fallingPlayer.findCollision(20)?.pos

                    if (thePlayer.posY - (collisionBlock?.y ?: 0) >= groundDistanceValue.get())
                        return

                    potion = potionInHotbar
                    mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(potion - 36))

                    if (thePlayer.rotationPitch <= 80F) {
                        RotationUtils.setTargetRotation(Rotation(thePlayer.rotationYaw, RandomUtils.nextFloat(80F, 90F)))
                    }
                    return
                }

                // Inventory Potion -> Hotbar Potion
                val potionInInventory = findPotion(9, 36)
                if (potionInInventory != -1 && InventoryUtils.hasSpaceHotbar()) {
                    if (openInventoryValue.get() && !classProvider.isGuiInventory(mc.currentScreen))
                        return

                    val openInventory = !classProvider.isGuiInventory(mc.currentScreen) && simulateInventory.get()

                    if (openInventory)
                        mc.netHandler.addToSendQueue(createOpenInventoryPacket())

                    mc.playerController.windowClick(0, potionInInventory, 0, 1, thePlayer)

                    if (openInventory)
                        mc.netHandler.addToSendQueue(classProvider.createCPacketCloseWindow())

                    msTimer.reset()
                }
            }
            POST -> {
                if (potion >= 0 && RotationUtils.serverRotation.pitch >= 75F) {
                    val itemStack = thePlayer.inventory.getStackInSlot(potion)

                    if (itemStack != null) {
                        mc.netHandler.addToSendQueue(createUseItemPacket(itemStack, WEnumHand.MAIN_HAND))
                        mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(thePlayer.inventory.currentItem))

                        msTimer.reset()
                    }

                    potion = -1
                }
            }
        }
    }

    private fun findPotion(startSlot: Int, endSlot: Int): Int {
        val thePlayer = mc.thePlayer!!

        for (i in startSlot until endSlot) {
            val stack = thePlayer.inventoryContainer.getSlot(i).stack

            if (stack == null || !classProvider.isItemPotion(stack.item) || !stack.isSplash())
                continue

            val itemPotion = stack.item!!.asItemPotion()

            for (potionEffect in itemPotion.getEffects(stack))
                if (potionEffect.potionID == classProvider.getPotionEnum(PotionType.HEAL).id)
                    return i

            if (!thePlayer.isPotionActive(classProvider.getPotionEnum(PotionType.REGENERATION)))
                for (potionEffect in itemPotion.getEffects(stack))
                    if (potionEffect.potionID == classProvider.getPotionEnum(PotionType.REGENERATION).id)
                        return i
        }

        return -1
    }

    override val tag: String?
        get() = healthValue.get().toString()

}