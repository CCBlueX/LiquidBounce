import {listenAlways} from "../integration/ws";
import {getModuleSettings} from "../integration/rest";
import {writable} from "svelte/store";
import type {SpaceSeperatedNamesChangeEvent, ValueChangedEvent} from "../integration/events";

export let spaceSeperatedNames = writable(false);

/**
 * The size of the HUD components relative to the game's GUI scale.
 */
export const hudScale = writable(1);

const HUD_SCALE_KEY = "liquidbounce.modules.hud.scale";

const wordBoundary = /[a-z](?=[A-Z0-9])|[A-Z0-9](?=[A-Z][a-z])/g;

/**
 * Inserts spaces between camel case words, keeping every other character.
 */
export function convertToSpacedString(name: string): string {
    return name.replace(wordBoundary, "$& ");
}

async function updateSettings() {
    const hudSettings = await getModuleSettings("HUD");
    spaceSeperatedNames.set(hudSettings.value.find(n => n.name === "SpaceSeperatedNames")?.value as boolean ?? true);
    hudScale.set(hudSettings.value.find(n => n.name === "Scale")?.value as number ?? 1);
}

listenAlways("spaceSeperatedNamesChange", (e: SpaceSeperatedNamesChangeEvent) => {
   spaceSeperatedNames.set(e.value);
});
listenAlways("valueChanged", (e: ValueChangedEvent) => {
    if (e.value.key?.toLowerCase() === HUD_SCALE_KEY) {
        hudScale.set(e.value.value as number);
    }
});
updateSettings();
