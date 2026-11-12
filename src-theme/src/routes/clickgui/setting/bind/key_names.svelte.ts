import {getPrintableKeyName} from "../../../../integration/rest";

const names = $state<Record<string, string>>({});
const asked = new Set<string>();

/**
 * What the keyboard layout calls a key. Until the client has answered, it is the last part of the key's id.
 */
export function keyName(key: string) {
    if (!asked.has(key)) {
        asked.add(key);
        getPrintableKeyName(key).then(printable => names[key] = printable.localized);
    }

    return names[key] ?? key.split(".").pop()!.toUpperCase();
}
