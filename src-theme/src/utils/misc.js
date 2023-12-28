/**
 * [r, g, b, a]
 */
export function intToRgba(value) {
    const red = (value >> 16) & 0xFF;
    const green = (value >> 8) & 0xFF;
    const blue = (value >> 0) & 0xFF;;
    const alpha = (value >> 24) & 0xff;
    return [red, green, blue, alpha];
}

/**
 * Returns the RGB value representing the color in the default sRGB ColorModel. 
 * (Bits 24-31 are alpha, 16-23 are red, 8-15 are green, 0-7 are blue).
 * 
 * @param rgba [r, g, b, a]
 * @returns {number}
 */
export function rgbaToInt(rgba) {
    const [r, g, b, a] = rgba;
    return ((a & 0xFF) << 24) |
        ((r & 0xFF) << 16) |
        ((g & 0xFF) << 8)  |
        ((b & 0xFF) << 0);
}

/**
 * Converts an RGB color value to hex with alpha channel, if present (not 255).
 *
 * Takes [r, g, b, a] (0-255, 0-255, 0-255, 0-255)
 * For example, [255, 0, 0, 255] -> '#FF0000'
 *
 * @returns {string} hex color
 * @param rgba
 */
export function rgbaToHex(rgba) {
    const [r, g, b, a] = rgba;
    const alpha = a === 255 ? '' : a.toString(16).padStart(2, '0');
    return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}${alpha}`;
}
