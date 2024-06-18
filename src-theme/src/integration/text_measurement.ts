const canvas = document.createElement("canvas");
const context = canvas.getContext("2d")!!;

export function getTextWidth(text: string, font: string) {
    context.font = font;
    const metrics = context.measureText(text);
    return metrics.width;
}