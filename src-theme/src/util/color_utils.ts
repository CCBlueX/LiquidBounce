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