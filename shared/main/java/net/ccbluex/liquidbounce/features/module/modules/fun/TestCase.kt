package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import kotlin.random.Random

@ModuleInfo(name = "TestCase", description = "", category = ModuleCategory.FUN, canEnable = false)
class TestCase : Module() {

    //This class is only intended dev builds

    val armorPices = arrayOf("diamond_helmet 1 ", "diamond_leggings 1 ", "diamond_chestplate 1 ", "diamond_boots 1 ",
            "leather_helmet 1 ", "leather_leggings 1 ", "leather_chestplate 1 ", "leather_boots 1 ",
            "golden_helmet 1 ", "golden_leggings 1 ", "golden_chestplate 1 ", "golden_boots 1 ",
            "iron_helmet 1 ", "iron_leggings 1 ", "iron_chestplate 1 ", "iron_boots 1 ",
            "chainmail_helmet 1 ", "chainmail_leggings 1 ", "chainmail_chestplate 1 ", "chainmail_boots 1 ")

    var enchant = arrayOf("{ench:[{id:0,lvl:1}]}", "{ench:[{id:0,lvl:2}]}", "{ench:[{id:0,lvl:3}]}", "{ench:[{id:0,lvl:4}]}", "{ench:[{id:34,lvl:1}]}", "{ench:[{id:34,lvl:2}]}", "{ench:[{id:34,lvl:3}]}",
            "{ench:[{id:0,lvl:1},{id:34,lvl:1}]}", "{ench:[{id:0,lvl:2},{id:34,lvl:1}]}", "{ench:[{id:0,lvl:3},{id:34,lvl:1}]}", "{ench:[{id:0,lvl:4},{id:34,lvl:1}]}",
            "{ench:[{id:0,lvl:1},{id:34,lvl:1}]}", "{ench:[{id:0,lvl:1},{id:34,lvl:2}]}", "{ench:[{id:0,lvl:1},{id:34,lvl:3}]}",
            "{ench:[{id:0,lvl:2},{id:34,lvl:1}]}", "{ench:[{id:0,lvl:2},{id:34,lvl:2}]}", "{ench:[{id:0,lvl:2},{id:34,lvl:3}]}",
            "{ench:[{id:0,lvl:3},{id:34,lvl:1}]}", "{ench:[{id:0,lvl:3},{id:34,lvl:2}]}", "{ench:[{id:0,lvl:3},{id:34,lvl:3}]}")

    override fun onEnable() {
        if (mc.playerController.isNotCreative)
            return
        for (i in 0 until 9*4) {
            val args: Array<String> = arrayOf("give", armorPices.random() + Random(1).nextInt(70), enchant.random())
            val itemStack = ItemUtils.createItem(StringUtils.toCompleteString(args, 1))
            mc.netHandler.addToSendQueue(classProvider.createCPacketCreativeInventoryAction(if (i < 9) i + 36 else i, itemStack))
        }
        super.onEnable()
    }
}
