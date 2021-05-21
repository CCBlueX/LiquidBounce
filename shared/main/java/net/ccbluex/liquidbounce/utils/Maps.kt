package net.ccbluex.liquidbounce.utils

object Maps
{
	@JvmField
	val ENCHANTMENT_SHORT_NAME: Map<Int, Pair<String, String?>> = object : HashMap<Int, Pair<String, String?>>(27)
	{
		init
		{
			/* Armor */
			put(0, "P" to "p") // Protection (Armor)
			put(1, "FP" to null) // Fire Protection (Armor)
			put(2, "FF" to "ff") // Feather Falling (Boots)
			put(3, "BP" to null) // Blast Protection (Armor)
			put(4, "PP" to null) // Projectile Protection (Armor)
			put(5, "R" to null) // Respiration (Armor)
			put(6, "AA" to null) // Aqua Affinity (Helmet)
			put(7, "T" to null) // Thorns (Armor)
			put(8, "DS" to null) // Depth Strider (Helmet)
			put(9, "FW" to null) // Frost Walker (Boots) (1.12+)

			/* Tools & Sword */
			put(16, "SH" to "s") // Sharpness (Sword)
			put(17, "SM" to null) // Smite (Sword)
			put(18, "BoA" to null) // Bane of Arthropods (Sword)
			put(19, "KB" to null) // Knockback (Sword)
			put(20, "FA" to null) // Fire Aspect (Sword)

			put(21, "L" to null) // Looting (Tool)
			put(32, "EFF" to "e") // Efficiency (Tool)
			put(33, "ST" to null) // Silk Touch (Tool)
			put(34, "UNB" to "u") // Unbreaking  (Tool)

			put(35, "F" to null) // Flame (Sword)

			/* Bow */
			put(48, "POW" to "p") // Power (Bow)
			put(49, "PUN" to null) // Punch (Bow)
			put(50, "FLA" to null) // Flame (Bow)
			put(51, "INF" to null) // Infinity

			/* Fishing Rod */
			put(61, "LoS" to null) // Luck of the Sea
			put(62, "LU" to null) // Lure

			put(70, "MEN" to null) // Mending (1.9+)
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
			put(27, "UNL") // Unluck
		}
	}
}
