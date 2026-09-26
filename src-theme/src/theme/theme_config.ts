import {listenAlways} from "../integration/ws";
import {getModuleSettings} from "../integration/rest";
import {writable} from "svelte/store";
import type {SpaceSeperatedNamesChangeEvent} from "../integration/events";

export let spaceSeperatedNames = writable(false);

const regex = /[A-Z]?[a-z]+|[A-Z]+[0-9]+|[0-9]+|[A-Z]+(?![a-z])/g;

/**
 * Handles space seperated names if enabled.
 */
export function convertToSpacedString(name: string): string {
    return (name.match(regex) as string[]).join(" ");
}

async function updateSettings() {
    const hudSettings = await getModuleSettings("HUD");
    spaceSeperatedNames.set(hudSettings.value.find(n => n.name === "SpaceSeperatedNames")?.value as boolean ?? true);
}

listenAlways("spaceSeperatedNamesChange", (e: SpaceSeperatedNamesChangeEvent) => {
   spaceSeperatedNames.set(e.value);
});
updateSettings();
