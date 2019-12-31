package net.ccbluex.liquidbounce.utils.render;

import net.minecraft.util.ChatAllowedCharacters;

import java.util.Random;
import java.util.regex.Pattern;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public enum ChatColor {

    BLACK('0'),
    DARK_BLUE('1'),
    DARK_GREEN('2'),
    DARK_AQUA('3'),
    DARK_RED('4'),
    DARK_PURPLE('5'),
    GOLD('6'),
    GRAY('7'),
    DARK_GRAY('8'),
    BLUE('9'),
    GREEN('a'),
    AQUA('b'),
    RED('c'),
    LIGHT_PURPLE('d'),
    YELLOW('e'),
    WHITE('f'),
    MAGIC('k'),
    BOLD('l'),
    STRIKETHROUGH('m'),
    UNDERLINE('n'),
    ITALIC('o'),
    RESET('r');

    // Color pattern
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + '§' + "[0-9A-FK-OR]");

    public static final int[] colors = new int[32];

    static {
        for(int index = 0; index < 32; index++) {
            final int var6 = (index >> 3 & 1) * 85;
            int var7 = (index >> 2 & 1) * 170 + var6;
            int var8 = (index >> 1 & 1) * 170 + var6;
            int var9 = (index & 1) * 170 + var6;

            if(index == 6)
                var7 += 85;

            if(index >= 16) {
                var7 /= 4;
                var8 /= 4;
                var9 /= 4;
            }

            colors[index] = (var7 & 255) << 16 | (var8 & 255) << 8 | var9 & 255;
        }
    }

    // Color code
    private final char code;

    ChatColor(final char code) {
        this.code = code;
    }

    public char getChar() {
        return code;
    }

    @Override
    public String toString() {
        return new String(new char[] {'§', code});
    }

    public static String stripColor(final String input) {
        if(input == null)
            return null;

        return ChatColor.STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static String translateAlternateColorCodes(final String textToTranslate) {
        final char[] chars = textToTranslate.toCharArray();

        for(int i = 0; i < chars.length - 1; ++i) {
            if(chars[i] == '&' && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) > -1) {
                chars[i] = '§';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }

        return new String(chars);
    }

    public static String toRandom(final String text) {
        final StringBuilder stringBuilder = new StringBuilder();
        final String allowedCharacters = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";

        for(final char c : text.toCharArray())
            if(ChatAllowedCharacters.isAllowedCharacter(c)) {
                final int index = new Random().nextInt(allowedCharacters.length());
                stringBuilder.append(allowedCharacters.toCharArray()[index]);
            }

        return stringBuilder.toString();
    }
}