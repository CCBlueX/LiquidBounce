import {type Writable, writable} from "svelte/store";
import type {Module} from "../../integration/types";


export interface TDescription {
    description: string;
    anchor: "left" | "right",
    x: number;
    y: number;
}

interface PanelConfig {
    top: number;
    left: number;
    expanded: boolean;
    scrollTop: number;
    zIndex: number;
}

export const os: Writable<string | null> = writable<string | null>(null);
export const locked = writable(false);
export const description: Writable<TDescription | null> = writable(null);
export const panelId: Writable<string | null> = writable(null);
export const maxPanelZIndex: Writable<number> = writable(0);
export const query = writable<string>("")
export const highlightModuleName: Writable<string | null> = writable(null);
export const savedConfigs = writable<Record<string, PanelConfig>>({});
export const scaleFactor: Writable<number> = writable(2);
export const panelLength: Writable<number> = writable(66);
export const fontSize: Writable<number> = writable(14);
export const showGrid: Writable<boolean> = writable(false);
export const moduleAutoCollapse: Writable<boolean> = writable(false);
export const snappingEnabled: Writable<boolean> = writable(true);
export const moduleDescription: Writable<boolean> = writable(true);
export const gridSize: Writable<number> = writable(10);

export const showResults = writable<boolean>(false);

export const filteredModules = writable<Module[]>([]);

export const showSearch = writable(false);
