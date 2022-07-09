package net.ccbluex.liquidbounce.utils

object Maps
{
    @JvmField
    val ENCHANTMENT_SHORT_NAME: Map<Int, String> = object : HashMap<Int, String>(27)
    {
        init
        {
            /* Armor */
            put(0, "P") // Protection (Armor)
            put(1, "FP") // Fire Protection (Armor)
            put(2, "FF") // Feather Falling (Boots)
            put(3, "BP") // Blast Protection (Armor)
            put(4, "PP") // Projectile Protection (Armor)
            put(5, "R") // Respiration (Armor)
            put(6, "AA") // Aqua Affinity (Helmet)
            put(7, "T") // Thorns (Armor)
            put(8, "DS") // Depth Strider (Helmet)
            put(9, "FW") // Frost Walker (Boots) (1.12+)

            /* Tools & Sword */
            put(16, "SH") // Sharpness (Sword)
            put(17, "SM") // Smite (Sword)
            put(18, "BoA") // Bane of Arthropods (Sword)
            put(19, "KB") // Knockback (Sword)
            put(20, "FA") // Fire Aspect (Sword)

            put(21, "L") // Looting (Tool)
            put(32, "E") // Efficiency (Tool)
            put(33, "ST") // Silk Touch (Tool)
            put(34, "U") // Unbreaking  (Tool)

            put(35, "F") // Flame (Sword)

            /* Bow */
            put(48, "POW") // Power (Bow)
            put(49, "PUN") // Punch (Bow)
            put(50, "FLA") // Flame (Bow)
            put(51, "INF") // Infinity

            /* Fishing Rod */
            put(61, "LoS") // Luck of the Sea
            put(62, "LU") // Lure

            put(70, "MEN") // Mending (1.9+)
        }
    }

    @JvmField
    val POTION_SHORT_NAME: Map<Int, String> = object : HashMap<Int, String>(27)
    {
        init
        {
            put(1, "Spd") // Speed
            put(2, "Slo") // Slowness
            put(3, "Hst") // Haste
            put(4, "MFtg") // Mining Fatigue
            put(5, "Str") // Strength
            put(6, "Hea") // Instant Health
            put(7, "Dmg") // Instant Damage
            put(8, "Jmp") // Jump Boost
            put(9, "Nau") // Nausea
            put(10, "Reg") // Regeneration
            put(11, "Res") // Resistance
            put(12, "FRes") // Fire Resistance
            put(13, "WBr") // Water Breathing
            put(14, "Inv") // Invisibility
            put(15, "Bli") // Blidness
            put(16, "NV") // Night Vision
            put(17, "Hun") // Hunger
            put(18, "Wea") // Weakness
            put(19, "Poi") // Poison
            put(20, "Wit") // Wither
            put(21, "HBst") // Health Boost
            put(22, "Abs") // Absorption
            put(23, "Sat") // Saturation
            put(24, "Glow") // Glowing
            put(25, "Lev") // Levitation
            put(26, "Luc") // Luck
            put(27, "UnL") // Unluck
        }
    }
}
