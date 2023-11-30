/**
 * Converts a Java integer to rgba(r, g, b, a) string.
 * @param value The integer to convert.
 * @returns The rgba(r, g, b, a) string.
 * @example
 * intToRgba(0xff000000); // "rgba(0, 0, 0, 255)"
 * intToRgba(-1); // "rgba(255, 255, 255, 255)"
 */
export function intToRgba(value: number): string {
  const alpha = value >>> 24;
  const red = (value >>> 16) & 0xff;
  const green = (value >>> 8) & 0xff;
  const blue = value & 0xff;

  return `rgba(${red}, ${green}, ${blue}, ${alpha})`;
}
