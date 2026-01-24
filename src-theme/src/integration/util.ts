import type {GroupedModules, Module} from "./types"

export const delay = (millis: number) => new Promise(resolve => setTimeout(resolve, millis));

export function groupByCategory(modules: Module[]): GroupedModules {
    return modules.reduce((acc: GroupedModules, current: Module) => {
        const { category } = current;
        if (!acc[category]) {
            acc[category] = [];
        }
        acc[category].push(current);
        return acc;
    }, {});
}

export function rgbaToInt(rgba: number[]): number {
    const [r, g, b, a] = rgba;
    return (
        ((a & 0xff) << 24) |
        ((r & 0xff) << 16) |
        ((g & 0xff) << 8) |
        ((b & 0xff) << 0)
    );
}

export function rgbaToHex(rgba: number[]): string {
    const [r, g, b, a] = rgba;
    const alpha = a === 255 ? "" : a.toString(16).padStart(2, "0");
    return `#${r.toString(16).padStart(2, "0")}${g
        .toString(16)
        .padStart(2, "0")}${b.toString(16).padStart(2, "0")}${alpha}`;
}

export function intToRgba(value: number): number[] {
    const red = (value >> 16) & 0xff;
    const green = (value >> 8) & 0xff;
    const blue = (value >> 0) & 0xff;
    const alpha = (value >> 24) & 0xff;
    return [red, green, blue, alpha];
}

export const swap = (array: any[], i: number, j: number) => {
    if (i < 0 || i >= array.length || j < 0 || j >= array.length) return;

    const it = array[i];
    array[i] = array[j];
    array[j] = it;
}

export const contentEquals = <T>(a: T[], b: T[]): boolean => {
    if (a.length !== b.length) return false;
    return a.every((item, index) => item === b[index]);
}

export const getHashParams = (): URLSearchParams => {
    const hash = window.location.hash.split('?')[1] || '';
    return new URLSearchParams(hash);
};
