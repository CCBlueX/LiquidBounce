/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.features.module.modules.combat.Aimbot
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.TpAura
import net.ccbluex.liquidbounce.features.module.modules.misc.MurderDetector
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.WorkerUtils
import java.awt.Color
import java.text.NumberFormat
import java.util.regex.Pattern
import kotlin.math.pow
import kotlin.random.Random

object ColorUtils : MinecraftInstance()
{
	/** Array of the special characters that are allowed in any text drawing of Minecraft.  */
	val allowedCharactersArray = charArrayOf('/', '\n', '\r', '\t', '\u0000', '', '`', '?', '*', '\\', '<', '>', '|', '\"', ':')

	fun isAllowedCharacter(character: Char): Boolean = character.toInt() != 167 && character.toInt() >= 32 && character.toInt() != 127

	private val COLOR_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]")

	@JvmField
	val hexColors = IntArray(16)

	init
	{
		WorkerUtils.workers.execute {
			repeat(16) { i ->
				val baseColor = (i shr 3 and 1) * 85

				val red = (i shr 2 and 1) * 170 + baseColor + if (i == 6) 85 else 0
				val green = (i shr 1 and 1) * 170 + baseColor
				val blue = (i and 1) * 170 + baseColor

				hexColors[i] = red and 255 shl 16 or (green and 255 shl 8) or (blue and 255)
			}
		}
	}

	@JvmStatic
	fun stripColor(input: String?): String?
	{
		return COLOR_PATTERN.matcher(input ?: return null).replaceAll("")
	}

	@JvmStatic
	fun translateAlternateColorCodes(textToTranslate: String): String
	{
		val chars = textToTranslate.toCharArray()

		repeat(chars.size - 1) { i ->
			if (chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".contains(chars[i + 1], true))
			{
				chars[i] = '\u00A7'
				chars[i + 1] = Character.toLowerCase(chars[i + 1])
			}
		}

		return String(chars)
	}

	fun randomMagicText(text: String): String
	{
		val stringBuilder = StringBuilder()
		val allowedCharacters = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000"

		text.toCharArray().filter(::isAllowedCharacter).forEach { _ -> stringBuilder.append(allowedCharacters.toCharArray()[Random.nextInt(allowedCharacters.length)]) }

		return "$stringBuilder"
	}

	@JvmStatic
	fun getESPColor(entity: IEntity?, colorMode: String, customStaticColor: Color, healthMode: String, indicateHurt: Boolean, indicateTarget: Boolean, indicateFriend: Boolean, rainbowSaturation: Float, rainbowBrightness: Float): Color
	{
		val provider = classProvider

		if (provider.isEntityLivingBase(entity))
		{
			val entityLiving = entity!!.asEntityLivingBase()

			val moduleManager = LiquidBounce.moduleManager

			val aimBot = moduleManager[Aimbot::class.java] as Aimbot
			val killAura = moduleManager[KillAura::class.java] as KillAura
			val tpAura = moduleManager[TpAura::class.java] as TpAura
			val murderDetector = moduleManager[MurderDetector::class.java] as MurderDetector

			// Indicate Hurt
			if (indicateHurt && entityLiving.hurtTime > 0 || indicateTarget && (entity.isEntityEqual(aimBot.target) || entity.isEntityEqual(killAura.target) || tpAura.isTarget(entityLiving))) return Color.RED

			// Indicate Friend
			if (indicateFriend && EntityUtils.isFriend(entityLiving)) return Color.BLUE

			// Indicate Murder
			if (murderDetector.state && murderDetector.murders.contains(entity)) return Color(153, 0, 153)

			when (colorMode.toLowerCase())
			{
				"rainbow" -> return rainbow(saturation = rainbowSaturation, brightness = rainbowBrightness)

				"team" ->
				{
					val chars = entity.displayName!!.formattedText.toCharArray()
					val charsSize = chars.size

					var color = Int.MAX_VALUE
					var i = 0
					while (i < charsSize)
					{
						if (chars[i] != '\u00A7' || i + 1 >= charsSize)
						{
							i++
							continue
						}

						val index = GameFontRenderer.getColorIndex(chars[i + 1])
						if (index < 0 || index > 15)
						{
							i++
							continue
						}

						color = hexColors[index]
						break
					}
					return Color(color)
				}

				"health" ->
				{
					var health = entityLiving.health
					val maxHealth = entityLiving.maxHealth
					if (provider.isEntityPlayer(entity) && (healthMode.equals("Mineplex", ignoreCase = true) || healthMode.equals("Hive", ignoreCase = true))) health = EntityUtils.getPlayerHealthFromScoreboard(entity.asEntityPlayer().gameProfile.name, isMineplex = healthMode.equals("mineplex", ignoreCase = true)).toFloat()
					return getHealthColor(health, maxHealth)
				}
			}
		}

		return if (colorMode.equals("Rainbow", ignoreCase = true)) rainbow(saturation = rainbowSaturation, brightness = rainbowBrightness) else customStaticColor
	}

	@JvmStatic
	fun rainbow(speed: Int = 10, saturation: Float = 1F, brightness: Float = 1F): Color
	{
		val currentColor = Color(Color.HSBtoRGB((System.nanoTime() + 400000L) / 10F.pow(9 + (11 - speed.coerceAtLeast(1).coerceAtMost(10))) % 1, saturation, brightness))
		return Color(currentColor.red / 255F * 1F, currentColor.green / 255f * 1F, currentColor.blue / 255F * 1F, currentColor.alpha / 255F)
	}

	@JvmStatic
	fun rainbow(offset: Long, speed: Int = 10, saturation: Float = 1F, brightness: Float = 1F): Color
	{
		val currentColor = Color(Color.HSBtoRGB((System.nanoTime() + offset) / 10F.pow(9 + (11 - speed.coerceAtLeast(1).coerceAtMost(10))) % 1, saturation, brightness))
		return Color(currentColor.red / 255F * 1F, currentColor.green / 255F * 1F, currentColor.blue / 255F * 1F, currentColor.alpha / 255F)
	}

	@JvmStatic
	fun rainbow(alpha: Float, speed: Int = 10, saturation: Float = 1F, brightness: Float = 1F) = rainbow(400000L, alpha, speed, saturation, brightness)

	@JvmStatic
	fun rainbow(alpha: Int, speed: Int = 10, saturation: Float = 1F, brightness: Float = 1F) = rainbow(400000L, alpha / 255F, speed, saturation, brightness)

	@JvmStatic
	fun rainbow(offset: Long, alpha: Int, speed: Int = 10, saturation: Float = 1F, brightness: Float = 1F) = rainbow(offset, alpha / 255F, speed, saturation, brightness)

	@JvmStatic
	fun rainbow(offset: Long, alpha: Float, speed: Int = 10, saturation: Float = 1F, brightness: Float = 1F): Color
	{
		val currentColor = Color(Color.HSBtoRGB((System.nanoTime() + offset) / 10F.pow(9 + (11 - speed.coerceAtLeast(1).coerceAtMost(10))) % 1, saturation, brightness))
		return Color(currentColor.red / 255F, currentColor.green / 255F, currentColor.blue / 255F, alpha)
	}

	@JvmStatic
	fun blendColors(fractions: FloatArray, colors: Array<Color>, progress: Float): Color
	{
		return if (fractions.size == colors.size)
		{
			val indices = getFractionIndices(fractions, progress)
			if (indices[0] < 0 || indices[0] >= fractions.size || indices[1] < 0 || indices[1] >= fractions.size) return colors[0]
			val range = floatArrayOf(fractions[indices[0]], fractions[indices[1]])
			val colorRange = arrayOf(colors[indices[0]], colors[indices[1]])
			blend(colorRange[0], colorRange[1], 1.0 - ((progress - range[0]) / (range[1] - range[0])))
		}
		else throw IllegalArgumentException("Fractions and colours must have equal number of elements")
	}

	@JvmStatic
	fun getFractionIndices(fractions: FloatArray, progress: Float): IntArray
	{
		val range = IntArray(2)
		var startPoint = 0
		while (startPoint < fractions.size && fractions[startPoint] <= progress) startPoint++
		if (startPoint >= fractions.size) startPoint = fractions.size - 1
		range[0] = startPoint - 1
		range[1] = startPoint
		return range
	}

	@JvmStatic
	fun blend(color1: Color, color2: Color, ratio: Double): Color
	{
		val r = ratio.toFloat()
		val ir = 1.0F - r
		val rgb1 = FloatArray(3)
		val rgb2 = FloatArray(3)
		color1.getColorComponents(rgb1)
		color2.getColorComponents(rgb2)
		val red = (rgb1[0] * r + rgb2[0] * ir).coerceAtLeast(0F).coerceAtMost(1F)
		val green = (rgb1[1] * r + rgb2[1] * ir).coerceAtLeast(0F).coerceAtMost(1F)
		val blue = (rgb1[2] * r + rgb2[2] * ir).coerceAtLeast(0F).coerceAtMost(1F)

		return try
		{
			Color(red, green, blue)
		}
		catch (exp: IllegalArgumentException)
		{
			val nf = NumberFormat.getNumberInstance()
			println(nf.format(red) + "; " + nf.format(green) + "; " + nf.format(blue))
			exp.printStackTrace()

			Color.white
		}
	}

	@JvmStatic
	fun getHealthColor(health: Float, maxHealth: Float): Color = blendColors(floatArrayOf(0f, 0.5f, 1f), arrayOf(Color.RED, Color.YELLOW, Color.GREEN), health / maxHealth).brighter()

	@JvmStatic
	fun applyAlphaChannel(rgb: Int, alpha: Int): Int
	{
		val color = Color(rgb)
		return Color(color.red, color.green, color.blue, alpha).rgb
	}
}
