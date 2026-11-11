import type {ChoiceSetting, ConfigurableSetting, Module, ModuleCategory, ModuleSetting} from "../../../../integration/types";
import {getCategories, getModule, getModules, getModuleSettings} from "../../../../integration/rest";
import {persistentDataLoaded, setItem} from "../../../../integration/persistent_storage";
import {isModified} from "../../setting/defaults";
import {UNKNOWN_KEY} from "../../../../util/utils";

const CATEGORY_KEY = "clickgui.category";
const MODULE_KEY = "clickgui.module";

/**
 * The pane shows these in its header, not among the settings.
 */
export const HEADER_SETTINGS = ["Bind", "Hidden"];

/**
 * Listed below the categories: every module with a key, whatever its category.
 */
export const BINDS = "Binds";

export type ListFilter = "All" | "Enabled" | "Bound";

export interface SettingHit {
    module: string;
    /**
     * The groups from the module down to the one that holds the setting.
     */
    trail: string[];
    name: string;
}

interface IndexedSetting extends SettingHit {
    needle: string;
}

export const modules = $state({
    list: [] as Module[],
    categories: [] as ModuleCategory[],
    category: "",
    selected: null as string | null,
    /**
     * Where the pane of the selected module is: the groups opened as pages, and a setting to point out.
     */
    trail: [] as string[],
    highlight: null as string | null,
    filter: "All" as ListFilter,
    query: "",
    /**
     * Modules with a setting that differs from its default.
     */
    modified: {} as Record<string, boolean>,
});

let settingIndex = new Map<string, IndexedSetting[]>();
let sweep = 0;

function needle(text: string) {
    return text.toLowerCase().replaceAll(" ", "");
}

export function isBound(module: Module) {
    return module.keyBind.boundKey !== UNKNOWN_KEY;
}

export function byName(name: string | null) {
    return modules.list.find(module => module.name === name);
}

/**
 * Reads the modules, and the first time also what each of them is set to. That is one request a module, so later
 * only when [settingsChanged] says all of it can be different.
 */
export async function loadModules(settingsChanged: boolean) {
    const [list, categories] = await Promise.all([getModules(), getCategories()]);
    modules.list = list;
    modules.categories = categories.filter(category => list.some(module => module.category === category.name));

    await persistentDataLoaded;
    if (!modules.category) {
        const stored = localStorage.getItem(CATEGORY_KEY);
        modules.category = stored === BINDS || modules.categories.some(category => category.name === stored)
            ? stored!
            : modules.categories[0]?.name ?? "";

        const selected = localStorage.getItem(MODULE_KEY);
        modules.selected = byName(selected)?.name ?? null;
    }

    if (settingsChanged || sweep === 0) {
        loadAllSettings();
    }
}

/**
 * Reads the settings of every module, for the search and the changed marks. A newer run ends an older one.
 */
async function loadAllSettings() {
    const run = ++sweep;
    const queue = modules.list.map(module => module.name);

    async function worker() {
        while (queue.length > 0 && run === sweep) {
            const name = queue.shift()!;
            try {
                indexSettings(name, await getModuleSettings(name));
            } catch (error) {
                console.error(`Failed to read the settings of ${name}`, error);
            }
        }
    }

    await Promise.all(Array.from({length: 6}, worker));
}

export function indexSettings(module: string, configurable: ConfigurableSetting) {
    const own = configurable.value.filter(setting => !HEADER_SETTINGS.includes(setting.name));
    modules.modified[module] = own.some(isModified);

    const entries: IndexedSetting[] = [];

    function walk(settings: ModuleSetting[], trail: string[]) {
        for (const setting of settings) {
            entries.push({module, trail, name: setting.name, needle: needle(setting.name)});

            if (setting.valueType === "CONFIGURABLE") {
                walk(setting.value as ModuleSetting[], [...trail, setting.name]);
            } else if (setting.valueType === "TOGGLEABLE") {
                walk((setting.value as ModuleSetting[]).slice(1), [...trail, setting.name]);
            } else if (setting.valueType === "CHOICE") {
                // Only the mode in use can be shown
                const choice = setting as ChoiceSetting;
                walk((choice.choices[choice.active]?.value ?? []) as ModuleSetting[], [...trail, setting.name]);
            }
        }
    }

    walk(own, []);
    settingIndex.set(module, entries);
}

export function searchModules(query: string): Module[] {
    const wanted = needle(query);
    if (!wanted) {
        return [];
    }

    return modules.list.filter(module => needle(module.name).includes(wanted)
        || module.aliases.some(alias => needle(alias).includes(wanted)));
}

export function searchSettings(query: string, limit: number): SettingHit[] {
    const wanted = needle(query);
    const hits: SettingHit[] = [];
    if (wanted.length < 2) {
        return hits;
    }

    for (const module of modules.list) {
        for (const entry of settingIndex.get(module.name) ?? []) {
            if (entry.needle.includes(wanted)) {
                hits.push(entry);
                if (hits.length >= limit) {
                    return hits;
                }
            }
        }
    }
    return hits;
}

export function selectCategory(name: string) {
    modules.category = name;
    modules.query = "";
    setItem(CATEGORY_KEY, name);
}

/**
 * Opens the pane of a module, on the group at [trail] with [highlight] pointed out.
 */
export function selectModule(name: string | null, trail: string[] = [], highlight: string | null = null) {
    modules.selected = name;
    modules.trail = trail;
    modules.highlight = highlight;
    setItem(MODULE_KEY, name ?? "");
}

export function applyToggle(name: string, enabled: boolean) {
    const module = byName(name);
    if (module) {
        module.enabled = enabled;
    }
}

/**
 * The bind and the hidden state of a module are part of its settings, the list shows them from the module.
 */
export async function refreshModule(name: string) {
    const fresh = await getModule(name);
    const index = modules.list.findIndex(module => module.name === name);
    if (index >= 0) {
        modules.list[index] = fresh;
    }
}
