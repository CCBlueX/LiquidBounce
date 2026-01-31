import {listenAlways} from "../integration/ws";
import {getMetadata, getModuleSettings} from "../integration/rest";
import {writable} from "svelte/store";
import type {SpaceSeperatedNamesChangeEvent} from "../integration/events";
import type {ThemeMetadata} from "../integration/types";

export const spaceSeperatedNames = writable(false);

/**
 * Handles space seperated names if enabled.
 */
export function convertToSpacedString(name: string): string {
    const regex = /[A-Z]?[a-z]+|[0-9]+|[A-Z]+(?![a-z])/g;
    return (name.match(regex) as string[]).join(" ");
}

async function updateSettings() {
    const hudSettings = await getModuleSettings("HUD");
    spaceSeperatedNames.set(hudSettings.value.find(n => n.name === "SpaceSeperatedNames")?.value as boolean ?? true);
}

let themeMetadata: ThemeMetadata | undefined;

/**
 * Loads cached theme metadata or fetch it from API.
 */
export const loadThemeMetadata = async () => themeMetadata ?? (themeMetadata = await getMetadata());

listenAlways("spaceSeperatedNamesChange", (e: SpaceSeperatedNamesChangeEvent) => {
   spaceSeperatedNames.set(e.value);
});
updateSettings();
