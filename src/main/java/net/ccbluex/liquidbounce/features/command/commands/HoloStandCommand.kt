/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.minecraft.item.Items
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NBTTagList
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket

object HoloStandCommand : Command("holostand") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 4) {
            if (!mc.interactionManager.currentGameMode.isCreative) {
                chat("§c§lError: §3You need to be in creative mode.")
                return
            }

            try {
                val x = args[1].toDouble()
                val y = args[2].toDouble()
                val z = args[3].toDouble()
                val message = StringUtils.toCompleteString(args, 4)

                val itemStack = ItemStack(Items.armor_stand)
                val base = NBTTagCompound()
                val entityTag = NBTTagCompound()
                entityTag.setInteger("Invisible", 1)
                entityTag.setString("CustomName", message)
                entityTag.setInteger("CustomNameVisible", 1)
                entityTag.setInteger("NoGravity", 1)
                val position = NBTTagList()
                position.appendTag(NbtDouble(x))
                position.appendTag(NbtDouble(y))
                position.appendTag(NbtDouble(z))
                entityTag.setTag("Pos", position)
                base.setTag("EntityTag", entityTag)
                itemStack.tagCompound = base
                itemStack.setStackDisplayName("§c§lHolo§eStand")
                sendPacket(CreativeInventoryActionC2SPacket(36, itemStack))

                chat("The HoloStand was successfully added to your inventory.")
            } catch (exception: NumberFormatException) {
                chatSyntaxError()
            }

            return
        }

        chatSyntax("holostand <x> <y> <z> <message...>")
    }
}