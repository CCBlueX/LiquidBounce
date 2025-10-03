import type { Module } from "./types";
import { getPrintableKeyName } from "./rest";

const KEY_PREFIXES = {
    KEYBOARD: "key.keyboard.",
    MOUSE: "key.mouse.",
    UNKNOWN: "key.keyboard.unknown"
} as const;

export interface ModuleWithKeyBind {
    module: Module;
    keyName: string;
    displayName: string;
}

export function hasValidKeyBind(module: Module): boolean {
    return !!module.keyBind?.boundKey && module.keyBind.boundKey !== KEY_PREFIXES.UNKNOWN;
}

export function formatKeyName(boundKey: string): string {
    if (boundKey.startsWith(KEY_PREFIXES.KEYBOARD)) {
        return boundKey.replace(KEY_PREFIXES.KEYBOARD, "").toUpperCase();
    }
    
    if (boundKey.startsWith(KEY_PREFIXES.MOUSE)) {
        return boundKey.replace(KEY_PREFIXES.MOUSE, "Mouse ").toUpperCase();
    }
    
    return boundKey;
}

export async function resolveKeyName(boundKey: string): Promise<string> {
    try {
        const printableKey = await getPrintableKeyName(boundKey);
        return printableKey.localized;
    } catch {
        return formatKeyName(boundKey);
    }
}

export async function createModuleWithKeyBind(
    module: Module,
    displayName: string
): Promise<ModuleWithKeyBind> {
    const keyName = await resolveKeyName(module.keyBind.boundKey);
    
    return {
        module,
        keyName,
        displayName
    };
}
