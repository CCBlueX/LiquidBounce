import type {TextComponent} from "../integration/types";

export type ReplaceableTextComponent = TextComponent | string;

export function createGlobalRegex(pattern?: string): RegExp | null {
    if (!pattern) {
        return null;
    }

    try {
        return new RegExp(pattern, "g");
    } catch {
        return null;
    }
}

export function replaceText(text: string, regex: RegExp, replacement: string): string {
    regex.lastIndex = 0;
    return text.replace(regex, replacement);
}

export function replaceTextComponent(
    component: ReplaceableTextComponent,
    regex: RegExp,
    replacement: string
): ReplaceableTextComponent {
    if (typeof component === "string") {
        return replaceText(component, regex, replacement);
    }

    const text = component.text ? replaceText(component.text, regex, replacement) : component.text;
    const extra = component.extra?.map(child => replaceTextComponent(child, regex, replacement));

    return {
        ...component,
        text,
        extra,
    };
}
