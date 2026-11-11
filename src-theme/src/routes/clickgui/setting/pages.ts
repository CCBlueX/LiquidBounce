import {getContext, setContext} from "svelte";
import type {ChoiceSetting, ModuleSetting} from "../../../integration/types";

/**
 * How many groups open in place below a page before the next one opens as a page of its own.
 */
const INLINE_DEPTH = 2;

const PAGES = Symbol("setting pages");
const PLACE = Symbol("setting place");

interface Pages {
    open: (trail: string[]) => void;
}

interface Place {
    /**
     * The groups from the root of the settings down to here.
     */
    trail: string[];
    /**
     * Groups opened in place between the page and here.
     */
    depth: number;
}

export function hostPages(open: (trail: string[]) => void) {
    setContext<Pages>(PAGES, {open});
}

export function startPage(trail: string[]) {
    setContext<Place>(PLACE, {trail, depth: 0});
}

/**
 * Called by a group for itself. Tells whether it opens as a page, which is never the case outside a host of
 * pages such as a popover, and gives the function that opens it.
 */
export function enterGroup(name: string) {
    const pages = getContext<Pages | undefined>(PAGES);
    const place = getContext<Place | undefined>(PLACE) ?? {trail: [], depth: 0};
    const trail = [...place.trail, name];

    setContext<Place>(PLACE, {trail, depth: place.depth + 1});

    return {
        asPage: pages !== undefined && place.depth >= INLINE_DEPTH,
        open: () => pages?.open(trail),
    };
}

/**
 * The settings a group shows: of a choice the ones of its active mode, of a toggleable all but its switch.
 */
export function settingsOf(group: ModuleSetting): ModuleSetting[] {
    switch (group.valueType) {
        case "CHOICE": {
            const choice = group as ChoiceSetting;
            return (choice.choices[choice.active]?.value ?? []) as ModuleSetting[];
        }
        case "TOGGLEABLE":
            return (group.value as ModuleSetting[]).slice(1);
        case "CONFIGURABLE":
            return group.value as ModuleSetting[];
        default:
            return [];
    }
}

/**
 * Follows [trail] from [root] as far as it leads to groups.
 */
export function followTrail(root: ModuleSetting, trail: string[]) {
    const groups = [root];
    for (const name of trail) {
        const next = settingsOf(groups[groups.length - 1]).find(setting => setting.name === name);
        if (!next || settingsOf(next).length === 0) {
            break;
        }
        groups.push(next);
    }
    return groups;
}
