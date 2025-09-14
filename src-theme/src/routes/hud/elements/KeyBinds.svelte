<script lang="ts">
    import {onMount} from "svelte";
    import type {ModuleSetting, BindSetting} from "../../../integration/types";
    import {getModules, getModuleSettings, getPrintableKeyName} from "../../../integration/rest";
    import {listen} from "../../../integration/ws";
    import {expoInOut} from "svelte/easing";
    import {fly} from "svelte/transition";
    import Line from "../common/Trims/Line.svelte";
    import {convertToSpacedString, spaceSeperatedNames} from "../../../theme/theme_config";

    export let settings: { [name: string]: any };

    type BindModifier = "Shift" | "Control" | "Alt" | "Super";

    type BindInfo = {
        moduleName: string;
        enabled: boolean;
        keyName: string;
        modifiers: BindModifier[];
        action: string;
    };

    let bindings: BindInfo[] = [];
    let loaded = false;
    const keyNameCache: Record<string, string> = {};

    let os: "windows" | "mac" | "linux" = "windows";

    function getRenderString(modifier: BindModifier): string {
        switch (os) {
            case "windows":
                switch (modifier) {
                    case "Control":
                        return "Ctrl";
                    case "Super":
                        return "\u229e";
                    default:
                        return modifier;
                }
            case "mac":
                switch (modifier) {
                    case "Shift":
                        return "\u21e7";
                    case "Control":
                        return "^";
                    case "Alt":
                        return "\u2325";
                    case "Super":
                        return "\u2318";
                    default:
                        return modifier;
                }
            default:
                return modifier;
        }
    }

    async function getCachedPrintableKeyName(key: string): Promise<string> {
        if (keyNameCache[key]) return keyNameCache[key];
        try {
            const res = await getPrintableKeyName(key);
            keyNameCache[key] = res.localized;
            return res.localized;
        } catch {
            return key;
        }
    }

    function isBindSetting(setting: ModuleSetting): setting is BindSetting {
        return (
            setting.valueType === "BIND" &&
            typeof (setting as BindSetting).value?.boundKey === "string"
        );
    }

    const formatKeyName = (key: string | undefined) => {
        if (!key) return key;

        const mouseButtonMap: Record<string, string> = {
            'Left Button': 'LMB',
            'Right Button': 'RMB',
            'Middle Button': 'MMB',
            'Button 4': 'MB4',
            'Button 5': 'MB5',
            'Left Control': 'L Ctrl',
            'Right Control': 'R Ctrl',
            'Left Shift': 'L Shift',
            'Right Shift': 'R Shift',
            'Left Alt': 'L Alt',
            'Right Alt': 'R Alt',
            'Left Win': 'Win',
            'Caps Lock': 'Caps'
        };

        return mouseButtonMap[key] || key;
    };

    async function updateBindings() {
        try {
            const modules = await getModules();
            const results: BindInfo[] = [];
            const UNKNOWN_KEY = "key.keyboard.unknown";

            await Promise.all(
                modules.map(async (module) => {
                    if (module.hidden) return;
                    const settings = await getModuleSettings(module.name);
                    const bindSetting = settings.value.find(isBindSetting);

                    if (bindSetting && bindSetting.value.boundKey !== UNKNOWN_KEY) {
                        let boundKey = bindSetting.value.boundKey.replace(/^(Right|Left)/, "").trim();
                        const printable = await getCachedPrintableKeyName(boundKey);

                        const modifiers: BindModifier[] = (bindSetting.value.modifiers || [])
                            .map((mod: string) => mod.replace(/^(Right|Left)/, "").trim())
                            .filter((mod: string): mod is BindModifier =>
                                ["Shift", "Control", "Alt", "Super"].includes(mod)
                            );

                        results.push({
                            moduleName: module.name,
                            enabled: module.enabled,
                            keyName: printable,
                            modifiers,
                            action: bindSetting.value.action,
                        });
                    }
                })
            );

            bindings = [...results.sort((a, b) => a.moduleName.localeCompare(b.moduleName))];
        } catch (error) {
            bindings = [];
        }
    }


    onMount(async () => {
        await updateBindings();
        spaceSeperatedNames.subscribe(() => updateBindings);

        loaded = true;
    });

    listen("moduleBindChange", updateBindings);
    listen("moduleToggle", updateBindings);
    listen("clickGuiValueChange", updateBindings);
</script>

{#if loaded}
    <div
            class="hud-container hud-container"
            transition:fly|global={{ duration: 500, y: -50, easing: expoInOut }}
    >
        <div class="title">
            <img class="icon" src="img/hud/keybinds/keyboard.svg" alt="keyboard"/>
            <span>Keybindings</span>
        </div>
        {#if settings?.divider}
            <Line gradient={settings?.gradient}/>
        {/if}
        {#each bindings as binding (binding.moduleName)}
            <div class:disabled={!binding.enabled} class="binding-item">
                <span class="module-name">       {$spaceSeperatedNames ? convertToSpacedString(binding.moduleName) : binding.moduleName}</span>
                <span class="key-info">
                    {#if binding.keyName}
                        {#each binding.modifiers as modifier (modifier)}
                            <span>{getRenderString(modifier)}</span>
                            <span class="divider">+</span>
                        {/each}
                        <span class="boundKey">{formatKeyName((binding.keyName))}</span>
                    {:else}
                        <span class="dimmed">None</span>
                    {/if}
                </span>
            </div>
        {/each}
    </div>
{/if}

<style lang="scss">
  @use "../../../colors.scss" as *;

  .hud-container {
    position: absolute;
    padding: 0.5em 0.8em;
    color: $text-color;
    min-width: 225px;
    font-size: 1rem;
  }

  .title {
    display: flex;
    align-items: center;
    font-size: 1em;
    font-weight: bold;
    letter-spacing: 0.1em;
    margin-bottom: 0.5em;

    .icon {
      margin: 0 0.3em;
      height: 1.4em;
      width: auto;
    }
  }

  .binding-item {
    display: flex;
    justify-content: space-between;
    padding: 0.2em 0;
    white-space: nowrap;
    transition: opacity 0.1s;
  }

  .disabled {
    opacity: 0.5;
    filter: grayscale(1);
  }

  .module-name {
    margin-right: 1em;
  }

  .key-info {
    color: $text-color;
    font-weight: 500;
    display: flex;
    align-items: center;
    column-gap: 2px;
  }

  .divider {
    color: $text-color;
    opacity: 0.8;
    font-size: 10px;
    line-height: 1;
    font-family: monospace;
  }

  .boundKey {
    font-weight: bold;
  }

  .dimmed {
    color: $text-color;
  }
</style>
