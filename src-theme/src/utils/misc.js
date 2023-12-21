/**
 * Converts a Java integer to rgba(r, g, b, a) string.
 * @param value The integer to convert.
 * @returns The rgba(r, g, b, a) string.
 * @example
 * intToRgba(0xff000000); // "rgba(0, 0, 0, 255)"
 * intToRgba(-1); // "rgba(255, 255, 255, 255)"
 */
export function intToRgba(value) {
    const alpha = value >>> 24;
    const red = (value >>> 16) & 0xff;
    const green = (value >>> 8) & 0xff;
    const blue = value & 0xff;

    return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
}

/**
 * fun toHex(alpha: Boolean = false): String {
 *         val hex = StringBuilder("#")
 *
 *         hex.append(componentToHex(r))
 *         hex.append(componentToHex(g))
 *         hex.append(componentToHex(b))
 *         if(alpha) hex.append((componentToHex(a)))
 *
 *         return hex.toString().uppercase()
 *     }
 */

export function intToHex(value) {
    const alpha = value >>> 24;
    const red = (value >>> 16) & 0xff;
    const green = (value >>> 8) & 0xff;
    const blue = value & 0xff;

    // Construct hex, only add alpha if it's not 255
    const hex = `#${componentToHex(red)}${componentToHex(green)}${componentToHex(blue)}${alpha !== 255 ? componentToHex(alpha) : ''}`;
    return hex.toUpperCase();
}

export function componentToHex(c) {
    const hex = c.toString(16);
    return hex.length === 1 ? `0${hex}` : hex;
}

/**
 * Convert rgba to Java integer
 * [r, g, b, a]
 * @param rgba
 * @returns {number}
 */
export function rgbaToInt(rgba) {
    const r = rgba[0];
    const g = rgba[1];
    const b = rgba[2];
    const a = rgba[3];
    return ((a << 24) | (r << 16) | (g << 8) | b) >>> 0;
}
