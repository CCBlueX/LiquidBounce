package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

fun String.isItemWithTiers() : Boolean {
    return this.contains(TIER_ID)
}

fun String.generalTiersName() : String {
    return this.split(TIER_ID)[0]   // example: sword:tier:2 -> sword
}

fun String.autoShopItemTier() : Int {
    if (!isItemWithTiers()) {
        return 0
    }

    // example: sword:tier:2 -> 2
    return this.split(TIER_ID)[1].toIntOrNull() ?: 0
}

/**
 * Checks if there is a better item so that it's not necessary to buy the current item
 */
fun hasBetterTierItem(item: String, items: Map<String, Int>) : Boolean {
    return getAllTierItems(item, ModuleAutoShop.currentConfig.itemsWithTiers ?: emptyMap())
        .filter { it.autoShopItemTier() > item.autoShopItemTier() }
        .any { (items[it] ?: 0) > 0 }
}

fun actualTierItem(item: String, itemsWithTiers: Map<String, List<String>> =
    ModuleAutoShop.currentConfig.itemsWithTiers ?: emptyMap()) : String {
    val tiers = itemsWithTiers[item.generalTiersName()] ?: return item
    val tier = item.autoShopItemTier()

    // example: sword:tier:2 -> iron_sword
    return tiers.getOrElse(tier - 1) { item }
}

fun getAllTierItems(item: String, itemsWithTiers: Map<String, List<String>>) : List<String> {
    val generalName = item.generalTiersName()    // example: sword:tier:2 -> sword
    val tiers = itemsWithTiers[generalName] ?: return emptyList()

    // example: [sword:tier:1, sword:tier:2, sword:tier:3, sword:tier:4]
    return List(tiers.size) { index -> "${generalName}$TIER_ID${index + 1}" }
}
