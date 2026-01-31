export function mapToColor(value: number): string {
    if (value <= 0) {
        return 'rgb(255, 0, 0)';
    } else if (value <= 60) {
        return `rgb(255, ${Math.floor(value * 255 / 60)}, 0)`;
    } else if (value <= 120) {
        return `rgb(${Math.floor((120 - value) * 255 / 60)}, 255, 0)`;
    } else {
        return 'rgb(0, 255, 0)';
    }
}

export function argbIntToRgba(argb: number) {
    const u = argb >>> 0;

    const a = (u >> 24) & 0xff;
    const r = (u >> 16) & 0xff;
    const g = (u >> 8) & 0xff;
    const b = u & 0xff;

    return `rgba(${r}, ${g}, ${b}, ${(a / 255).toFixed(3)})`;
}

export function argbIntToRgbValue(argb: number) {
    const u = argb >>> 0;

    // ALWAYS FULL ALPHA
    const r = (u >> 16) & 0xff;
    const g = (u >> 8) & 0xff;
    const b = u & 0xff;

    return `${r} ${g} ${b}`;
}
