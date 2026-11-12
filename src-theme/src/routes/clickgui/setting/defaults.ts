import type {ChoiceSetting, ModuleSetting} from "../../../integration/types";

function isGroup(setting: ModuleSetting) {
    return setting.valueType === "CONFIGURABLE" || setting.valueType === "TOGGLEABLE";
}

// The client leaves an empty list out of an object, the controls put it back in
function comparable(value: unknown) {
    return JSON.stringify(value, (key, inner) => key !== "" && Array.isArray(inner) && inner.length === 0 ? undefined : inner);
}

function same(left: unknown, right: unknown) {
    return comparable(left) === comparable(right);
}

/**
 * Whether the setting, or for a group anything inside it, differs from its default.
 * Of a choice only the active mode counts, the others are not in use. A bind is the key the user picked,
 * so it never counts and a reset leaves it alone.
 */
export function isModified(setting: ModuleSetting): boolean {
    if (setting.valueType === "BIND") {
        return false;
    }

    if (isGroup(setting)) {
        return (setting.value as ModuleSetting[]).some(isModified);
    }

    if (setting.valueType === "CHOICE") {
        const choice = setting as ChoiceSetting;
        const active = choice.choices[choice.active];
        return (choice.defaultActive !== undefined && choice.active !== choice.defaultActive)
            || (active !== undefined && isModified(active));
    }

    return setting.defaultValue !== undefined && !same(setting.value, setting.defaultValue);
}

/**
 * Puts the setting, and for a group everything inside it, back to its default.
 */
export function resetToDefault(setting: ModuleSetting) {
    if (isGroup(setting)) {
        (setting.value as ModuleSetting[]).forEach(resetToDefault);
    } else if (setting.valueType === "CHOICE") {
        const choice = setting as ChoiceSetting;
        choice.active = choice.defaultActive ?? choice.active;
        Object.values(choice.choices).forEach(resetToDefault);
    } else if (setting.defaultValue !== undefined && setting.valueType !== "BIND") {
        // The setting may be a reactive proxy, which structuredClone refuses
        setting.value = JSON.parse(JSON.stringify(setting.defaultValue));
    }
}
