import {getModuleSettings} from '../../../../integration/rest';
import type {
    ChoiceSetting,
    ChooseSetting,
    ConfigurableSetting,
    FloatRangeSetting,
    FloatSetting,
    IntRangeSetting,
    IntSetting,
    MultiChooseSetting, TextSetting,
} from '../../../../integration/types';
import {ArraylistRenderSettings, primaryRgb, secondaryRgb} from '../../../../util/Theme/ThemeManager';
import {type Readable} from "svelte/store";
import Color from 'colorjs.io';

let currentRenderSettings = new Set<string>();

export async function getPrefixAsync(name: string) {
    const settings = await getModuleSettings(name);
    let value = '';

    // @ts-ignore
    const checkSetting = (setting: any): string | null => {
        const valueType = setting.valueType.toLowerCase();
        if (!currentRenderSettings.size || currentRenderSettings.has(valueType)) {
            switch (valueType) {
                case 'choice':
                    return (setting as ChoiceSetting).active;
                case 'choose':
                    return (setting as ChooseSetting).value;
                case 'multi_choose':
                    return (setting as MultiChooseSetting).value.toString();
                case 'int_range':
                    const intRange = setting as IntRangeSetting;
                    return `${intRange.value.from}-${intRange.value.to}`;
                case 'float_range':
                    const floatRange = setting as FloatRangeSetting;
                    return `${floatRange.value.from}-${floatRange.value.to}`;
                case 'text':
                    return (setting as TextSetting).value
                case 'int':
                    return (setting as IntSetting).value.toString();
                case 'float':
                    return (setting as FloatSetting).value.toString();
                default:
                    return null;
            }
        }
        return null;
    };


    for (const setting of settings.value) {
        const result = checkSetting(setting);
        if (result) {
            return result;
        }
    }

    const checkConfigurable = (configurable: ConfigurableSetting): string | null => {
        for (const setting of configurable.value) {
            const result = checkSetting(setting);
            if (result) {
                return result;
            }

            if (setting.valueType === 'CONFIGURABLE') {
                const nestedResult = checkConfigurable(setting as ConfigurableSetting);
                if (nestedResult) {
                    return nestedResult;
                }
            }
        }
        return null;
    };

    const configurable = settings.value.find(n => n.valueType === 'CONFIGURABLE');
    if (configurable) {
        const configurableValue = checkConfigurable(configurable as ConfigurableSetting);
        if (configurableValue) {
            return configurableValue;
        }
    }

    return value;
}

interface RGBColor {
    r: number;
    g: number;
    b: number;
}

let currentPrimaryRgb = '';
let currentSecondaryRgb = '';

export function subscribeRenderSettings(callback?: (settings: Set<string>) => void) {
    return ArraylistRenderSettings.subscribe(settings => {
        currentRenderSettings = settings;
        callback?.(settings);
    });
}

export function subscribeColors() {
    const unsub1 = (primaryRgb as Readable<string>).subscribe((v) => {
        currentPrimaryRgb = v;
        arraylistGradient();
    });
    const unsub2 = (secondaryRgb as Readable<string>).subscribe((v) => {
        currentSecondaryRgb = v;
        arraylistGradient();
    });
    return [unsub1, unsub2] as [() => void, () => void];
}

function parseRgbString(str: string): RGBColor {
    const parts = str.split(',').map((x) => parseInt(x.trim()));
    return {r: parts[0] || 0, g: parts[1] || 0, b: parts[2] || 0};
}

export function colorInterpolate(a: RGBColor, b: RGBColor, t: number): RGBColor {
    t = Math.max(0, Math.min(1, t));

    const colorA = new Color("srgb", [a.r / 255, a.g / 255, a.b / 255]);
    const colorB = new Color("srgb", [b.r / 255, b.g / 255, b.b / 255]);

    const interpolated = colorA.mix(colorB, t, {space: "oklch", outputSpace: "srgb"});

    const [red, green, blue] = interpolated.coords.map(x => Math.round(x * 255));

    return {r: red, g: green, b: blue};
}

let speed = 50;
let progress = 0;
const tSpeed = (0.04 / 20) * speed;

export function arraylistGradient() {
    const container = document.getElementById('arraylist');
    if (!container) return;

    const rgb1 = parseRgbString(currentPrimaryRgb);
    const rgb2 = parseRgbString(currentSecondaryRgb);
    const children = container.children as HTMLCollectionOf<HTMLElement>;
    const total = children.length;

    for (let i = 0; i < total; i++) {
        const el = children[i];
        if (el.id !== 'module-name') continue;

        const pct = 1 - i / total + 0.5 * Math.sin(0.5 * i + progress);
        const {r, g, b} = colorInterpolate(rgb1, rgb2, pct);

        el.style.color = `rgba(${r}, ${g}, ${b}, 0.8)`;
        el.style.textShadow = `0.25px 0.25px 0 rgb(${r}, ${g}, ${b})`;
        el.style.filter = `drop-shadow(0 0 4px rgba(${r}, ${g}, ${b}, 0.1))`;
        el.style.backgroundImage = `
      linear-gradient(rgba(0,0,0,0.1), rgba(0,0,0,0.1)),
      linear-gradient(rgba(${r}, ${g}, ${b}, 0.2), rgba(${r}, ${g}, ${b}, 0.2))
    `;
        el.style.backgroundBlendMode = 'overlay';

        const sidebar = el.querySelector('.side-bar') as HTMLElement;
        if (sidebar) {
            sidebar.style.backgroundColor = `rgba(${r}, ${g}, ${b}, 1)`;
            sidebar.style.boxShadow = `0 0 6px rgba(${r}, ${g}, ${b}, 0.8)`;
        }
    }

    progress += tSpeed;
    if (progress >= Math.PI * 2) progress = 0;
}

export function destroyGradient(intervalId: number, unsubs: [() => void, () => void]) {
    clearInterval(intervalId);
    unsubs[0]();
    unsubs[1]();
}
